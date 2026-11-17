"""Resolve release sources and produce an unabridged commit changelog."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import quote

from select_build_targets import parse_versions


def git(*args):
    return subprocess.check_output(['git', *args], text=True).strip()


def tag_commit(tag):
    # A full ref and --end-of-options prevent option/revision-expression injection.
    subprocess.run(['git', 'check-ref-format', f'refs/tags/{tag}'], check=True, capture_output=True)
    return git('rev-parse', '--verify', '--end-of-options', f'refs/tags/{tag}^{{commit}}')


def release_source(tag, source):
    commit = git('rev-parse', '--verify', '--end-of-options', f'{source}^{{commit}}')
    properties = dict(line.split('=', 1) for line in git('show', f'{commit}:gradle.properties').splitlines()
                      if '=' in line and not line.startswith('#'))
    tag = tag.strip() or 'v' + properties['mod_version']
    if not re.fullmatch(r'v?\d+\.\d+(?:\.\d+)?(?:[-+][0-9A-Za-z.-]+)?', tag):
        raise ValueError('Release tag must be a version, for example v1.5.0 or v1.5.0-rc.1')
    # Existing tags are immutable; a manual run builds their source, not branch HEAD.
    exists = subprocess.run(['git', 'show-ref', '--verify', '--quiet', f'refs/tags/{tag}']).returncode == 0
    if exists:
        commit = tag_commit(tag)
    return tag, commit, tag.removeprefix('v')


def is_ancestor(base, commit):
    return subprocess.run(['git', 'merge-base', '--is-ancestor', base, commit]).returncode == 0


def previous_release(releases, tag, commit, override=''):
    if override:
        previous = tag_commit(override)
        if override == tag or not is_ancestor(previous, commit):
            raise ValueError('Previous release must be a different tag reachable from the release commit')
        return override
    candidates = sorted((r for r in releases if not r['draft'] and r['tag_name'] != tag),
                        key=lambda r: r.get('published_at') or '', reverse=True)
    for release in candidates:
        try:
            previous = tag_commit(release['tag_name'])
        except subprocess.CalledProcessError:
            continue
        if is_ancestor(previous, commit):
            return release['tag_name']
    return None


def escape_markdown(value):
    return re.sub(r'([\\`*_\[\]<>])', r'\\\1', value)


def release_notes(repository, tag, commit, targets, previous=None):
    revision = f'{tag_commit(previous)}..{commit}' if previous else commit
    log = git('log', '--reverse', '--format=%H%x00%s%x00%an%x00', revision)
    fields = log.split('\0')
    commits = [(fields[i].strip(), fields[i + 1], fields[i + 2]) for i in range(0, len(fields) - 2, 3)]
    base = f'https://github.com/{repository}'
    lines = [f'AutoCrop {escape_markdown(tag)}', '', 'Minecraft: ' + ', '.join(targets), '',
             f'Source: [{commit[:12]}]({base}/commit/{commit})', '']
    if previous:
        lines += [f'All commits since [{escape_markdown(previous)}]({base}/releases/tag/{quote(previous, safe="")}):', '']
    else:
        lines += ['First release on this history: all commits are included.', '']
    for sha, subject, author in commits:
        lines.append(f'- [{sha[:7]}]({base}/commit/{sha}) {escape_markdown(subject)} — {escape_markdown(author)}')
    if not commits:
        lines.append('No new commits since the previous release.')
    if previous:
        lines += ['', f'[Full comparison]({base}/compare/{quote(previous, safe="")}...{commit})']
    return '\n'.join(lines) + '\n'


def github_releases(repository):
    pages = json.loads(subprocess.check_output(
        ['gh', 'api', '--paginate', '--slurp', f'repos/{repository}/releases?per_page=100'], text=True))
    return [release for page in pages for release in page]


def require_unpublished(releases, tag):
    existing = next((r for r in releases if r['tag_name'] == tag), None)
    if existing and not existing['draft']:
        raise ValueError(f'{tag} is already published; use a new release tag')
    return existing


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['prepare', 'draft'])
    args = parser.parse_args()
    repository = os.environ['GITHUB_REPOSITORY']
    if args.command == 'prepare':
        targets = parse_versions(os.environ.get('VERSIONS', 'all'))
        tag, commit, version = release_source(os.environ.get('RELEASE_TAG', ''), os.environ['GITHUB_SHA'])
        require_unpublished(github_releases(repository), tag)
        previous = os.environ.get('PREVIOUS_RELEASE', '').strip()
        if previous:
            previous_release([], tag, commit, previous)  # Validate overrides before building.
        with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
            output.write(f'tag={tag}\ncommit={commit}\nversion={version}\ntargets={json.dumps(targets)}\n')
        print(f'Build {tag} from {commit} for {", ".join(targets)}')
    else:
        tag, commit = os.environ['RELEASE_TAG'], os.environ['SOURCE_COMMIT']
        targets = json.loads(os.environ['TARGETS'])
        releases = github_releases(repository)
        existing = require_unpublished(releases, tag)
        previous = previous_release(releases, tag, commit, os.environ.get('PREVIOUS_RELEASE', '').strip())
        notes = Path('release_artifacts/RELEASE_NOTES.md')
        notes.write_text(release_notes(repository, tag, commit, targets, previous))
        if existing:
            subprocess.run(['gh', 'release', 'edit', tag, '--repo', repository,
                            '--title', f'AutoCrop {tag}', '--notes-file', str(notes), '--target', commit], check=True)
        else:
            subprocess.run(['gh', 'release', 'create', tag, '--repo', repository, '--draft',
                            '--title', f'AutoCrop {tag}', '--notes-file', str(notes), '--target', commit], check=True)
        assets = sorted(p for p in Path('release_artifacts').iterdir() if p.is_file())
        subprocess.run(['gh', 'release', 'upload', tag, '--repo', repository, '--clobber',
                        *(str(p) for p in assets)], check=True)
        # A rerun with a different version list must not leave jars from the old selection.
        current_names = {p.name for p in assets}
        for asset in (existing or {}).get('assets', []):
            name = asset['name']
            if name.startswith('autocrop-') and name.endswith('.jar') and name not in current_names:
                subprocess.run(['gh', 'release', 'delete-asset', tag, name, '--repo', repository, '--yes'], check=True)


if __name__ == '__main__':
    main()

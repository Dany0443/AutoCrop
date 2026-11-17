"""Choose supported Minecraft targets without executing any event-provided code."""
import json
import os
from pathlib import Path
import re
import subprocess

SUPPORTED = ['26.2', '26.3']


def select_targets(paths, manual=None):
    if manual:
        if manual == 'both':
            return SUPPORTED.copy()
        if manual not in SUPPORTED:
            raise ValueError(f'Unsupported Minecraft target: {manual}')
        return [manual]

    targets = set()
    for path in paths:
        if path.endswith('.md') or path == 'LICENSE' or path.startswith('.github/ISSUE_TEMPLATE/'):
            continue
        for target in SUPPORTED:
            if path == f'versions/{target}.properties' or path.startswith(f'src/minecraft{target}/'):
                targets.add(target)
                break
        else:
            return SUPPORTED.copy()
    return sorted(targets)


def changed_paths(event, event_name):
    if event_name == 'pull_request':
        base, head = event['pull_request']['base']['sha'], event['pull_request']['head']['sha']
        separator = '...'
    elif event_name == 'push':
        base, head = event['before'], event['after']
        separator = '..'
        if base == '0' * 40:
            return ['build.gradle']  # First push: verify both targets.
        if head == '0' * 40:
            return []  # Deleted branch.
    else:
        raise ValueError(f'Unsupported event: {event_name}')
    for revision in [base, head]:
        if not re.fullmatch(r'[0-9a-f]{40}', revision):
            raise ValueError('Expected a full Git commit SHA')
    try:
        result = subprocess.check_output(
            ['git', 'diff', '--name-only', '--no-renames', '-z', f'{base}{separator}{head}', '--']
        )
    except subprocess.CalledProcessError:
        # Force-push history may no longer contain the previous commit.
        return ['build.gradle']
    return result.decode().rstrip('\0').split('\0') if result else []


def main():
    event = json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text())
    event_name = os.environ['GITHUB_EVENT_NAME']
    if event_name == 'workflow_dispatch':
        targets = select_targets([], event.get('inputs', {}).get('versions', 'both'))
    else:
        targets = select_targets(changed_paths(event, event_name))
    output = f'targets={json.dumps(targets)}\nhas_targets={str(bool(targets)).lower()}\n'
    with open(os.environ['GITHUB_OUTPUT'], 'a') as handle:
        handle.write(output)
    summary = 'Minecraft targets: ' + (', '.join(targets) if targets else 'none (no build changes)')
    print(summary)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as handle:
            handle.write(summary + '\n')


if __name__ == '__main__':
    main()

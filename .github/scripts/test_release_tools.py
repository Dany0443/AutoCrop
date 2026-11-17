import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

from release_tools import previous_release, release_notes, release_source, require_unpublished


SCRIPT = Path(__file__).with_name('release_tools.py').resolve()


class ReleaseNotesTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.previous_directory = os.getcwd()
        os.chdir(self.folder.name)
        self.git('init')
        self.git('config', 'user.name', 'Release test')
        self.git('config', 'user.email', 'test@example.invalid')
        Path('gradle.properties').write_text('mod_version=1.5.0-dev\n')
        self.commit('Initial commit')
        self.git('tag', 'v1.4')
        self.base = self.git('rev-parse', 'HEAD')
        self.commit('fix: correct harvest batch')
        self.commit('feat: setting without a pull request')
        self.head = self.git('rev-parse', 'HEAD')

    def tearDown(self):
        os.chdir(self.previous_directory)
        self.folder.cleanup()

    def git(self, *args):
        return subprocess.check_output(['git', *args], text=True, stderr=subprocess.DEVNULL).strip()

    def commit(self, subject):
        self.git('add', '.')
        self.git('commit', '--allow-empty', '-m', subject)

    def release(self, tag='v1.4', draft=False, date='2026-01-01'):
        return {'tag_name': tag, 'draft': draft, 'published_at': date}

    def test_every_commit_since_previous_release_is_included(self):
        notes = release_notes('owner/repo', 'v1.5.0', self.head, ['26.3', '1.21.11'], 'v1.4')
        self.assertIn('fix: correct harvest batch', notes)
        self.assertIn('feat: setting without a pull request', notes)
        self.assertNotIn('Initial commit', notes)
        self.assertIn('Minecraft: 26.3, 1.21.11', notes)
        self.assertEqual(notes.count('/commit/'), 3)  # Source and the two new commits.

    def test_version_branch_commits_are_included_without_repeating_previous_sources(self):
        self.git('checkout', '-b', 'version', self.base)
        self.commit('Previous version-specific fix')
        old_version = self.git('rev-parse', 'HEAD')
        self.commit('New version-specific fix')
        new_version = self.git('rev-parse', 'HEAD')
        sources = {'include': [{'minecraft': '26.2', 'branch': '26.2', 'ref': new_version}]}
        previous_sources = {'include': [{'minecraft': '26.2', 'branch': '26.2', 'ref': old_version}]}
        notes = release_notes('owner/repo', 'v1.5.0', self.head, ['26.2'], 'v1.4', sources, previous_sources)
        self.assertIn('New version-specific fix', notes)
        self.assertIn('setting without a pull request', notes)
        self.assertNotIn('Previous version-specific fix', notes)
        self.assertNotIn('Initial commit', notes)
        self.assertIn(f'`26.2` at [{new_version[:12]}]', notes)

    def test_first_release_includes_entire_history(self):
        self.assertIn('Initial commit', release_notes('owner/repo', 'v1.5.0', self.head, ['26.1']))

    def test_drafts_are_not_used_as_previous_release(self):
        self.git('tag', 'v1.5.0-draft')
        releases = [self.release('v1.5.0-draft', True, '2026-11-01'), self.release()]
        self.assertEqual(previous_release(releases, 'v1.5.0', self.head), 'v1.4')

    def test_unrelated_newer_release_is_skipped(self):
        self.git('checkout', '--orphan', 'unrelated')
        self.commit('Unrelated history')
        self.git('tag', 'v9.0.0')
        releases = [self.release('v9.0.0', False, '2026-11-01'), self.release()]
        self.assertEqual(previous_release(releases, 'v1.5.0', self.head), 'v1.4')

    def test_manual_previous_tag_must_be_an_ancestor(self):
        self.git('checkout', '--orphan', 'unrelated')
        self.commit('Unrelated history')
        self.git('tag', 'v9.0.0')
        with self.assertRaises(ValueError):
            previous_release([], 'v1.5.0', self.head, 'v9.0.0')

    def test_new_manual_tag_builds_selected_commit_with_requested_version(self):
        self.assertEqual(release_source('v1.5.0', self.head), ('v1.5.0', self.head, '1.5.0'))

    def test_existing_tag_keeps_its_exact_source(self):
        self.assertEqual(release_source('v1.4', self.head), ('v1.4', self.base, '1.4'))

    def test_empty_tag_defaults_to_mod_version(self):
        self.assertEqual(release_source('', self.head), ('v1.5.0-dev', self.head, '1.5.0-dev'))

    def test_published_releases_cannot_be_overwritten(self):
        with self.assertRaises(ValueError):
            require_unpublished([self.release()], 'v1.4')
        self.assertIsNotNone(require_unpublished([self.release(draft=True)], 'v1.4'))

    def test_invalid_release_tag_is_rejected(self):
        with self.assertRaises(ValueError):
            release_source('v1.5.0\ncommit=wrong', self.head)


    def run_draft_workflow(self, existing=None):
        Path('release_artifacts').mkdir()
        Path('release_artifacts/autocrop-1.5.0-mc26.3.jar').write_bytes(b'test jar')
        Path('release_artifacts/SHA256SUMS.txt').write_text('test checksum')
        Path('tools').mkdir()
        helper = Path('tools/gh')
        helper.write_text("""#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
with Path(os.environ['CALL_LOG']).open('a') as log:
    log.write(json.dumps(sys.argv[1:]) + '\\n')
if sys.argv[1] == 'api':
    print(os.environ['MOCK_RELEASES'])
""")
        helper.chmod(0o755)
        releases = [self.release()]
        if existing:
            releases.append(existing)
        env = {**os.environ, 'PATH': str(Path('tools').resolve()) + os.pathsep + os.environ['PATH'],
               'CALL_LOG': str(Path('calls.jsonl').resolve()), 'MOCK_RELEASES': json.dumps([releases]),
               'GITHUB_REPOSITORY': 'owner/repo', 'RELEASE_TAG': 'v1.5.0',
               'SOURCE_COMMIT': self.head, 'TARGETS': '["26.3"]', 'PREVIOUS_RELEASE': ''}
        result = subprocess.run(['python3', str(SCRIPT), 'draft'], env=env, capture_output=True, text=True)
        calls = [json.loads(line) for line in Path('calls.jsonl').read_text().splitlines()]
        return result, calls

    def test_draft_workflow_creates_unpublished_release_with_full_notes_and_artifacts(self):
        result, calls = self.run_draft_workflow()
        self.assertEqual(result.returncode, 0, result.stderr)
        create = next(call for call in calls if call[:2] == ['release', 'create'])
        self.assertIn('--draft', create)
        self.assertEqual(create[create.index('--target') + 1], self.head)
        upload = next(call for call in calls if call[:2] == ['release', 'upload'])
        self.assertIn('release_artifacts/autocrop-1.5.0-mc26.3.jar', upload)
        notes = Path('release_artifacts/RELEASE_NOTES.md').read_text()
        self.assertIn('setting without a pull request', notes)
        self.assertNotIn('Initial commit', notes)

    def test_draft_rerun_updates_notes_and_removes_only_obsolete_mod_jars(self):
        existing = self.release('v1.5.0', True)
        existing['assets'] = [{'name': 'autocrop-1.5.0-mc26.2.jar'}, {'name': 'custom-file.jar'}]
        result, calls = self.run_draft_workflow(existing)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(any(call[:2] == ['release', 'edit'] for call in calls))
        self.assertFalse(any(call[:2] == ['release', 'create'] for call in calls))
        deleted = [call[3] for call in calls if call[:2] == ['release', 'delete-asset']]
        self.assertEqual(deleted, ['autocrop-1.5.0-mc26.2.jar'])

    def test_workflow_refuses_to_modify_a_published_release(self):
        result, calls = self.run_draft_workflow(self.release('v1.5.0'))
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('already published', result.stderr)
        self.assertFalse(any(call[0] == 'release' for call in calls))


if __name__ == '__main__':
    unittest.main()

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

from select_build_targets import select_targets


class SelectBuildTargetsTest(unittest.TestCase):
    def test_shared_gameplay_builds_both_versions(self):
        self.assertEqual(select_targets(['src/main/java/com/danz/autocrop/CropManager.java']), ['26.2', '26.3'])

    def test_target_profile_only_builds_that_version(self):
        self.assertEqual(select_targets(['versions/26.2.properties']), ['26.2'])

    def test_target_adapter_only_builds_that_version(self):
        self.assertEqual(select_targets(['src/minecraft26.3/java/com/danz/autocrop/MinecraftCompat.java']), ['26.3'])

    def test_documentation_changes_skip_compilation(self):
        self.assertEqual(select_targets(['README.md', 'docs/setup.md', 'LICENSE']), [])

    def test_build_workflow_changes_build_both(self):
        self.assertEqual(select_targets(['.github/workflows/compile.yml']), ['26.2', '26.3'])

    def test_unknown_changes_are_built_conservatively(self):
        self.assertEqual(select_targets(['new-build-tool.toml']), ['26.2', '26.3'])

    def test_manual_selection_overrides_changed_paths(self):
        self.assertEqual(select_targets(['src/main/java/Anything.java'], '26.2'), ['26.2'])
        self.assertEqual(select_targets([], 'both'), ['26.2', '26.3'])

    def test_empty_change_set_skips_compilation(self):
        self.assertEqual(select_targets([]), [])

    def test_invalid_manual_target_is_rejected(self):
        with self.assertRaises(ValueError):
            select_targets([], '1.21.1')

    def test_multiple_paths_are_deduplicated_and_sorted(self):
        self.assertEqual(select_targets(['versions/26.3.properties', 'versions/26.2.properties', 'README.md']), ['26.2', '26.3'])


class WorkflowEventTest(unittest.TestCase):
    def run_selector(self, event_name, event, cwd=None):
        script = Path(__file__).with_name('select_build_targets.py').resolve()
        with tempfile.TemporaryDirectory() as folder:
            event_file = Path(folder, 'event.json')
            output_file = Path(folder, 'output')
            event_file.write_text(json.dumps(event))
            result = subprocess.run(
                ['python3', str(script)], cwd=cwd, check=True, capture_output=True,
                env={**os.environ, 'GITHUB_EVENT_PATH': str(event_file),
                     'GITHUB_EVENT_NAME': event_name, 'GITHUB_OUTPUT': str(output_file),
                     'GITHUB_STEP_SUMMARY': str(Path(folder, 'summary'))})
            return dict(line.split('=', 1) for line in output_file.read_text().splitlines())

    def test_manual_workflow_outputs_one_target(self):
        output = self.run_selector('workflow_dispatch', {'inputs': {'versions': '26.3'}})
        self.assertEqual(json.loads(output['targets']), ['26.3'])
        self.assertEqual(output['has_targets'], 'true')

    def test_first_push_builds_both(self):
        output = self.run_selector('push', {'before': '0' * 40, 'after': 'a' * 40})
        self.assertEqual(json.loads(output['targets']), ['26.2', '26.3'])

    def test_real_git_diff_handles_version_only_and_docs_only_changes(self):
        with tempfile.TemporaryDirectory() as folder:
            def git(*args):
                return subprocess.check_output(['git', '-C', folder, *args], stderr=subprocess.DEVNULL).decode().strip()
            git('init')
            git('config', 'user.name', 'Workflow test')
            git('config', 'user.email', 'test@example.invalid')
            Path(folder, 'README.md').write_text('Initial')
            git('add', '.')
            git('commit', '-m', 'Initial')
            before = git('rev-parse', 'HEAD')
            Path(folder, 'versions').mkdir()
            Path(folder, 'versions/26.2.properties').write_text('minecraft_version=26.2')
            git('add', '.')
            git('commit', '-m', 'Target update')
            after = git('rev-parse', 'HEAD')
            output = self.run_selector('push', {'before': before, 'after': after}, folder)
            self.assertEqual(json.loads(output['targets']), ['26.2'])
            # A PR uses the merge-base comparison and yields the same target.
            output = self.run_selector('pull_request', {'pull_request': {'base': {'sha': before}, 'head': {'sha': after}}}, folder)
            self.assertEqual(json.loads(output['targets']), ['26.2'])
            Path(folder, 'README.md').write_text('Updated docs')
            git('add', '.')
            git('commit', '-m', 'Docs')
            output = self.run_selector('push', {'before': after, 'after': git('rev-parse', 'HEAD')}, folder)
            self.assertEqual(output['has_targets'], 'false')


if __name__ == '__main__':
    unittest.main()

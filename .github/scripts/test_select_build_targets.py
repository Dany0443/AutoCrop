import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

from select_build_targets import select_targets, parse_versions

ACTIVE = ['26.1', '26.2', '26.3', '1.21.11']


class SelectBuildTargetsTest(unittest.TestCase):
    def test_shared_gameplay_builds_all_supported_versions(self):
        self.assertEqual(select_targets(['src/main/java/com/danz/autocrop/CropManager.java']), ACTIVE)

    def test_target_profile_only_builds_that_version(self):
        self.assertEqual(select_targets(['versions/26.2.properties']), ['26.2'])

    def test_target_adapter_only_builds_that_version(self):
        self.assertEqual(select_targets(['src/minecraft26.3/java/com/danz/autocrop/MinecraftCompat.java']), ['26.3'])

    def test_documentation_changes_skip_compilation(self):
        self.assertEqual(select_targets(['README.md', 'docs/setup.md', 'LICENSE']), [])

    def test_build_workflow_changes_build_all_supported_versions(self):
        self.assertEqual(select_targets(['.github/workflows/compile.yml']), ACTIVE)

    def test_unknown_changes_are_built_conservatively(self):
        self.assertEqual(select_targets(['new-build-tool.toml']), ACTIVE)

    def test_manual_selection_overrides_changed_paths(self):
        self.assertEqual(select_targets(['src/main/java/Anything.java'], '26.2'), ['26.2'])
        self.assertEqual(select_targets([], 'all'), ACTIVE)

    def test_empty_change_set_skips_compilation(self):
        self.assertEqual(select_targets([]), [])

    def test_invalid_manual_target_is_rejected(self):
        with self.assertRaises(ValueError):
            select_targets([], '1.21.1')

    def test_multiple_paths_are_deduplicated_and_sorted(self):
        self.assertEqual(select_targets(['versions/26.3.properties', 'versions/26.2.properties', 'README.md']), ['26.2', '26.3'])


    def test_every_supported_profile_and_adapter_selects_only_its_target(self):
        for target in ACTIVE:
            for path in [f'versions/{target}.properties', f'src/minecraft{target}/java/MinecraftCompat.java']:
                with self.subTest(path=path):
                    self.assertEqual(select_targets([path]), [target])

    def test_branch_contains_its_default_target_profile(self):
        root = Path(__file__).resolve().parents[2]
        profiles = {p.stem for p in (root / 'versions').glob('*.properties')}
        self.assertTrue(profiles <= set(ACTIVE))
        properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
        self.assertEqual(profiles, {properties['minecraft_target']})


class ParseVersionsTest(unittest.TestCase):
    def test_comma_separated_versions_preserve_order_and_remove_duplicates(self):
        self.assertEqual(parse_versions(' 26.3, 26.2,26.3, 1.21.11 '), ['26.3', '26.2', '1.21.11'])

    def test_all_selects_all_active_versions(self):
        self.assertEqual(parse_versions('all'), ACTIVE)

    def test_legacy_alias_still_accepts_both_modern_targets(self):
        self.assertEqual(parse_versions('both'), ['26.2', '26.3'])

    def test_26_1_is_supported(self):
        self.assertEqual(parse_versions('26.1'), ['26.1'])

    def test_unknown_legacy_and_empty_items_fail(self):
        for versions in ['', '26.3,', '26.3,,26.2', '1.21.10', '26.4', 'all,26.3', '../26.3']:
            with self.subTest(versions=versions), self.assertRaises(ValueError):
                parse_versions(versions)


class WorkflowEventTest(unittest.TestCase):
    def run_selector(self, event_name, event, cwd=None):
        script = Path(__file__).with_name('select_build_targets.py').resolve()
        with tempfile.TemporaryDirectory() as folder:
            event_file = Path(folder, 'event.json')
            output_file = Path(folder, 'output')
            event_file.write_text(json.dumps(event))
            result = subprocess.run(
                ['python3', str(script)], cwd=cwd, check=True, capture_output=True,
                env={**{k: v for k, v in os.environ.items() if k != 'GITHUB_REF_NAME'}, 'GITHUB_EVENT_PATH': str(event_file),
                     'GITHUB_EVENT_NAME': event_name, 'GITHUB_OUTPUT': str(output_file),
                     'GITHUB_STEP_SUMMARY': str(Path(folder, 'summary'))})
            return dict(line.split('=', 1) for line in output_file.read_text().splitlines())

    def test_manual_workflow_outputs_one_target(self):
        output = self.run_selector('workflow_dispatch', {'inputs': {'versions': '26.3'}})
        self.assertEqual(json.loads(output['targets']), ['26.3'])
        self.assertEqual(output['has_targets'], 'true')

    def test_first_push_builds_all_supported_versions(self):
        output = self.run_selector('push', {'before': '0' * 40, 'after': 'a' * 40})
        self.assertEqual(json.loads(output['targets']), ACTIVE)

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

import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from select_build_targets import select_targets
from branch_sources import resolve_sources


class BranchSourcesTest(unittest.TestCase):
    def test_automatic_pushes_only_build_their_own_version(self):
        for version in ['1.21.11', '26.1', '26.2', '26.3']:
            self.assertEqual(select_targets(['src/main/java/CropManager.java'], branch=version), [version])
        self.assertEqual(select_targets(['src/main/java/CropManager.java'], branch='main'), ['26.3'])
        self.assertEqual(select_targets(['README.md'], branch='26.2'), [])

    def test_manual_input_can_build_other_branches(self):
        self.assertEqual(select_targets([], manual='26.2,1.21.11', branch='main'), ['26.2', '1.21.11'])

    def test_resolves_each_version_branch_and_never_uses_main(self):
        with tempfile.TemporaryDirectory() as folder:
            def git(*args):
                return subprocess.check_output(['git', '-C', folder, *args], text=True, stderr=subprocess.DEVNULL).strip()
            git('init')
            git('config', 'user.name', 'Branch test')
            git('config', 'user.email', 'test@example.invalid')
            git('commit', '--allow-empty', '-m', 'main')
            main = git('rev-parse', 'HEAD')
            commits = {}
            for version in ['26.3', '26.2', '1.21.11']:
                git('commit', '--allow-empty', '-m', version)
                commits[version] = git('rev-parse', 'HEAD')
                git('update-ref', f'refs/remotes/origin/{version}', commits[version])
            old = os.getcwd()
            os.chdir(folder)
            try:
                sources = resolve_sources(['26.3', '26.2', '1.21.11'], pin_push=False)
                self.assertEqual(sources['include'], [
                    {'minecraft': v, 'branch': v, 'ref': commits[v]} for v in commits])
                self.assertNotIn(main, [s['ref'] for s in sources['include']])
                # Main can advance independently without changing any selected build.
                git('commit', '--allow-empty', '-m', 'unreleased main changes')
                self.assertEqual(resolve_sources(list(commits), pin_push=False), sources)
                # A branch push pins the triggering revision, even if its branch has advanced.
                with patch.dict(os.environ, {'GITHUB_EVENT_NAME': 'push', 'GITHUB_REF': 'refs/heads/26.2', 'GITHUB_SHA': main}):
                    self.assertEqual(resolve_sources(['26.2'])['include'][0]['ref'], main)
                with self.assertRaises(subprocess.CalledProcessError):
                    resolve_sources(['26.1'])
                for invalid in ['../main', 'all', 'both', '26.3,26.2']:
                    with self.assertRaises(ValueError):
                        resolve_sources([invalid])
            finally:
                os.chdir(old)

"""Pin each requested Minecraft version to its own version branch."""
import json
import os
import re
import subprocess

from select_build_targets import SUPPORTED


def resolve_sources(targets, pin_push=True):
    sources = []
    for target in targets:
        if target not in SUPPORTED:
            raise ValueError(f'Unsupported version branch: {target}')
        if pin_push and os.environ.get('GITHUB_EVENT_NAME') == 'push' and os.environ.get('GITHUB_REF') == f'refs/heads/{target}':
            revision = os.environ['GITHUB_SHA']
            if not re.fullmatch(r'[0-9a-f]{40}', revision):
                raise ValueError('Expected a full triggering commit SHA')
        else:
            revision = f'refs/remotes/origin/{target}'
        commit = subprocess.check_output(
            ['git', 'rev-parse', '--verify', '--end-of-options', f'{revision}^{{commit}}'], text=True).strip()
        sources.append({'minecraft': target, 'branch': target, 'ref': commit})
    return {'include': sources}


if __name__ == '__main__':
    matrix = resolve_sources(json.loads(os.environ['TARGETS']))
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write('sources=' + json.dumps(matrix) + '\n')
    print(json.dumps(matrix, indent=2))

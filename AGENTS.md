# Repository workflow

- Work directly on the existing version branches and `main`. Do not create Codex branches or pull requests unless the user explicitly requests them.
- `main` contains only the latest Minecraft implementation, currently 26.3. Maintain Minecraft 1.21.11, 26.1, 26.2, and 26.3 on their corresponding named branches.
- Each branch contains only its own Minecraft dependency profile and compatibility adapter. Port shared gameplay/configuration fixes to the affected version branches and test each branch before pushing.
- Build workflows must check out each selected version's named branch, pinned to a commit. Never build older Minecraft targets from `main`.
- Use the repository owner's configured Git author and committer: Dany0443 <danzcrackz@gmail.com>. Do not append Codex co-author trailers.
- Preserve existing branch history; do not force-push or merge the abandoned Codex branch.

# graphify

When the user types `/graphify`, read and use the installed graphify skill before doing anything else.

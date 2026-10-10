# Contributing to AutoCrop

Thank you for your interest in contributing to AutoCrop!

## Branch-Per-Version Architecture

AutoCrop follows the **Branch-Per-Version Architecture**:
- `main` always tracks the latest supported Minecraft version (currently **Minecraft 26.3**).
- Dedicated git branches track each supported older release (e.g. `26.2`, `26.1`, `1.21.1` - `1.21.11`).
- Every branch is completely self-contained. You can check out any branch and run `./gradlew build` directly at the repository root.
- There are no nested `versions/` subdirectories.

## Development Workflow

1. Clone or fork the repository.
2. Checkout the branch for the Minecraft version you want to target:
   ```bash
   git switch main      # for Minecraft 26.3
   git switch 26.2      # for Minecraft 26.2
   git switch 26.1      # for Minecraft 26.1
   git switch 1.21.1    # for Minecraft 1.21.1
   ```
3. Create your feature branch from that target version:
   ```bash
   git checkout -b fix/my-improvement
   ```
4. Verify your build passes locally:
   ```bash
   ./gradlew build
   ```
5. Follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g. `fix: ...`, `feat: ...`, `chore: ...`).
6. Submit a Pull Request targeting the respective version branch.

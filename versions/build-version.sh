#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: ./versions/build-version.sh <mc-version>"
  exit 1
fi

version="$1"
project_dir="versions/$version"

if [ ! -d "$project_dir" ]; then
  echo "Unknown version project: $project_dir"
  exit 1
fi

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

GRADLE_USER_HOME="$repo_root/.gradle" ./gradlew -p "$project_dir" clean build

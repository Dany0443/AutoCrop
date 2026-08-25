#!/usr/bin/env bash
# script to scaffold a new minecraft version subproject
set -euo pipefail

if [ "$#" -ne 2 ]; then
  echo "Usage: ./versions/new-version.sh <new-mc-version> <from-mc-version>"
  exit 1
fi

new_version="$1"
from_version="$2"

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
from_dir="$repo_root/versions/$from_version"
new_dir="$repo_root/versions/$new_version"

if [ ! -d "$from_dir" ]; then
  echo "Source version does not exist: $from_dir"
  exit 1
fi

if [ -e "$new_dir" ]; then
  echo "Target version already exists: $new_dir"
  exit 1
fi

mkdir -p "$new_dir"
cp "$from_dir/build.gradle" "$new_dir/build.gradle"
cp "$from_dir/gradle.properties" "$new_dir/gradle.properties"
cp "$from_dir/settings.gradle" "$new_dir/settings.gradle"

sed -i "s/^minecraft_version=.*/minecraft_version=$new_version/" "$new_dir/gradle.properties"
sed -i "s/^mod_version=.*/mod_version=1.4/" "$new_dir/gradle.properties"
sed -i "s/^rootProject.name = .*/rootProject.name = \"autocrop-$new_version\"/" "$new_dir/settings.gradle"

echo "Created $new_dir"
echo "Next: update yarn_mappings/fabric_version/cloth_config_version/modmenu_version in gradle.properties"

# 1.21.x Version Matrix

This folder contains dedicated Fabric Loom remap projects for each `1.21.x` target.

## Build one version

From the repo root:

```bash
GRADLE_USER_HOME=$PWD/.gradle ./gradlew -p versions/1.21.6 clean build
```

Or use the helper script:

```bash
./versions/build-version.sh 1.21.6
```

## Current pinned targets

| MC version | Yarn mappings | Loader | Fabric API | Cloth Config | Mod Menu |
|---|---|---|---|---|---|
| 1.21.6 | 1.21.6+build.1 | 0.19.2 | 0.128.2+1.21.6 | 19.0.147 | 15.0.2 |
| 1.21.7 | 1.21.7+build.8 | 0.19.2 | 0.129.0+1.21.7 | 19.0.147 | 15.0.2 |
| 1.21.8 | 1.21.8+build.1 | 0.19.2 | 0.136.1+1.21.8 | 19.0.147 | 15.0.2 |
| 1.21.9 | 1.21.9+build.1 | 0.19.2 | 0.134.1+1.21.9 | 20.0.149 | 16.0.1 |
| 1.21.10 | 1.21.10+build.3 | 0.19.2 | 0.138.4+1.21.10 | 20.0.149 | 16.0.1 |
| 1.21.11 | 1.21.11+build.5 | 0.19.2 | 0.141.4+1.21.11 | 21.11.153 | 17.0.0 |

## Add the next patch version

```bash
./versions/new-version.sh 1.21.12 1.21.11
```

Then edit `versions/1.21.12/gradle.properties` with the new dependency pins.

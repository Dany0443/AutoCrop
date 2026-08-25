# AutoCrop Multi-Version Matrix

This folder contains dedicated Fabric Loom projects for each supported Minecraft target.

## Build Single Version

From repository root:

```bash
./gradlew -p versions/1.21.1 clean build
```

## Supported Pinned Targets

| Minecraft Version | Yarn Mappings | Loader | Fabric API | Cloth Config | Mod Menu |
|---|---|---|---|---|---|
| 1.21.1 | 1.21.1+build.3 | 0.19.2 | 0.116.11+1.21.1 | 15.0.140 | 11.0.3 |
| 1.21.2 | 1.21.2+build.1 | 0.19.2 | 0.106.1+1.21.2 | 16.0.141 | 12.0.0 |
| 1.21.3 | 1.21.3+build.2 | 0.19.2 | 0.114.1+1.21.3 | 16.0.141 | 13.0.1 |
| 1.21.4 | 1.21.4+build.8 | 0.19.2 | 0.119.4+1.21.4 | 17.0.144 | 14.0.0 |
| 1.21.5 | 1.21.5+build.1 | 0.19.2 | 0.128.2+1.21.5 | 18.0.145 | 14.0.0 |
| 1.21.6 | 1.21.6+build.1 | 0.19.2 | 0.128.2+1.21.6 | 19.0.147 | 15.0.2 |
| 1.21.7 | 1.21.7+build.8 | 0.19.2 | 0.129.0+1.21.7 | 19.0.147 | 15.0.2 |
| 1.21.8 | 1.21.8+build.1 | 0.19.2 | 0.136.1+1.21.8 | 19.0.147 | 15.0.2 |
| 1.21.9 | 1.21.9+build.1 | 0.19.2 | 0.134.1+1.21.9 | 20.0.149 | 16.0.1 |
| 1.21.10 | 1.21.10+build.3 | 0.19.2 | 0.138.4+1.21.10 | 20.0.149 | 16.0.1 |
| 1.21.11 | 1.21.11+build.5 | 0.19.2 | 0.141.4+1.21.11 | 21.11.153 | 17.0.0 |
| 26.1 | Official Mojang | 0.18.4 | 0.145.2+26.1.1 | 26.1.154 | 18.0.0-alpha.8 |
| 26.2 | Official Mojang | 0.18.4 | 0.145.2+26.1.1 | 26.1.154 | 18.0.0-alpha.8 |

## Scaffold Next Minecraft Version

```bash
./versions/new-version.sh 1.21.12 1.21.11
```

Then verify mappings and dependency versions in `versions/1.21.12/gradle.properties`.

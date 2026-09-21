# StackGres Build Framework -- Design Document

## 1. Introduction and Design Philosophy

The StackGres build framework is a custom, shell-based build system that produces
Docker images from a directed acyclic graph (DAG) of modules. It is designed
around three core invariants:

**Content-addressable tagging.** Every built image is tagged with an MD5 hash
derived exclusively from its inputs: source files, configuration, build
environment, parent image tag, and builder version. Identical inputs always
produce the same tag, regardless of when or where the build runs.

**Zero-rebuild guarantee.** Before building any module, the framework probes the
remote registry (and local Docker cache) for an image with the computed hash
tag. If found, the build is skipped entirely. This means a CI pipeline that
touches only the `operator` module will never rebuild `parent-java`, `common`,
or any other unchanged ancestor.

**Deterministic dependency resolution.** Modules form a linear chain defined in
the `stages` section of `config.yml`. The framework walks this chain from any
requested module back to its root, ensuring all ancestors are processed in
topological order before their dependents.

The framework is intentionally written in POSIX shell (with minor bashisms via
`local`) to minimize dependencies. It requires only `sh`, `jq`, `yq`, `md5sum`,
`git`, `flock`, and `docker`.


## 2. Architecture Overview

The system is organized in three layers:

```
+-----------------------------------------------------------------------+
|                         CI Integration Layer                          |
|  build-gitlab.sh    Registry auth, --extract post-processing          |
|  ciw                Docker wrapper for reproducible CI environment     |
+-----------------------------------------------------------------------+
        |  invokes
        v
+-----------------------------------------------------------------------+
|                          Entry Point Layer                            |
|  build.sh           Dependency resolution, orchestration loop         |
+-----------------------------------------------------------------------+
        |  sources (. build-functions.sh)
        v
+-----------------------------------------------------------------------+
|                          Core Library Layer                           |
|  build-functions.sh                                                   |
|    init_config          YAML -> JSON config parsing                   |
|    generate_image_hashes  Content-addressable hash computation        |
|    find_image_digests   Parallel registry probing (xargs -P 16)       |
|    build_image          Per-module build orchestration                 |
|    build_module_image   Container build execution                     |
|    copy_from_image      Artifact extraction from parent images        |
|    module_image_name    Hash computation for a single module          |
|    source_image_name    Parent image resolution via stages            |
|    docker_*             Thin wrappers around Docker CLI               |
+-----------------------------------------------------------------------+
        |  reads
        v
+-----------------------------------------------------------------------+
|                          Configuration Layer                          |
|  config.yml         Module definitions, stages DAG, image anchors     |
+-----------------------------------------------------------------------+
```

### State Directory

All intermediate and generated state lives under `stackgres-k8s/ci/build/target/`:

| File                                   | Purpose                                       |
|----------------------------------------|-----------------------------------------------|
| `config.json`                          | Parsed YAML config (JSON form)                |
| `config.yml.md5`                       | MD5 of config.yml for cache invalidation      |
| `build_hash`                           | MD5 of the requested module list              |
| `project_hash.<BUILD_HASH>`            | Git tree hash + env hash for skip-all check   |
| `image-hashes.<BUILD_HASH>`            | MODULE=IMAGE_NAME mapping for all modules     |
| `image-type-hashes.<BUILD_HASH>`       | MODULE_TYPE=HASH for grouped type hashes      |
| `all-images.<BUILD_HASH>`              | Flat list of all image names to probe          |
| `found-image-digests.<BUILD_HASH>`     | Registry probe results                        |
| `image-digests.<BUILD_HASH>`           | Working copy of digests (found + built)       |
| `<MODULE>-hash`                        | Raw hash input file per module                |
| `<MODULE>-build-env`                   | Evaluated build_env for container injection   |
| `Dockerfile.<MODULE>`                  | Generated Dockerfile per module               |
| `.git/`                                | Shadow git repo for `path_hash()` via tree SHA|
| `junit-build.hashes.xml.<BUILD_HASH>`  | JUnit XML report of hashes (for CI artifacts) |
| `manifest.<TAG>` / `manifest.local.<TAG>` | Cached image manifests                    |


## 3. Configuration Schema

The configuration file `config.yml` is a YAML document with four top-level keys.

### `.images`

A dictionary of YAML anchors for base image references. Not consumed directly by
the build system -- it exists solely so modules can reference images via `*anchor`
syntax.

```yaml
.images:
  ci_image: &ci_image registry.gitlab.com/ongresinc/stackgres/ci:1.31
  jdk_build_image: &jdk_build_image registry.gitlab.com/ongresinc/stackgres/builder:1.10
```

### `.platforms`

A list of platform strings in `os/arch` format. Used when a module has
`platform_dependent: true` to generate per-platform image tags.

```yaml
platforms:
  - linux/x86_64
  - linux/aarch64
```

### `.modules`

A dictionary where each key is a module name and each value is a module
definition object. YAML anchor inheritance (`<<: *parent`) is used extensively
to share configuration across related modules.

| Field                  | Type          | Required | Description |
|------------------------|---------------|----------|-------------|
| `type`                 | string        | Yes      | Label grouping modules (e.g. `java`, `native`, `jvm-image`). Used only for type-hash aggregation, not for build logic. |
| `path`                 | string        | Yes      | Module path relative to project root. Exposed as `$MODULE_PATH` in build containers. |
| `sources`              | list/dict     | No       | Files/directories whose git tree hashes are included in the content hash. Accepts both list `[...]` and dict `{0: ..., 1: ...}` forms. |
| `filtered_sources`     | list of string| No       | Shell commands whose stdout is included in the content hash. Used for version-redacted source content (e.g. `sh redact-version.sh`). |
| `artifacts`            | list/dict     | Yes      | Files/directories to COPY into the built image. Also listed in `.dockerignore` as exceptions. Accepts both list and dict forms. |
| `build_image`          | string        | Yes      | Docker image used to run `build_commands`, `pre_build_commands`, and `post_build_commands`. Set to `null` to skip the build container step. |
| `target_image`         | string/null   | No       | Base image for the final artifact image. If `null`, inherits the parent module's built image. Mutually exclusive with `dockerfile.path`. |
| `build_commands`       | list of string| No       | Shell commands executed inside `build_image` as `$BUILD_UID`. |
| `pre_build_commands`   | list of string| No       | Shell commands executed before `build_commands`, as `pre_post_build_uid` (or `$BUILD_UID`). |
| `post_build_commands`  | list of string| No       | Shell commands executed after `build_commands`, as `pre_post_build_uid` (or `$BUILD_UID`). |
| `build_env`            | dict          | No       | Environment variables injected into build containers and included in hash computation. Values are shell-evaluated (supports `$VAR` references). |
| `cache`                | list of string| No       | Directories to extract from the build image for local caching (e.g. `.m2/`). |
| `dockerfile.path`      | string        | No       | Path to a custom Dockerfile. Mutually exclusive with `target_image`. |
| `dockerfile.args`      | dict          | No       | Build arguments passed to `docker build --build-arg`. Values are shell-evaluated at build time. |
| `dockerfile.seds`      | list of string| No       | `sed` expressions applied to the Dockerfile before building (e.g. rewriting COPY paths). |
| `platform_dependent`   | bool          | No       | When `true`, the module produces per-platform images with a platform suffix in the tag. Hash generation iterates over all `.platforms`. |
| `pre_post_build_uid`   | string        | No       | UID:GID for `pre_build_commands` and `post_build_commands`. Defaults to `$BUILD_UID`. |

### `.stages`

An ordered list of single-entry objects defining the dependency graph. Each
object maps a module name to its parent module (or `null` for root modules).
The list order defines the canonical build sequence.

```yaml
stages:
  - parent-java: null              # root -- no parent
  - operator-framework-java: parent-java
  - common-java: test-util-java
  - operator-java: common-java
  - operator-jvm-image: operator-java
  - operator-native: operator-java
  - operator-native-image: operator-native
  - helm-packages: null            # independent root
```

The stages array serves two purposes:
1. It defines the parent-child relationship used by `source_image_name()` to
   resolve which image a module inherits artifacts from.
2. The backward walk in `build.sh` (lines 24-49) uses it to discover all
   transitive ancestors of requested modules.


## 4. Build Lifecycle

A build proceeds through five sequential phases:

```
  config.yml
      |
      v
  [Phase 1: Config Parsing]  -- init_config() (lines 572-585)
      |                         yq . config.yml > config.json
      |                         Cached by MD5 of config.yml
      v
  [Phase 2: Dependency Resolution]  -- build.sh (lines 24-49)
      |                                Walk stages[] backward to roots
      |                                Produce topologically-sorted MODULES list
      v
  [Phase 3: Hash Generation]  -- generate_image_hashes() (lines 484-555)
      |                          init_hash() creates shadow git repo
      |                          module_image_name() computes MD5 per module
      |                          Cached by project_hash (git HEAD tree + env)
      v
  [Phase 4: Registry Probing]  -- find_image_digests() (lines 645-653)
      |                           16-way parallel probe via xargs -P 16
      |                           Each probe: docker manifest inspect or docker inspect
      v
  [Phase 5: Per-Module Build]  -- build_image() loop in build.sh (lines 96-99)
                                  Skip if digest found in Phase 4
                                  Skip if local Docker image exists
                                  Otherwise: build_module_image()
```

### Phase 1: Config Parsing

`init_config()` at line 572 of `build-functions.sh` converts `config.yml` to
JSON using `yq`. The result is cached at `target/config.json` and invalidated
only when the MD5 of `config.yml` changes (tracked in `target/config.yml.md5`).

### Phase 2: Dependency Resolution

For each module passed on the command line, `build.sh` walks the `stages[]`
array upward through parent references until it finds a root (`null` parent).
Each discovered ancestor is prepended to `COMPLETED_MODULES` (ensuring
parents appear before children). Duplicates are skipped. The result is a flat,
topologically-sorted list.

If no modules are specified, all modules from `config.json` are built.

### Phase 3: Hash Generation

`generate_image_hashes()` first checks whether the project state has changed
since the last run by comparing `project_hash()` (git HEAD tree hash +
environment MD5). If unchanged, all previously computed hashes are reused.

Otherwise, for each module, `generate_image_hash()` calls
`module_image_name()` which assembles a hash input file and computes its MD5.
See Section 5 for the full hash algorithm.

### Phase 4: Registry Probing

`find_image_digests()` takes the `all-images.<BUILD_HASH>` file (a flat list
of every source and target image name) and probes them in parallel:

```sh
sort "$1" | uniq \
  | xargs -I @ -P 16 sh stackgres-k8s/ci/build/build-functions.sh find_image_digest @
```

Each `find_image_digest()` call attempts `docker manifest inspect -v` (remote)
or `docker inspect` (local, when `SKIP_REMOTE_MANIFEST=true`). Results are
written to individual `target/image-digests.<TAG>` files, then concatenated.

### Phase 5: Per-Module Build

`build_image()` (line 389) checks three conditions in order:

1. **Remote cache hit**: image digest found in `image-digests.<BUILD_HASH>`
   -> skip build, only extract parent artifacts via `copy_from_image()`.
2. **Local cache hit**: `docker inspect` succeeds for the image name
   -> skip build, only extract parent artifacts.
3. **Cache miss**: invoke `build_module_image()`, then `docker push` (unless
   `SKIP_PUSH=true`).

The `DO_BUILD` env var and `DO_BUILD_MODULES` list override these checks to
force rebuilds.


## 5. Content-Addressable Hashing

The hash for each module is computed by `module_image_name()` (lines 23-82).
The function assembles a file at `target/<MODULE>-hash` containing the
following inputs, concatenated in order:

```
1. BUILDER_VERSION major.minor   (e.g. "1.0", from ${BUILDER_VERSION%.*})
2. SOURCE_IMAGE_NAME             (parent's computed image name, or null)
3. Module JSON config            (full .modules["<name>"] object)
4. Evaluated build_env           (shell-expanded, sorted by key)
5. [if custom dockerfile]:
   a. path_hash of Dockerfile
   b. path_hash of each artifact (if present on disk)
6. filtered_sources output       (stdout of each command in filtered_sources[])
7. path_hash of each source      (git tree SHA for each sources[] entry)
```

The assembled file is then hashed:

```sh
MODULE_HASH="$(md5sum "stackgres-k8s/ci/build/target/$MODULE-hash" | cut -d ' ' -f 1)"
```

The final image name is:

```
registry.gitlab.com/ongresinc/stackgres/build/<MODULE>:hash-<MD5>[-<PLATFORM>]
```

The platform suffix (e.g. `-linux-x86_64`) is appended only when
`platform_dependent: true`.

### path_hash()

```sh
path_hash() {
  git --git-dir "stackgres-k8s/ci/build/target/.git" rev-parse HEAD:"$FILE"
}
```

This uses git's content-addressable tree to produce a stable hash of any file
or directory, independent of timestamps. The shadow git repo in `target/.git`
is initialized by `init_hash()` (lines 302-337), which either copies the
project's `.git` directory or creates a fresh repo and commits the working tree.

### Hash Cascading

Because a module's hash input includes its parent's `SOURCE_IMAGE_NAME` (which
itself is a hash-tagged image), any change to an ancestor automatically
invalidates all descendants. This provides transitive cache invalidation without
explicitly tracking the full dependency tree in the hash function.


## 6. Dependency Resolution

### Algorithm (build.sh, lines 24-49)

```
Input:  MODULES = list of user-requested module names
Output: COMPLETED_MODULES = topologically-sorted list (ancestors first)

for each MODULE in MODULES:
    CURRENT_MODULE = MODULE
    while true:
        PARENT_MODULE = stages[CURRENT_MODULE]   // lookup in config.json
        if PARENT_MODULE is undefined:
            error: module stage not defined
        if PARENT_MODULE is null:
            break   // reached a root
        if PARENT_MODULE not in COMPLETED_MODULES:
            prepend PARENT_MODULE to COMPLETED_MODULES
        CURRENT_MODULE = PARENT_MODULE
    if MODULE not in COMPLETED_MODULES:
        append MODULE to COMPLETED_MODULES
```

Key properties:
- Ancestors are prepended, so they always appear before their dependents.
- Duplicate detection via `grep -qF` on the space-delimited list prevents
  redundant entries when multiple requested modules share ancestors.
- The `SKIP_DEPENDENCIES` env var bypasses this resolution, building only the
  explicitly requested modules (assumes ancestors are already available).

### source_image_name() Resolution

`source_image_name()` (line 348) looks up a module's parent in `stages[]`.
If the parent is `null`, it returns the global `target_image` (currently unused
in practice). If the parent is another module, it calls `image_name()` which
reads the precomputed `image-hashes.<BUILD_HASH>` file to get the parent's
hash-tagged image name.

### Artifact Inheritance

When a module builds, `copy_from_image()` extracts the parent's artifacts from
its Docker image into the project working directory. This means the child
module sees all files its parent produced (Maven JARs, compiled assets, etc.)
on the local filesystem before its own `build_commands` execute.


## 7. Caching Strategy

The framework employs five layers of caching, from fastest to slowest:

### L1: Config Parsing Cache

`init_config()` stores the MD5 of `config.yml` in `target/config.yml.md5`.
If the hash matches on subsequent runs, the `yq` conversion is skipped.

### L2: Project Hash (Full Skip)

`generate_image_hashes()` computes a `project_hash()` consisting of:
- `git rev-parse HEAD:` (the tree object of the entire working directory)
- `env -0 | sort -z | md5sum` (all environment variables)

If this composite hash matches `target/project_hash.<BUILD_HASH>`, the entire
hash generation phase is skipped and previously computed hashes are reused.
This is the fastest path for repeated invocations with no changes.

### L3: Remote Registry Cache

`find_image_digests()` probes the remote registry for each computed image tag.
A 16-way parallel `xargs` invocation uses `docker manifest inspect` to check
existence. Found images are recorded in `target/image-digests.<BUILD_HASH>`.

During `build_image()`, if a module's image exists in this digest file, the
build is skipped entirely.

### L4: Local Docker Cache

If the remote probe finds nothing (or `SKIP_REMOTE_MANIFEST=true`), `build_image()`
falls back to `docker inspect` to check for a locally available image.

### L5: Build Artifact Cache

Modules with a `cache` field (e.g. `.m2/` for Maven) have those directories
extracted from the build container after each build. On subsequent builds, these
directories are available in the project workspace, providing warm caches for
package managers even when the Docker image itself must be rebuilt.


## 8. Build Module Image Process

`build_module_image()` (lines 188-289) executes the following steps:

```
build_module_image(MODULE, SOURCE_IMAGE_NAME, IMAGE_NAME)
    |
    |-- 1. Read config: BUILD_IMAGE_NAME, TARGET_IMAGE_NAME, MODULE_PATH,
    |      MODULE_DOCKERFILE, MODULE_ARTIFACTS
    |
    |-- 2. copy_from_image(SOURCE_IMAGE_NAME)
    |      Extract parent artifacts to project working directory
    |      via: docker run --rm <parent_image> sh -c 'cp -a /project/. /project-target/.'
    |
    |-- 3. [if BUILD_IMAGE_NAME != null]:
    |      |
    |      |-- 3a. pre_build_in_container(MODULE, BUILD_IMAGE_NAME)
    |      |       Run pre_build_commands as pre_post_build_uid (or BUILD_UID)
    |      |
    |      |-- 3b. build_in_container(MODULE, BUILD_IMAGE_NAME)
    |      |       Run build_commands as BUILD_UID
    |      |
    |      |-- 3c. post_build_in_container(MODULE, BUILD_IMAGE_NAME)
    |              Run post_build_commands as pre_post_build_uid (or BUILD_UID)
    |
    |-- 4. Generate .dockerignore
    |      Starts with '*' (ignore everything), then '!<artifact>' for each
    |      artifact path -- only artifacts are included in the Docker context.
    |
    |-- 5. Generate Dockerfile at target/Dockerfile.<MODULE>
    |      |
    |      |-- [if custom dockerfile.path]:
    |      |   Prepend ARG declarations for dockerfile.args
    |      |   Apply dockerfile.seds transformations via piped sed
    |      |
    |      |-- [if auto-generated]:
    |      |   FROM "$TARGET_IMAGE_NAME" as target
    |      |   ARG BUILD_UID; USER $BUILD_UID; WORKDIR /project
    |      |   COPY ./<artifact> /project/<artifact>  (for each artifact)
    |
    |-- 6. docker build
           --platform <BUILD_PLATFORM or get_platform()>
           --build-arg BUILD_UID=<uid>
           --build-arg TARGET_IMAGE_NAME=<target_image>
           --build-arg <key>=<evaluated_value>  (for each dockerfile.args entry)
           -f target/Dockerfile.<MODULE> .
```

### Container Execution Model

`run_commands_in_container()` (lines 146-186) runs commands inside the
`build_image` container with:
- The project directory mounted at `/project`
- Docker socket mounted for Docker-in-Docker operations
- Build environment variables evaluated and injected
- Configurable UID via `--user`
- `--pull always` unless `SKIP_REMOTE_MANIFEST=true`


## 9. Platform Handling

### Configuration

Supported platforms are declared in the top-level `platforms` list:

```yaml
platforms:
  - linux/x86_64
  - linux/aarch64
```

### Module Behavior

When `platform_dependent: true`, the framework:

1. **Hash generation**: iterates over all platforms, producing a separate image
   tag per platform with a `-<os>-<arch>` suffix (e.g. `hash-abc123-linux-x86_64`).
2. **Build execution**: uses `--platform` flag on all `docker run` and
   `docker build` commands. Defaults to `get_platform()` which returns
   `$(uname | tr upper lower)/$(uname -m)`.

When `platform_dependent: false` (the default), a single image is produced
with no platform suffix. This is appropriate for platform-independent artifacts
like Helm charts.

### Override

The `BUILD_PLATFORM` environment variable overrides `get_platform()` for all
Docker commands, allowing cross-platform builds.


## 10. Extension Points

### Docker Wrapper Functions

All Docker CLI calls go through thin wrapper functions (lines 799-837):

```sh
docker_inspect()          { docker inspect "$@"; }
docker_run()              { docker run "$@"; }
docker_build()            { docker build "$@"; }
docker_push()             { docker push --platform=... "$@"; }
docker_tag()              { docker tag "$@"; }
docker_rm()               { docker rm "$@"; }
docker_manifest_inspect() { docker manifest inspect "$@"; }
docker_buildx_inspect()   { docker buildx inspect "$@"; }
```

These can be overridden by sourcing `build-functions.sh` and redefining the
functions, enabling dry-run modes, test mocking, or alternative container
runtimes.

### Direct Function Invocation

The script's tail guard (lines 839-842) enables direct function calls:

```sh
if [ "$(basename "$0")" = "build-functions.sh" ] && [ "$#" -ge 1 ]
then
  "$@"
fi
```

This is used by `build-gitlab.sh` for extraction (`extract` function), by
`find_image_digests` for parallel probing, and by `config.yml` itself for
runtime hash lookups (`get_module_hash`).

### Type-Agnostic Design

The `type` field is a label only. The build system does not branch on module
type anywhere in the code. All behavioral differences between, say, a `java`
module and a `helm` module come from their specific `build_image`,
`build_commands`, `artifacts`, and `dockerfile` configuration. This makes
adding new module types a pure configuration change.

### filtered_sources

The `filtered_sources` field allows arbitrary shell commands whose output
becomes part of the hash input. This is used to normalize version-sensitive
content (e.g. stripping patch versions from `pom.xml` via `redact-version.sh`)
so that version bumps do not invalidate caches when the actual source code
has not changed.

### dockerfile.seds

The `dockerfile.seds` field applies sed transformations to custom Dockerfiles
at build time. This enables path rewriting (e.g. adjusting COPY source paths
to be relative to the module path) without maintaining separate Dockerfile
variants.


## 11. Environment Variables Reference

| Variable               | Default                        | Description |
|------------------------|--------------------------------|-------------|
| `DO_BUILD`             | (unset)                        | Set to `true` to force rebuild of all modules regardless of cache state. |
| `DO_BUILD_MODULES`     | (unset)                        | Space-separated list of module names to force rebuild. Other modules still use cache. |
| `SKIP_PUSH`            | (unset)                        | Set to `true` to skip `docker push` after building. |
| `SKIP_REMOTE_MANIFEST` | (unset)                        | Set to `true` to skip remote registry probing; use only local Docker cache. |
| `SKIP_DEPENDENCIES`    | (unset)                        | Set to `true` to skip dependency resolution; build only the explicitly requested modules. |
| `BUILD_PLATFORM`       | `$(get_platform)`              | Override the target platform for all Docker commands (e.g. `linux/aarch64`). |
| `BUILD_SKIP_PRE_BUILD` | (unset)                        | Set to `true` to skip `pre_build_commands` execution. |
| `BUILD_SKIP_BUILD`     | (unset)                        | Set to `true` to skip `build_commands` execution. |
| `BUILD_SKIP_POST_BUILD`| (unset)                        | Set to `true` to skip `post_build_commands` execution. |
| `BUILD_UID`            | `$(id -u):$(docker socket gid)`| UID:GID used for build container commands. Derived from current user and Docker socket group. |
| `DEBUG`                | (unset)                        | Set to `true` to enable shell trace (`set -x`) for all build operations. |
| `SHELL_XTRACE`         | (unset)                        | Passed into build containers; set to `-x` when `DEBUG=true`. Used by build scripts that accept trace flags. |
| `PROJECT_PATH`         | `$(pwd)`                       | Absolute path to the project root. Used for volume mounts in Docker commands. |
| `CI_REGISTRY`          | (unset)                        | Docker registry URL for `docker login` in `build-gitlab.sh`. |
| `CI_REGISTRY_USER`     | (unset)                        | Registry username for `docker login`. |
| `CI_REGISTRY_PASSWORD` | (unset)                        | Registry password for `docker login`. |
| `DOCKER_AUTH_CONFIG`   | (unset)                        | Path to a Docker auth config JSON file. Copied to `$HOME/.docker/config.json` by `build-gitlab.sh`. |
| `DOCKER_BUILD_OPTS`    | (unset)                        | Additional flags passed to `docker build` (e.g. `--no-cache`). |
| `CONTAINER_NAME`       | `buildw-<hex timestamp>`       | Container name used by `ciw` for the CI wrapper container. |
| `IMAGE`                | (auto-detected from .gitlab-ci.yml) | Docker image used by `ciw` for the CI wrapper container. |

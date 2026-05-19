# Plan: curated Vector agent build under one Bazel workspace

## Context

`logs/build/` is the Bazel workspace that produces the curated Vector OCI
image used as the **agent**: a per-host process that tails Postgres `csvlog`
files and ships them via OTLP/HTTP to a configured collector.

The agent is curated — Vector exposes a per-component Cargo feature for
every source/sink/transform; this build enables only the four features the
agent actually needs (`sources-file`, `transforms-remap`, `sinks-console`,
`sinks-opentelemetry`). The result is a substantially smaller binary with
a smaller dependency graph than the upstream "everything-on" Vector
distribution. See `doc/INVESTIGATION.md` for the size/feature analysis and
the rationale for each Cargo and rustc setting.

Design decisions:

| Decision | Choice |
|---|---|
| Build artifact | One curated OCI image — `//agent:image` — Vector with only the features the agent needs. |
| Min PG version | 17. The agent's `pg_parse` requires the canonical 26-column csvlog layout; a different count emits a `parse_error` instead of silently mis-parsing. |
| Multi-instance agent | A single agent process can service N Postgres instances. The orchestrator templates the yaml with one `(file_source, tag_transform)` pair per instance, each pair hardcoding the instance's UUID. The shared downstream (`pg_parse`, `pg_to_otlp`, sinks) consumes from however many tag transforms exist. |
| OTLP shape | Standards-compliant `ExportLogsServiceRequest` (`encoding.codec: otlp` over HTTP). `instance.uuid` rides as a resource attribute. |
| Image base | `gcr.io/distroless/cc-debian12`, digest-pinned. Minimal runtime, glibc 2.36; the binary is built with a matching `cargo zigbuild --target=...gnu.2.36`. |
| Reproducibility | `SOURCE_DATE_EPOCH`, stable cargo `WORK` dir, `--remap-path-prefix`, content-addressed toolchain tarballs, digest-pinned base image. Verified by `verify/repro_check.sh` (double-cold-build manifest-digest comparison). |

---

## Directory layout

```
logs/build/
├── .bazelversion                # 8.4.2
├── .bazelrc                     # lockfile, SOURCE_DATE_EPOCH, action_env passthrough
├── .gitignore                   # bazel-* symlinks
├── MODULE.bazel                 # module(name="vector-agent"); deps + http_archives
├── MODULE.bazel.lock            # tracked dep graph
├── BUILD.bazel                  # workspace-root marker (intentionally empty)
├── doc/
│   ├── INVESTIGATION.md         # build-options investigation (PG csvlog, OTLP nuances)
│   └── PLAN.md                  # this document
├── rust/
│   ├── BUILD.bazel              # exports BUILD.vector.bazel for MODULE.bazel
│   ├── BUILD.vector.bazel       # @vector filegroup
│   ├── vector_binary.bzl        # vector_binary() macro: parameterized cargo-zigbuild
│   └── patches/
│       ├── BUILD.bazel
│       └── 0001-disable-default-features.patch
├── agent/
│   ├── BUILD.bazel              # vector_binary + oci_image stack for the agent
│   └── pipeline.yaml.template   # agent runtime config template; slon substitutes ${INSTANCE_UUID} / ${OTLP_ENDPOINT} at deploy time
└── verify/
    ├── BUILD.bazel
    ├── repro_check.sh           # double-cold-build manifest-digest comparison
    ├── e2e_agent_test.sh        # agent + fake OTel collector
    └── fixtures/
        ├── postgresql-test.csv  # 3-row PG17 csvlog (LOG/ERROR/WARNING; LOG carries a checkpoint message)
        └── collector-config.yaml # OTel Collector config used by the agent test
```

---

## Build infrastructure

### `rust/vector_binary.bzl` — the parameterized macro

`vector_binary(name, features)` wraps a genrule that:

1. Stages the `@vector` source tree into `/tmp/vector-build-bazel-<name>` (a
   stable, name-namespaced location — required for reproducibility, since
   openssl-src bakes its install prefix into the static archive at C
   compile time, and rustc's `--remap-path-prefix` doesn't reach C-archive
   contents).
2. Runs `cargo zigbuild --release --target x86_64-unknown-linux-gnu.2.36
   --no-default-features --features <features> --bin vector` with the
   production rustc flags from `doc/INVESTIGATION.md` §7 (lto=fat, opt=3,
   cgu=1, panic=abort, strip=symbols).
3. Sets `RUSTFLAGS=--remap-path-prefix=$WORK=/build` belt-and-suspenders so
   panic-location and `track_caller` strings (which survive
   `strip=symbols`) become abstract paths too.

The genrule is `local = 1`; cargo + rustup are taken from the host's PATH
(passed through via `.bazelrc`'s `--action_env=PATH/HOME/CARGO_HOME/RUSTUP_HOME`).
Vector's `rust-toolchain.toml` pins Rust 1.92, which rustup installs on
first run if missing.

zig (0.16.0) and cargo-zigbuild (0.22.3) are fetched as content-addressed
`http_archive`s in `MODULE.bazel`. `protoc` comes from the BCR `protobuf`
module (30.2). Distroless `cc-debian12` is pinned by manifest digest in
`oci.pull`.

### `agent/BUILD.bazel`

Calls `vector_binary("vector", "sources-file,transforms-remap,sinks-console,sinks-opentelemetry")`,
then assembles the binary into a distroless OCI image via `pkg_tar` +
`oci_image` + `oci_load` + `oci_push`. Repo tag: `vector-agent:dev`.

---

## Agent runtime configuration

`agent/pipeline.yaml.template` is a **single-tenant** runtime config:
one source, one tag transform, baked-in at template-fill time. The
agent runs co-located with `slon`+Patroni+Postgres in a single
container (one slon = one Postgres instance), so the multi-tenant
clone-pair pattern earlier drafts considered does not apply. See the
subsystem `DESIGN.md` for the rationale.

```yaml
sources:
  pg_csvlog:
    type: file
    include: [/var/log/postgresql/postgresql-*.csv]
    multiline: { ... }

transforms:
  tag_instance:
    type: remap
    inputs: [pg_csvlog]
    source: |
      .instance_uuid = "${INSTANCE_UUID}"

  pg_parse:
    inputs: [tag_instance]
    ...
```

`slon` consumes the template at startup and produces the runtime
`/etc/vector/vector.yaml`: substitutes `${INSTANCE_UUID}` (its known
slon-level constant) and `${OTLP_ENDPOINT}` (from its runtime
config), and conditionally drops the `otlp_remote` sink if the user
has not opted into remote log shipment. The `${VAR}` syntax is also
Vector-native env-var interpolation, so a passthrough mode (slon
sets the env vars on the agent process; Vector interpolates at
config load) works from the same template — that is what the e2e
verify script does.

### `pg_parse`

PG17+ requires the canonical 26-column csvlog. The transform unconditionally
assigns `cols[0]…cols[25]` to named fields; if `length(cols) != 26` it
sets `.parse_error` instead so a future PG18 format change surfaces
loudly.

### `pg_to_otlp`

Builds the `ExportLogsServiceRequest` envelope. Resource attributes:
`service.name=postgresql`, `db.system=postgresql`, `db.namespace=<from
record>`, `instance.uuid=<slon-substituted constant>`. Per-record attributes:
all 23 fields outside of `log_time` / `severity` / `message` (which go
into `timeUnixNano` / `severityText` / `body.stringValue`). Keys use
OTel-conventional `db.*` where the spec has an equivalent and `pg.*`
where it doesn't (vxid, leader_pid, query_id, internal_query, context,
location, query_pos, session_line_num, session_start_time, hint, detail).

---

## Verification

| Script | What it does |
|---|---|
| `verify/e2e_agent_test.sh` | Spins up an OTel Collector + agent Vector in a private docker network. Feeds `verify/fixtures/postgresql-test.csv`. Asserts: console sink emits 3 PG-stderr-format lines; collector receives OTLP with `instance.uuid`, `db.system=postgresql`, all 3 severities (Info(9)/Error(17)/Warn(13)), `db.application=psql`, `pg.leader_pid=1234`, etc. |
| `verify/repro_check.sh` | Double-cold-build manifest-digest comparison. Default target is `//agent:image`; pass other targets explicitly. ~10–15 min per target. |

These are **build-level** verifications — they exercise the agent image in
isolation, with a generic OTel Collector capturing what the agent emits.

---

## End-to-end smoke

```bash
cd ~/github/ssh/ongres/ondb/logs/build

# 1. Build the image
bazel build //agent:image

# 2. Load into the local OCI runtime
bazel run //agent:image_load

# 3. Functional verification
./verify/e2e_agent_test.sh           # agent + fake OTel collector

# 4. Reproducibility (slow)
./verify/repro_check.sh              # //agent:image
```

Reference figures (current build):

| | Agent |
|---|---|
| Binary size | 31.9 MB stripped |
| Image size (compressed) | 41.1 MB |
| Image layers | 19 (1 + 18 distroless) |
| Max GLIBC referenced | 2.34 |

---

## Open follow-ups

* **Multi-instance fixture in agent e2e.** Today's agent e2e uses a
  single placeholder UUID. A future test could template multiple
  `(source, tag)` pairs to verify the fan-in pattern under the
  orchestrator's expected workload.

* **`api` feature for `vector top` introspection.** Costs ~1–2 MB of
  binary size. Worth opting in if/when operational visibility becomes a
  pain point.

* **Upstream patch contributions.** See `doc/INVESTIGATION.md` §9 for
  candidate refactorings (gating `apache-avro` and `prost`/`prost-reflect`
  in `lib/codecs/Cargo.toml` behind features) that would shrink every
  curated build, not just ours.

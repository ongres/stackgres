# Custom Vector build for Postgres log pipeline


Investigation summary documenting the build options, trade-offs, and verified
configuration for a curated Vector binary that tails Postgres logs and ships
them via OTLP/HTTP. Written 2026-04-30.


## 1. Goal


A Vector binary that:

* Tails Postgres `csvlog` files. Minimum supported PG version: **17**
  (canonical 26-column csvlog format). Older versions are out of scope; an
  unexpected column count surfaces as `parse_error` rather than silently
  mis-parsing.
* Parses each record and emits two outputs:
   * **Console**: a Postgres-stderr-like one-liner for local dev convenience.
   * **OTLP/HTTP**: standards-compliant `ExportLogsServiceRequest` protobuf to a
     remote OTLP collector.
* Is **as small as practical without trading away production performance**.


## 2. Two questions answered up front


* **Custom-feature builds?** Yes. Vector exposes a per-component Cargo feature
  for every source/sink/transform; a curated build with only the components you
  use is the official path. See `Cargo.toml:511-1138`.
* **Plugin / `.so` modules at runtime?** No. Vector statically links every
  enabled component. Components register at compile time via the `inventory`
  crate. A 2020 RFC (`rfcs/2020-04-15-2341-wasm-plugins.md`) proposed WASM
  plugins; it was abandoned because the WASM runtime alone would have *grown*
  the binary.


## 3. Module selection is compile-time, not runtime


Component selection is **baked into the binary** via Cargo features. To add a
new source/sink/transform later you must rebuild. Concretely:

```bash
cargo build --release \
  --no-default-features \
  --features "sources-file,transforms-remap,sinks-console,sinks-opentelemetry" \
  --bin vector
```

The features used in this investigation:

| Feature | Purpose |
|---|---|
| `sources-file` | Tail Postgres log files (`/var/log/postgresql/*.csv`). |
| `transforms-remap` | VRL transforms (`parse_csv`, OTLP envelope construction). |
| `sinks-console` | Human-readable text output to stdout/stderr. |
| `sinks-opentelemetry` | Pulls `sinks-http` + `codecs-opentelemetry` (OTLP encoder). |

Useful umbrellas if you want to keep more components on hand at the cost of
size: `sources`, `sinks-logs`, `transforms`. Lookup table is in
`Cargo.toml:511-1138`.

`--no-default-features` is important: the default feature set
(`default-no-api-client`) pulls in everything via the `base` feature. Without
`--no-default-features`, your `--features` flag is additive on top of the full
set.


## 4. Configuration model — file-driven, with hot-reload mechanisms


Vector does **not** have a "send a new config over the wire" REST API in the
upstream. The configuration model is file-driven, with several mechanisms to
make this feel like a remote-config workflow:

| Mechanism | Where | Use case |
|---|---|---|
| `--config <path>` | CLI flag | Static start-up config. |
| `--watch-config` | CLI flag | Vector watches the config file with inotify and hot-reloads on change. Combine with an external system that writes the file. |
| `SIGHUP` | Process signal | Manual / orchestrator-triggered reload from disk. |
| **`http` provider** | Config block (always compiled in) | Vector polls a remote URL for its config on an interval; reloads on change. |
| Vector API | Optional `api` feature | **Read-only** GraphQL: introspect components, tap event streams, fetch metrics. **Not for config changes.** |

The two distinctions worth pinning down:

* The `api` Cargo feature gates the **GraphQL introspection API** (read-only).
  Pulls in `tonic` + `prost` + `tonic-reflection` (~1 MB). Disabled in the
  builds in this investigation.
* The `http` config **provider** is unconditional (`src/providers/mod.rs`). It
  is in the binary even with our minimal feature set.

### Recommended for "configured by API" workflows


The `http` provider is the closest fit for that pattern. It works like this:

```yaml
# vector.yaml — minimal bootstrap config
provider:
  type: http
  url: https://config-server.example.com/vector/postgres-node-1.yaml
  poll_interval_secs: 30
  config_format: yaml
  request:
    headers:
      authorization: "Bearer ${CONFIG_TOKEN}"
```

The remote config server then serves the *real* config — including the entire
sources/transforms/sinks block — on that URL. Vector reloads the topology in
place when the served config changes. This is a true GitOps-friendly setup, no
write access to local files required.

If you also want **introspection** (component status, metrics, log tap from
`vector top` / `vector tap`), build with `--features
sources-file,transforms-remap,sinks-console,sinks-opentelemetry,api` and add
`api: { enabled: true, address: "0.0.0.0:8686" }` to the runtime config. Costs
~1-2 MB of binary size.


## 5. The OTLP nuance you must know


The `opentelemetry` sink in this Vector version is a thin wrapper over the
`http` sink with a single-variant enum for the protocol
(`src/sinks/opentelemetry/mod.rs:36-39`):

```rust
pub enum Protocol {
    Http(HttpSinkConfig),
}
// "Currently only HTTP is supported, but we plan to support gRPC."
```

So:

* **Transport**: HTTP/1.1 only. **No gRPC.** Many OTLP collectors listen on both
  port 4318 (HTTP) and 4317 (gRPC); only 4318 will work today. Watch the source
  for `Protocol::Grpc`; when it lands, the YAML config alone changes.
* **Body encoding**: Two codec choices, with very different downstream
  consequences:
  * `encoding.codec: json` — emits **Vector's own log-event JSON shape**, *not*
    OTLP's `ResourceLogs` schema. A standards-compliant OTLP collector will
    reject it. Only Vector-aware consumers parse this.
  * `encoding.codec: otlp` — emits **standards-compliant OTLP protobuf**
    (`application/x-protobuf`). **But** it requires the event to already have
    `resourceLogs` at the top level. For Postgres logs you must build that
    envelope in a VRL transform first. See `../agent/pipeline.yaml.template`
    for the working construction.
* **Batching efficiency**: Vector's HTTP sink concatenates the encoded
  protobuf bodies of all events in a batch into a single POST body. Proto3
  merge semantics make this a valid `ExportLogsServiceRequest` (repeated
  fields like `resourceLogs[]` accumulate), so standards-compliant collectors
  parse it correctly. In our smoke test 3 events became 2 POSTs of 982 + 372
  bytes. The space inefficiency is that each event keeps its own
  `resourceLogs[]` entry, duplicating the resource attribute block per event
  rather than sharing one. To merge events under a single resource block you
  would need a `reduce` transform (Cargo feature `transforms-reduce`) that
  aggregates `logRecords[]` arrays before the encoder sees them. Worth
  measuring at your event rate before adding that complexity.

The choice of OTLP-over-HTTP here is intentional — it preserves the optionality
of swapping Vector for the OTel Collector or another OTLP shipper later. The
Vector-native `vector` sink would lock the deployment to Vector.


## 6. Build size journey


Single binary, same pipeline (file → remap → console + OTLP), all on Rust 1.92
(pinned via `rust-toolchain.toml`). Verified functional with the CSV and JSON
configs at the end of every row.

| Variant | Profile | Size | Build time |
|---|---|---:|---:|
| Official downloaded | full features, stock release | 135.2 MB | n/a |
| Stock profile + curated features | `[profile.release] debug = false` only | 82.4 MB | 15m12s |
| **Sweet-spot** (`thin LTO`, `opt=3`) | thin LTO, cgu=1, abort, strip | 32.7 MB | 8m39s |
| **Production** (`fat LTO`, `opt=3`) | fat LTO, cgu=1, abort, strip | **30.6 MB** | 10m52s |
| Size-aggressive | fat LTO, **opt=z**, cgu=1, abort, strip | 23.3 MB | 7m48s |
| Production + UPX `--best --lzma` | + post-build pack | ~8 MB | +2s |

Configuration knobs that matter:

### `opt-level`

* `3` (stock release default): full performance. **Use this for production.**
* `s`: optimize for size, keeps loop vectorization. ~10% smaller, 2-8% slower.
* `z`: aggressive size opt, *disables* vectorization. ~15% smaller, 5-20%
  slower on hot loops. Acceptable for a mostly I/O-bound pipeline; meaningful
  hit on VRL-heavy workloads.

### `lto` (link-time optimization)

This is the most important flag. LTO is a **Pareto improvement on perf and
size** simultaneously — the only cost is build time. Cross-crate inlining and
dead-code elimination is what kills the unused tonic paths, the unused
`apache_avro` branches, and so on.

| Value | Build time | Size | Perf |
|---|---|---|---|
| `false` (stock default) | Fastest | Largest | Baseline |
| `"thin"` | +2-3× | -10-25% | +5-15% |
| `"fat"` (used here)   | +5-10× | -15-30% | +6-18% |

`thin` LTO captures ~95% of `fat` LTO's benefit at ~30% of the build time and
runs in parallel. `fat` is single-threaded and serial; the marginal 1-3% perf
edge over `thin` is real but small. Either is dramatically better than no LTO.

### `codegen-units`

Default 16, our build uses 1. With LTO already on, the additional benefit is
small (+2-5%). Cheap insurance.

### `panic = "abort"`

Default `"unwind"` generates per-function landing pads and `.eh_frame` /
`.gcc_except_table` sections (~7 MB combined in our build). With `"abort"` the
process `SIGABRT`s on panic; the supervisor restarts it. **Right default for a
daemon under systemd / Kubernetes / supervisord.**

Trade-off: `catch_unwind` no longer works. Vector itself does not depend on
this; some library users do, which is one reason upstream defaults to
`"unwind"`.

### `strip = "symbols"`

Removes `.symtab`, `.strtab`, `.dynsym`, `.dynstr`. **Zero perf impact** —
symbols are not loaded into the icache. The trade-off is purely "do I need
function names in stack traces?" If you keep an unstripped artifact archived
for symbolication, take the win.


### Why doesn't the official build use any of these?


* **CI cost across N targets**. Vector ships ~10 target triples; full LTO turns
  a 5-min CI build into 25 min × 10 = 4+ hours per release.
* **`panic = "abort"` changes documented behavior**. The Vector crate is also
  published as a library; `catch_unwind` is part of the implicit contract.
* **Conservative LLVM**. `lto = "fat" + cgu = 1` occasionally surfaces
  codegen-corner-case bugs across the matrix of target triples.
* **Generic distribution**. The official binary has every component on; their
  optimization is "works for everyone out of the box", not "tuned for one
  curated pipeline".

None of those reasons apply to a curated, single-target production build.


## 7. Recommended profile for production


```toml
[profile.release]
opt-level = 3
lto = "fat"
codegen-units = 1
panic = "abort"
strip = "symbols"
```

Apply via either:

* `.cargo/config.toml` at the workspace root (auto-applied to every release
  build in this checkout — preferred), **or**
* `--config 'profile.release.X = Y'` flags on the cargo command (no file
  changes; useful in CI scripts).

In a more powerful environment:

* `lto = "fat" + codegen-units = 1` is single-threaded at link time but parallel
  during compile. A many-core machine speeds up the *compile* phase but not the
  final link. Expect ~10-15 minutes wall-clock on a 16-core box.
* RAM during link can spike to 8-12 GB for fat-LTO of this binary; size your
  build host accordingly.
* For incremental development, run an unoptimized debug build (`cargo build`)
  separately. The release artifact (in whichever target directory the build
  is configured for) is the shipping deliverable.

Reference result on the development hardware: **30.6 MB binary, 10m52s
wall-clock**, verified on both CSV and JSON Postgres log fixtures.


## 8. Vector OTLP-strict configuration


See `../agent/pipeline.yaml.template` in this directory. Two-stage pipeline:

1. `pg_parse` — parses CSV into named fields, formats `.message` for the console
   sink.
2. `pg_to_otlp` — replaces the event with a fully-formed
   `ExportLogsServiceRequest` envelope (`resourceLogs.scopeLogs.logRecords`).

The console sink reads from `pg_parse` (text-formatted output); the OTLP sink
reads from `pg_to_otlp` (OTLP-shaped events) and uses `encoding.codec: otlp` to
emit binary protobuf with `Content-Type: application/x-protobuf`.

For `jsonlog` (Postgres 15+), the parser stage can be replaced with the file
source's built-in `decoding.codec: json`, removing the multiline aggregation and
the `parse_csv` call. Field names are different
(`.timestamp`/`.user`/`.dbname`/`.error_severity`/`.message` vs.
`.log_time`/`.user_name`/`.database_name`/`.severity`/`.pg_message`); update the
`pg_to_otlp` block accordingly.


## 9. Open follow-ups


* **Upstream PR**: gate `apache-avro` and `prost`/`prost-reflect` in
  `lib/codecs/Cargo.toml` behind features (the `arrow`/`parquet` codecs already
  follow this pattern, lines 17-24, 80-82). Conservative estimate: -1.5 MB
  uncompressed, meaningfully more under UPX. Self-contained refactor, plausibly
  upstreamed.
* **Watch for `Protocol::Grpc`** in `src/sinks/opentelemetry/mod.rs`. When it
  lands, you can flip transports without a config rewrite — only the
  `protocol.type` field changes.
* **Batch efficiency under one resource**: see the batching note in section 5.
  Adding `transforms-reduce` (feature `transforms-reduce`) and a small VRL
  block that merges `logRecords[]` under a single `resourceLogs[]/scopeLogs[]`
  entry would eliminate per-event resource-attribute duplication. Today the
  HTTP sink batches multiple events per POST via proto3 concatenation merge,
  which is valid OTLP but duplicates the resource block per event.
* **TLS stack consolidation**: `openssl_sys` (2.3 MB unconditional, declared at
  `Cargo.toml:374`) and `rustls` (~512 KB) are both linked. Removing one
  requires source changes; not a config-only win.

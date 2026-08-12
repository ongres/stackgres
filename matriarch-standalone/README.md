# `matriarch-next-quarkus/` — example adapter: CreateCluster end-to-end (draft)

The Quarkus/CDI wrapper around the framework-free `matriarch-next` core. Proves
the architecture with ONE use case wired end to end:

```
gRPC stackgres.api.v1.StackGresApi/CreateCluster
   → StackGresApiResource (@GrpcService)        – the only protobuf-aware layer (+ ProtoMapper)
   → Matriarch core  (createCluster)       – idempotency → persist desired → reconcile
   → InMemoryStateStore + FakeExecutor      – demo SPI impls (swap for SQLite/etcd, slony/k8s)
   ← streamed ClusterOperation: ACCEPTED → PROVISIONING → DONE/HEALTHY
```

Everything else on `StackGresApi` is intentionally `UNIMPLEMENTED`.

## Layout
- `StackGresApiResource` — gRPC service; maps request→domain, calls core, streams progress via a `ProgressSink`.
- `ProtoMapper` — the *only* place `stackgres.api.v1` protobuf meets the domain model (decision (b)).
- `InMemoryStateStore` — ConcurrentHashMap StateStore with resourceVersion CAS.
- `FakeExecutor` — reports HEALTHY; stands in for slony-linux / k8s executors.
- `MatriarchProducer` — CDI seam: builds the core from its two SPIs.
- protos copied into `src/main/proto/` (self-contained, per repo convention).

## Build & run

> **Requires a Java 21 runtime.** Quarkus 3.29's jboss-threads fails to
> initialize on Java 25 (`JDKSpecific$ThreadAccess`). The core targets `release=21`.

```bash
mvn -f ../matriarch-next/pom.xml install      # install the core first
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
mvn test                                       # runs the end-to-end slice test
mvn quarkus:dev                                # live gRPC on :9000 (plaintext, reflection on)
```

`quarkus.generate-code.grpc.scan-for-imports=all` lets codegen resolve
`google/rpc/status.proto` from `proto-google-common-protos`.

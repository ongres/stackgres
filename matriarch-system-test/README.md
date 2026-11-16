# StackGres system tests

Black-box system tests for the matriarch (CLI + api.v1), driven against a **running** environment. One set
of JUnit scenarios runs at every point of a 2-axis space — **target** (a direct matriarch or the cloud) ×
**agent** (in-process mock or a real slony/slon) — chosen purely by config; the scenarios don't know which.

Standalone on purpose (not in the aggregator reactor): the environment is long-lived; tests are run against
it by hand or from CI. Parent is `stackgres-parent`; the only build dependency is `stackgres-proto`.

## Layout
```
matriarch-system-test/
├── pom.xml                     # standalone; surefire argLine knob (systemtest.argLine)
└── src/
    ├── main/java/io/stackgres/systemtest/
    │   ├── TestConfig.java      # the single config seam (system prop → env var → default)
    │   ├── Target.java          # DIRECT | CLOUD
    │   ├── AgentMode.java        # MOCK | REAL
    │   ├── Poll.java             # await-a-condition helper
    │   ├── cli/                  # drives the `stackgres` process (Cli, CommandResult; PTY for exec/psql)
    │   ├── grpc/                 # api.v1 white-box client (MatriarchApiClient; TLS+JWT for cloud)
    │   └── agent/                # behavioral mock agents (MockSlony host + per-instance MockSlon)
    └── test/java/io/stackgres/systemtest/
        ├── BaseSystemTest.java   # cli + api + the skip-gates, target-agnostic
        ├── BaseNodeTest.java     # + a node (mock on direct; a real/registered one otherwise)
        ├── catalog/              # version/extension catalog (api.v1)
        ├── cluster/              # lifecycle, create-options, list/bulk, events, exec/tunnel/psql, logs
        ├── node/                 # tags, list/get, delete, logs, events
        ├── environment/          # environment list/get
        ├── context/              # CLI-local config round-trip
        ├── slon/                 # slon list/get (direct-only)
        ├── root/                 # version / status smoke
        └── cloud/                # cloud-only: auth smoke, aggregation, REST /events
```

## Flavors
The combinations that make sense. The same scenarios run at each; the skip-gates (below) decide what's
in or out.

| Flavor                           | target / agent          | Prereqs                                                               | Exercises                                                                               |
|----------------------------------|-------------------------|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| **A. direct + mock** *(default)* | direct / mock           | matriarch on :9000; `stackgres` on PATH. No containerd, no secrets.   | The whole matrix, hermetic & fast. Fault-injection + synthetic scenarios run here only. |
| **B. direct + real**             | direct / real           | A + containerd/CRI socket, slon image, **sudo**, `psql` binary.       | Same matrix against real slony/slon/PG; adds psql + real data-plane.                    |
| **C. cloud**                     | cloud / (env's agents)  | Deployed cloud URL + **JWT** + env id. No local matriarch/containerd. | The full api.v1 matrix (incl. create/delete/node writes) + `cloud/` scenarios. The cli.proto **data plane** (exec/tunnel/psql, slon/diagnostics/checkpoints) skips. **Use a dedicated test env** — it creates/deletes real clusters. |

There is no "cloud + mock": the mock dials the matriarch's plaintext SlonyService, which only makes sense for
a direct target.

## Config
One seam: a system property, else its `UPPER_SNAKE` env var, else a default. Nothing else reads the
environment.

| Property (`-D`)                                | Env var                       | Default                       | Notes                                                       |
|------------------------------------------------|-------------------------------|-------------------------------|-------------------------------------------------------------|
| `stackgres.systemtest.target`                  | `STACKGRES_SYSTEMTEST_TARGET` | `direct`                      | `direct` \| `cloud`                                         |
| `stackgres.systemtest.agent`                   | `…_AGENT`                     | `mock`                        | `mock` \| `real` (direct only)                              |
| `stackgres.systemtest.endpoint`                | `…_ENDPOINT`                  | `localhost:9000`              | port-less **+ tls ⇒ 443**                                   |
| `stackgres.systemtest.tls`                     | `…_TLS`                       | `false`                       |                                                             |
| `stackgres.systemtest.token`                   | `…_TOKEN`                     | *(none)*                      | the JWT (a CI secret)                                       |
| `stackgres.systemtest.environment`             | `…_ENVIRONMENT`               | *(none)*                      | environment id to scope to                                  |
| `stackgres.systemtest.http-base`               | `…_HTTP_BASE`                 | derived from endpoint         | override for the REST `/events` host                        |
| `stackgres.systemtest.cli`                     | `…_CLI`                       | `stackgres`                   | path / name / wrapper script                                |
| `stackgres.systemtest.command-timeout-seconds` | `…_COMMAND_TIMEOUT_SECONDS`   | `30`                          | per-command / gRPC deadline                                 |
| `stackgres.systemtest.healthy-timeout-seconds` | `…_HEALTHY_TIMEOUT_SECONDS`   | `60`                          | await cluster convergence                                   |
| `systemtest.argLine`                           | —                             | *(empty)*                     | extra fork JVM args, e.g. `-Djava.net.preferIPv4Stack=true` |

## Skip-gates
How one suite behaves correctly everywhere. A gated-out scenario shows as **skipped** (green), not failed.

- **`requireMock()`** — fault injection / synthetic pushes → **mock only**.
- **`requireReal()`** — needs a real Postgres (psql) → **real only**.
- **`requireDirect()`** — the cli.proto commands the cloud doesn't route: the PgWire data plane
  (exec/tunnel/psql) plus `slon`, diagnostics, checkpoints → **skip on cloud**.
- **`requireCloud()`** — the `cloud/` scenarios → **cloud only**.
- **`nodeIdOrSkip()`** — node-scoped reads skip if the environment has no bare-metal node (e.g. k8s-backed).

Net: A/B run everything; C runs the full api.v1 matrix (CRUD, node writes, logs, events) + `cloud/`, and
skips only the data-plane `requireDirect` + mock-only scenarios. Mutating tests create and **delete** real
clusters, so point C at a dedicated test environment.

## Running

Bootstrap once (any flavor): build + install the components the tests drive (and the proto jar this
standalone module resolves its stubs from), then make the CLI resolvable as `stackgres`.
```bash
# from stackgres/matriarch-system-test — installs proto, matriarch, cli, matriarch-standalone into ~/.m2
mvn -f ../pom.xml -pl proto,matriarch-model,matriarch,cli,matriarch-standalone -am -DskipTests install
# then put the built `stackgres` on PATH, or pass -Dstackgres.systemtest.cli=/path/to/stackgres
```

**A — direct + mock** (default; nothing else needed but a matriarch):
```bash
(cd ../matriarch-standalone && mvn quarkus:dev)   # a matriarch on :9000
mvn test                                          # localhost:9000, in-process mock agents
```

**B — direct + real** (real slony/slon via containerd). Prereqs: containerd running with a CRI socket, a
built+loaded `slon` image, a `psql` binary on PATH, and `sudo` (CRI access). Start the matriarch (as in A),
then launch a real slony pointed at it and run with `agent=real`:
```bash
# real slony: dials the matriarch, uses containerd via the CRI socket (runs under sudo for CRI access)
sudo STACKGRES_ENDPOINT_URL=localhost:9000 STACKGRES_ENDPOINT_TLS=false \
     STACKGRES_CRI_SOCKET_PATH=/run/containerd/containerd.sock \
     java -jar ../slony/target/quarkus-app/quarkus-run.jar
# once it has registered (check: stackgres environment node list):
mvn test -Dstackgres.systemtest.agent=real
```

**C — cloud** (points at a deployed **test** cloud, no local infra; creates/deletes real clusters):
```bash
mvn test -Dstackgres.systemtest.target=cloud \
         -Dstackgres.systemtest.endpoint=<host>:443 \
         -Dstackgres.systemtest.tls=true \
         -Dstackgres.systemtest.token=$JWT \
         -Dstackgres.systemtest.environment=<env-id>
# add -Dsystemtest.argLine=-Djava.net.preferIPv4Stack=true if the host resolves to an unreachable IPv6
```

## CI/CD mapping
| Pipeline stage           | Flavor            | Runner needs                                                                                  |
|--------------------------|-------------------|-----------------------------------------------------------------------------------------------|
| **PR gate**              | A (direct + mock) | JDK + Maven; start matriarch on :9000. No containerd, no secrets. The cheap, hermetic signal. |
| **Nightly / privileged** | B (direct + real) | A + containerd/CRI, slon image, sudo, `psql`. Not per-PR.                                      |
| **Post-deploy**          | C (cloud)         | The JWT (secret) + endpoint/env; a **dedicated test** cloud env (it creates/deletes clusters). |

Job definitions live as pipeline files (GitLab/GH Actions) that set the `-D` props above; this README is
the stable reference they point at. Consider asserting expected skip counts per flavor so a mis-gated
scenario can't silently vanish.

## Scope / status
Covered: the full single-instance Postgres matrix on the **direct** target in both **mock** and **real** —
lifecycle, start/stop/restart, create-options, list/tags, bulk/selection, events, data-plane
(exec/tunnel/psql), logs, node (tags/list/delete/logs/events), slon, catalog, environment, context, root.
Plus the **cloud** target: the same api.v1 matrix routed through the cloud (against a dedicated test env),
the `cloud/` scenarios (auth smoke, `environmentId`/multi-env aggregation, `GET /events`); only the cli.proto
data plane skips.

Next: **HA / multi-instance** (`--ha`, `-i/--instance` across exec/psql/tunnel/logs, replication roles);
**persistence/recovery** (matriarch restart → cluster adoption); the **SSO full-flow** smoke
(user/pass → token → `/account`); and smaller **engine/feature** slices (IvorySQL, `cluster metrics`).
Deferred: the operator/k8s target, and real bare-metal data-plane in the cloud.

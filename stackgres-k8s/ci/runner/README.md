# Running the e2e tests as pods

The build jobs run podman inside an unprivileged pod already: every container
they need runs in the namespaces of the pod, with the cgroups disabled, which is
what `/etc/containers/containers.conf` of the CI image configures.

The e2e tests can not work that way. Their default environment is kind, and a
kind node is a container that runs its own kubelet and its own containerd, so it
needs:

* **its own network namespace**, so that the nodes reach each other on a bridge
  of their own and the pod network of the cluster under test does not collide
  with the one of the cluster running the job;
* **its own cgroups**, that the kubelet of the node manages.

Neither is granted to a pod by default, and no unprivileged pod on a real
cluster can be made to grant them (see "The dead ends" below). What runs the e2e
tests on the rack instead is a **privileged podman sidecar**: the job container
stays unprivileged and drives the engine of a second container in the same pod.

## The shape

Every e2e job pod carries a container running
`podman system service --time=0` on a socket in a volume shared with the job
container. The job container is given:

| | |
|---|---|
| `CONTAINER_ENGINE` | `podman` |
| `CONTAINER_HOST` | the socket of the sidecar, so every `podman` is a remote client |
| `BUILD_UID` | `1000:1000`, so containers started through the socket write as the job user |
| build directory | mounted at the **same path** in the sidecar |
| kind cache | a volume mounted at the **same path** in both, `/var/lib/kind-cache` |

The sidecar is privileged and runs as a real root, with `hostUsers` left true.
Both are load-bearing: netavark writes sysctls per bridge and `/proc/sys` is
read-only below `privileged`, while the kubelet of a node has to `mkdir` in a
cgroup owned by the real root of the machine. This is the trust boundary docker
in docker already has — whoever reaches the socket can ask for a privileged
container — but it is confined to the sidecar instead of the job.

## What the framework does with it

`podman info --format '{{ .Host.ServiceIsRemote }}'` is what tells this runner
apart, and `container_engine_is_remote` in `stackgres-k8s/e2e/helpers` is the
predicate built on it. Where it is true:

* **the requirement checks are skipped.** `/dev/net/tun` and the cgroups of the
  job container describe the wrong machine: a rootful podman builds a bridge and
  a veth pair itself and never opens a tap device, and the cgroups that matter
  are the sidecar's. See `check_kind_podman_support` in `stackgres-k8s/e2e/envs/kind`.
* **`E2E_TEMP_PATH` has to name a directory both sides see.** Paths given to the
  engine are resolved on its side, so a path of the job container alone would be
  created there empty and the containerd cache would silently stop being one.
  This is the same knob a runner with a local engine already sets to a path of
  its host; `stackgres-k8s/ci/test/e2e-run-all-tests-gitlab.sh` defaults it to
  the shared volume when it finds the engine remote.
* **`CONTAINERS_CONF` selects `containers-kind.conf`**, which `envs/kind` does
  for any podman. A podman 4 client sends its own defaults to the service, so
  the configuration of the *client* decides the namespaces the nodes get.

## The dead ends

Both unprivileged paths this folder used to describe were measured on the rack
and neither works:

* **A pod with its own user namespace** (`hostUsers: false`). The premise was
  that the runtime hands the cgroups of the pod to the mapped root. It does not:
  `/sys/fs/cgroup` stays owned by the real root and read-only, the pod gets no
  `CAP_SYS_ADMIN`, and a node container fails with
  `mkdir: can't create directory '/sys/fs/cgroup/kubelet.slice': Permission denied`.
* **A device plugin advertising `/dev/net/tun`**, with podman rootless. It fixes
  the networking and stops at exactly the same cgroup wall. A rootful service
  does not need the device at all, which is why the plugin is gone.

## Files

| File | What it is |
|---|---|
| `kind-probe.yaml` | A pod shaped like a job pod |
| `kind-probe.sh` | Checks each requirement, then creates and deletes a cluster |
| `config.toml.example` | The parts of the runner configuration that matter |

`kind-probe.yaml` and `config.toml.example` still describe the unprivileged
attempt and have not been rewritten for the sidecar; the configuration that is
live on the rack is the reference. `kind-probe.sh` is aware of both: against a
remote service it only runs the checks that mean something there.

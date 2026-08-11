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

Both are exactly what the configuration of the build jobs takes away, and
neither is granted to a pod by default. `/etc/containers/containers-kind.conf`
of the CI image is the configuration that asks for them back; what follows is
what the pod has to provide so that podman can deliver them.

> **Nothing here has been run against the rack.** The files are written from the
> failure modes below and are meant to be corrected by what `kind-probe.sh`
> reports. What *has* been verified, on a workstation, is that the kind
> environment itself works with rootless podman once those requirements are met.

## The two blockers

**Networking.** In a pod, `/dev` has no `net/tun`. Both `pasta` and
`slirp4netns`, the two backends a rootless podman uses to connect a container
network to the outside, open that device, so a rootless podman can run a
container in the network of the pod but can not give it one of its own:

```
Error: setting up Pasta: pasta failed with exit code 1:
Failed to open() /dev/net/tun: No such file or directory
```

**Cgroups.** A rootless podman delegates a subtree of the cgroups to each
container. On a workstation systemd delegates that subtree to the user session;
a pod has no user session, and `/sys/fs/cgroup` belongs to root. Doing the
delegation from an init container does not work: with a private cgroup
namespace every container of the pod sees its *own* cgroup as the root of
`/sys/fs/cgroup`, so an init container would only ever chown a cgroup that is
deleted when it exits.

## Path 1 — give the pod its own user namespace

`hostUsers: false` maps the root of the pod to an unprivileged id of the node.
Two things follow, and they are precisely the two blockers:

* the runtime hands the cgroups of the pod to the mapped root, which is the
 delegation that was missing;
* podman runs as root *inside that namespace*, so it creates network namespaces
 and veth pairs directly and never reaches for `/dev/net/tun`.

The node keeps seeing an unprivileged process throughout: no capability is
granted, no device plugin is deployed, and nothing on the node is shared.

Requires the `UserNamespacesSupport` feature gate, a container runtime with
idmap mount support and a kernel 6.3 or later. **Check this first** — if the
rack satisfies it, path 2 is not worth building.

Apply `kind-probe.yaml`, run `kind-probe.sh` in it, and if the cluster comes up
merge the `pod_spec` of `config.toml.example` into the runner configuration.

## Path 2 — advertise `/dev/net/tun` and run podman rootless

Only if path 1 is not available. Deploy `tun-device-plugin.yaml`, request
`squat.ai/tun` in the job pod and make the job run as a non root user, which is
what makes podman choose its rootless mode. A `hostPath` on `/dev/net/tun` is
not a substitute: the device filter of the cgroups v2 denies opening the
character device to a container that did not receive it from the runtime.

This solves the networking blocker and **leaves the cgroup one open**: a
rootless podman that shares the user namespace of the node still has no
delegated cgroup subtree. `kind-probe.sh` checks that case explicitly. If the
cgroups of the pod turn out not to be writable, this path is a dead end and
there is nothing left to try short of a privileged pod, which is not on the
table.

## Path 3 — leave the e2e tests where they are

Keep the e2e jobs on the runners that have a docker daemon and let the rack
serve the build and image jobs, which podman already covers. This is the honest
outcome if path 1 is unavailable and path 2 stops at the cgroups, and it costs
nothing: the kind environment supports podman either way, which is what makes it
usable on a workstation without docker.

## Files

| File | What it is |
|---|---|
| `kind-probe.yaml` | A pod shaped like a job pod, path 1 by default |
| `kind-probe.sh` | Checks each requirement, then creates and deletes a cluster |
| `tun-device-plugin.yaml` | Path 2 only, advertises `/dev/net/tun` as `squat.ai/tun` |
| `config.toml.example` | The parts of the runner configuration that matter |

Once a path is proven, point the `stackgres-e2e-runner` tag in
`.gitlab-ci/e2e-test.yml` at the rack runner and set `CONTAINER_ENGINE=podman`
for those jobs.

/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch.clustername;

import java.net.http.HttpClient;
import java.util.Optional;

/**
 * A single cloud's best-effort attempt to read the managed Kubernetes cluster name from its node
 * metadata service. Implementations must never throw and must return quickly (short per-request
 * timeouts): on a non-matching cloud the endpoint is unreachable and the probe simply returns
 * {@link Optional#empty()}.
 *
 * <p>Phase 1 is detect-and-log only. These probes are intentionally operator-local and modelled on
 * {@code slony}'s {@code io.stackgres.slony.cloud} probes (same endpoints and timeout shape) so they
 * can later be lifted into {@code matriarch-model} and shared — the still-open "module reuse"
 * decision, not made here.
 */
interface CloudClusterNameProbe {

  /**
   * Short tag for logs and (later) the detection source, e.g. {@code gke}, {@code eks}, {@code aks}.
   */
  String source();

  /** Best-effort cluster name, or empty when this is not that cloud or the name is unavailable. */
  Optional<String> detect(HttpClient client);

}

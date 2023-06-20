/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import org.jooq.lambda.tuple.Tuple3;

public class PodStats
    extends Tuple3<Pod, ImmutableMap<PatroniStatsScripts, String>,
      Optional<PersistentVolumeClaim>> {

  private static final long serialVersionUID = 1L;

  private PodStats(Pod pod, ImmutableMap<PatroniStatsScripts, String> stats,
      Optional<PersistentVolumeClaim> pvc) {
    super(pod, stats, pvc);
  }

  public static PodStats fromTuple(
      Tuple3<Pod, ImmutableMap<PatroniStatsScripts, String>, Optional<PersistentVolumeClaim>> t) {
    return new PodStats(t.v1, t.v2, t.v3);
  }

}

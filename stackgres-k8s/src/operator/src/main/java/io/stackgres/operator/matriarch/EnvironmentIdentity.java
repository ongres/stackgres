/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.util.Optional;

import io.stackgres.operator.app.OperatorInstallationInfoHolder;
import io.stackgres.operator.matriarch.clustername.ClusterNameDetector;
import io.stackgres.proto.api.v1.Environment;
import io.stackgres.proto.types.v1.ApiSurface;
import io.stackgres.proto.types.v1.Id;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * The identity of THIS StackGres install as one api.v1 {@link Environment} — shared by the local api.v1
 * server ({@link StackGresApiService}), the write handlers and the cloud uplink
 * ({@link CloudUplinkClient}) so the install presents itself consistently (the operator-side analog of
 * the standalone matriarch's {@code EnvironmentIdentity}).
 *
 * <ul>
 *   <li>{@link #id()} — the stable installation id (persisted in {@code SGConfig.status}). The cloud keys
 *       its cache by it; users never address an environment by it.</li>
 *   <li>{@link #name()} — the environment's user-facing identifier: the detected Kubernetes cluster name
 *       ({@link ClusterNameDetector}), falling back to the installation id. Toward the cloud this is only
 *       a <em>proposal</em>: the cloud owns the per-tenant name registry, de-duplicates it
 *       ({@code name}, {@code name-1}, …) and lets the user rename it, so the name the cloud serves can
 *       differ from this one. This local API serves (and accepts) its own name.</li>
 * </ul>
 */
@ApplicationScoped
public class EnvironmentIdentity {

  private static final Logger LOG = Logger.getLogger(EnvironmentIdentity.class);

  private static final String NS_UID_SOURCE = "ns-uid";
  private static final int SHORT_UID_LENGTH = 8;

  @Inject
  OperatorInstallationInfoHolder installationInfoHolder;

  @Inject
  ClusterNameDetector clusterNameDetector;

  /** The stable installation id (falls back to "local"). */
  public String id() {
    String id = installationInfoHolder.getInstallationId();
    return id == null || id.isBlank() ? "local" : id;
  }

  /**
   * The environment's name: the detected Kubernetes cluster name, or the installation id when nothing
   * was detected. The detector's last-resort result — the bare {@code kube-system} namespace UID — is
   * unique but unreadable, so it is shortened to {@code k8s-<first 8 chars>}; should two clusters ever
   * share that prefix, the cloud de-duplicates the name like any other collision.
   */
  public String name() {
    Optional<ClusterNameDetector.Detected> detected = detected();
    if (detected.isEmpty() || detected.get().name().isBlank()) {
      return id();
    }
    String name = detected.get().name().strip();
    if (NS_UID_SOURCE.equals(detected.get().source())) {
      return "k8s-" + name.substring(0, Math.min(SHORT_UID_LENGTH, name.length()));
    }
    return name;
  }

  /**
   * True if {@code selector} addresses this environment: empty (the only environment here), its name,
   * or its installation id.
   */
  public boolean matches(String selector) {
    return selector == null || selector.isBlank() || selector.equals(name()) || selector.equals(id());
  }

  /** This install as a full api.v1 Environment (id, name, kind, advertised surfaces). */
  public Environment environment() {
    return Environment.newBuilder()
        .setId(Id.newBuilder().setValue(id()))
        .setName(name())
        .setKind(Environment.Kind.KIND_K8S_STACKGRES)
        // What this install actually serves: cluster create/delete/restart/credentials and the event
        // timeline. Catalog (stubbed) and log tail (not on k8s yet) are deliberately not advertised.
        .addSurface(ApiSurface.API_SURFACE_CLUSTER_LIFECYCLE)
        .addSurface(ApiSurface.API_SURFACE_EVENTS)
        .build();
  }

  private Optional<ClusterNameDetector.Detected> detected() {
    if (clusterNameDetector == null) {
      return Optional.empty();
    }
    try {
      return clusterNameDetector.detected();
    } catch (RuntimeException e) {
      LOG.debugf("cluster name detection failed: %s", e.getMessage());
      return Optional.empty();
    }
  }
}

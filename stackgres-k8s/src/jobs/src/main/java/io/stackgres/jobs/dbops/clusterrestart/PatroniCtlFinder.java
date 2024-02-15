/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniCtl.PatroniCtlInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PatroniCtlFinder {

  @Inject
  KubernetesClient client;

  @Inject
  PatroniCtl patroniCtl;

  public PatroniCtlInstance findPatroniCtl(
      String clusterName, String namespace) {
    var cluster = findCluster(clusterName, namespace);
    return patroniCtl.instanceFor(cluster);
  }

  StackGresCluster findCluster(String clusterName, String namespace) {
    return Optional.ofNullable(client.resources(StackGresCluster.class)
        .inNamespace(namespace)
        .withName(clusterName)
        .get())
        .orElseThrow(() -> new RuntimeException("Can not find SGCluster"));
  }

}

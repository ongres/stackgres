/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import io.stackgres.crd.sgcluster.StackGresCluster;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgReplicaSets {

  private static final Logger log = LoggerFactory.getLogger(Deployments.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create a new ReplicaSet.
   */
  public ReplicaSet create(StackGresCluster sgcluster) {

    log.debug("Creating cluster name: {}", sgcluster.getMetadata().getName());

    Map<String, String> labels = new HashMap<>();
    labels.put("app", "stackgres");
    labels.put("cluster", sgcluster.getMetadata().getName());

    Map<String, Quantity> limits = new HashMap<>(2);
    if (!"".equals(sgcluster.getSpec().getCpu())) {
      limits.put("cpu", new Quantity(sgcluster.getSpec().getCpu()));
    }
    if (!"".equals(sgcluster.getSpec().getMemory())) {
      limits.put("memory", new Quantity(sgcluster.getSpec().getMemory()));
    }

    String gen = Long.toHexString(Double.doubleToLongBits(Math.random()));
    gen = gen.substring(2, 7);

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      ReplicaSet rs = new ReplicaSetBuilder()
          .withKind("ReplicaSet")
          .withNewMetadata()
          .withName(sgcluster.getMetadata().getName() + "-" + gen)
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withReplicas(1)
          .withSelector(new LabelSelectorBuilder()
              .addToMatchLabels(labels)
              .build())
          .withTemplate(new PodTemplateSpecBuilder()
              .withMetadata(new ObjectMetaBuilder()
                  .addToLabels(labels)
                  .build())
              .withNewSpec()
              .addNewContainer()
              .withName("sg-postgres")
              .withImage("postgres:11")
              .withResources(new ResourceRequirementsBuilder().addToLimits(limits).build())
              .withPorts(new ContainerPortBuilder().withContainerPort(5432).build())
              .endContainer()
              .endSpec()
              .build())
          .endSpec()
          .build();

      log.debug("Creating or replacing deployment: {}", sgcluster.getMetadata().getName());

      client.apps().replicaSets().inNamespace(namespace).createOrReplace(rs);
      return rs;
    }
  }

}

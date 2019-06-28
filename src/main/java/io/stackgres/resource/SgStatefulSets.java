/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import io.stackgres.crd.sgcluster.StackGresCluster;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("initialization.fields.uninitialized")
@ApplicationScoped
public class SgStatefulSets {

  private static final Logger log = LoggerFactory.getLogger(SgStatefulSets.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  public StatefulSet create(@NonNull StackGresCluster sgcluster) {

    log.debug("Creating cluster name: {}", sgcluster.getMetadata().getName());

    Map<String, String> labels = new HashMap<>();
    labels.put("app", "StackGres");
    labels.put("cluster-name", sgcluster.getMetadata().getName());

    Map<String, Quantity> limits = new HashMap<>();
    if (!"".equals(sgcluster.getSpec().getCpu())) {
      limits.put("cpu", new Quantity(sgcluster.getSpec().getCpu()));
    }
    if (!"".equals(sgcluster.getSpec().getMemory())) {
      limits.put("memory", new Quantity(sgcluster.getSpec().getMemory()));
    }

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      StatefulSet rs = new StatefulSetBuilder()
          .withNewMetadata()
          .withName(sgcluster.getMetadata().getName())
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withReplicas(sgcluster.getSpec().getInstances())
          .withSelector(new LabelSelectorBuilder()
              .addToMatchLabels(labels)
              .build())
          .withServiceName(sgcluster.getMetadata().getName())
          .withTemplate(new PodTemplateSpecBuilder()
              .withMetadata(new ObjectMetaBuilder()
                  .addToLabels(labels)
                  .build())
              .withNewSpec()
              .withShareProcessNamespace(true)
              .withServiceAccountName(sgcluster.getMetadata().getName())
              .addNewContainer()
              .withName(sgcluster.getMetadata().getName())
              .withImage("registry.gitlab.com/ongresinc/artifacts-builder/patroni-postgresql")
              .withResources(new ResourceRequirementsBuilder().addToLimits(limits).build())
              .withImagePullPolicy("Always")
              .withPorts(
                  new ContainerPortBuilder()
                      .withContainerPort(5432).build(),
                  new ContainerPortBuilder()
                      .withContainerPort(8008).build())
              .withVolumeMounts(new VolumeMountBuilder()
                  .withName("config-volume")
                  .withMountPath("/etc/stackgres")
                  .build())
              .withEnvFrom(new EnvFromSourceBuilder()
                  .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                      .withName(sgcluster.getMetadata().getName()).build())
                  .build())
              .withEnv(
                  new EnvVarBuilder().withName("PATRONI_NAME")
                      .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                          new ObjectFieldSelectorBuilder().withFieldPath("metadata.name").build())
                          .build())
                      .build(),
                  new EnvVarBuilder().withName("PATRONI_KUBERNETES_POD_IP")
                      .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                          new ObjectFieldSelectorBuilder().withFieldPath("status.podIP").build())
                          .build())
                      .build(),
                  new EnvVarBuilder().withName("PATRONI_SUPERUSER_PASSWORD")
                      .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                          new SecretKeySelectorBuilder()
                              .withName(sgcluster.getMetadata().getName())
                              .withKey("superuser-password")
                              .build())
                          .build())
                      .build(),
                  new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
                      .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                          new SecretKeySelectorBuilder()
                              .withName(sgcluster.getMetadata().getName())
                              .withKey("replication-password")
                              .build())
                          .build())
                      .build())
              .endContainer()
              .withVolumes(new VolumeBuilder()
                  .withName("config-volume")
                  .withConfigMap(new ConfigMapVolumeSourceBuilder()
                      .withName(sgcluster.getMetadata().getName())
                      .build())
                  .build())
              .withImagePullSecrets(new LocalObjectReferenceBuilder()
                  .withName("registry-secret").build())
              .endSpec()
              .build())
          .endSpec()
          .build();

      log.debug("Creating or replacing: {}", sgcluster.getMetadata().getName());

      client.apps().statefulSets().inNamespace(namespace).createOrReplace(rs);
      return rs;
    }
  }

}

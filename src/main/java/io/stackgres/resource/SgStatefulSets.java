/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgStatefulSets {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgStatefulSets.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  public StatefulSet create(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    Map<String, String> labels = ResourceUtils.defaultLabels(name);

    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withReplicas(resource.getSpec().getInstances())
        .withSelector(new LabelSelectorBuilder()
            .addToMatchLabels(labels)
            .build())
        .withServiceName(name)
        .withTemplate(new PodTemplateSpecBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(labels)
                .build())
            .withNewSpec()
            .withShareProcessNamespace(true)
            .withServiceAccountName(name + SgPatroniRole.SUFIX)
            .addNewContainer()
            .withName(name)
            .withImage("docker.io/ongres/patroni:11.4")
            .withImagePullPolicy("Always")
            .withPorts(
                new ContainerPortBuilder().withContainerPort(5432).build(),
                new ContainerPortBuilder().withContainerPort(8008).build())
            .withVolumeMounts(pgSocket)
            .withEnvFrom(new EnvFromSourceBuilder()
                .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                    .withName(name).build())
                .build())
            .withEnv(
                new EnvVarBuilder().withName("PATRONI_NAME")
                    .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.name").build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_KUBERNETES_NAMESPACE")
                    .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.namespace")
                            .build())
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
                            .withName(name)
                            .withKey("superuser-password")
                            .build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
                    .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                        new SecretKeySelectorBuilder()
                            .withName(name)
                            .withKey("replication-password")
                            .build())
                        .build())
                    .build())
            .endContainer()
            .addNewContainer()
            .withName("postgres-util")
            .withImage("docker.io/ongres/postgres-util:11.4")
            .withImagePullPolicy("Always")
            .withNewSecurityContext()
            .withNewCapabilities()
            .addNewAdd("SYS_PTRACE")
            .endCapabilities()
            .endSecurityContext()
            .addNewEnv()
            .withName("PG_VERSION")
            .withValue("11")
            .endEnv()
            .withStdin(true)
            .withTty(true)
            .withCommand("/bin/sh")
            .withArgs("-c", "while true; do sleep 10; done")
            .withVolumeMounts(pgSocket)
            .endContainer()
            .withVolumes(new VolumeBuilder()
                .withName("pg-socket")
                .withNewEmptyDir()
                .withMedium("Memory")
                .endEmptyDir()
                .build())
            .withTerminationGracePeriodSeconds(0L)
            .endSpec()
            .build())
        .endSpec()
        .build();

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      client.apps().statefulSets().inNamespace(namespace).createOrReplace(statefulSet);

      LOGGER.debug("Creating StatefulSet: {}", name);
      LOGGER.trace("StatefulSet: {}", statefulSet);
      return statefulSet;
    }
  }

  /**
   * Update the specification of the cluster.
   */
  public StatefulSet update(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      StatefulSet statefulSet =
          client.apps().statefulSets().inNamespace(namespace).withName(name).get();
      if (statefulSet != null) {
        int instances = resource.getSpec().getInstances();

        StatefulSetSpec spec = statefulSet.getSpec();
        if (spec.getReplicas() != instances) {
          spec.setReplicas(instances);
        }
        statefulSet.setSpec(spec);

        client.apps().statefulSets().inNamespace(namespace).createOrReplace(statefulSet);
      }

      LOGGER.debug("Updating StatefulSet: {}", name);
      return statefulSet;
    }
  }

  /**
   * Delete resource.
   */
  public StatefulSet delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      return delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public StatefulSet delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    StatefulSet statefulSet = client.apps().statefulSets().inNamespace(namespace)
        .withName(name).get();
    if (statefulSet != null) {
      Boolean deleted = client.apps().statefulSets().inNamespace(namespace).withName(name)
          .cascading(true).delete();
      LOGGER.debug("Deleting StatefulSet: {}, success: {}", name, deleted);
    }

    return statefulSet;
  }

}

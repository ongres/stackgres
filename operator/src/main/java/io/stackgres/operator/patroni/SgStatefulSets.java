/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ResourceUtils;
import io.stackgres.common.sgcluster.StackGresCluster;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.configuration.ImmutableStorageConfig;
import io.stackgres.operator.configuration.PatroniConfig;
import io.stackgres.operator.configuration.StorageConfig;
import io.stackgres.operator.customresources.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresources.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresources.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.operator.customresources.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.operator.customresources.sgprofile.StackGresProfile;
import io.stackgres.operator.customresources.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.customresources.sgprofile.StackGresProfileDoneable;
import io.stackgres.operator.customresources.sgprofile.StackGresProfileList;
import io.stackgres.operator.patroni.parameters.Blacklist;
import io.stackgres.sidecars.Sidecar;
import io.stackgres.sidecars.pgbouncer.PgBouncer;
import io.stackgres.sidecars.pgutils.PostgresUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgStatefulSets {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgStatefulSets.class);

  private static final String NAME = "patroni";
  private static final String IMAGE_PREFIX = "docker.io/ongres/patroni:";

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  public StatefulSet create(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();
    final String pgVersion = resource.getSpec().getPostgresVersion();
    // final Integer pg_version = resource.getSpec().getPostgresVersion();
    final Optional<StackGresProfile> profile = getProfile(resource);

    ResourceRequirements resources = new ResourceRequirements();
    StorageConfig storage = ImmutableStorageConfig.builder().size("").build();
    if (profile.isPresent()) {
      resources.setRequests(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
      resources.setLimits(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
      storage = ImmutableStorageConfig.builder()
          .size(profile.get().getSpec().getVolumeSize())
          .storageClass(profile.get().getSpec().getVolumeStorageClass())
          .build();
    }

    PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(storage.getResourceRequirements())
        .withStorageClassName(storage.getStorageClass());

    Map<String, String> labels = ResourceUtils.defaultLabels(name);

    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    VolumeMount pgData = new VolumeMountBuilder()
        .withName("pg-data")
        .withMountPath("/var/lib/postgresql")
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
            .withShareProcessNamespace(Boolean.TRUE)
            .withServiceAccountName(name + SgPatroniRole.SUFFIX)
            .addNewContainer()
            .withName(NAME)
            .withImage(IMAGE_PREFIX + pgVersion)
            .withImagePullPolicy("Always")
            .withSecurityContext(new SecurityContextBuilder()
                .withRunAsUser(999L)
                .withAllowPrivilegeEscalation(Boolean.FALSE)
                .build())
            .withPorts(
                new ContainerPortBuilder().withContainerPort(5432).build(),
                new ContainerPortBuilder().withContainerPort(8008).build())
            .withVolumeMounts(pgSocket, pgData)
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
            .withLivenessProbe(new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder()
                    .withPath("/health")
                    .withPort(new IntOrString(8008))
                    .withScheme("HTTP")
                    .build())
                .withInitialDelaySeconds(600)
                .withPeriodSeconds(60)
                .withFailureThreshold(5)
                .build())
            .withReadinessProbe(new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder()
                    .withPath("/health")
                    .withPort(new IntOrString(8008))
                    .withScheme("HTTP")
                    .build())
                .withInitialDelaySeconds(5)
                .withPeriodSeconds(10)
                .build())
            .withResources(resources)
            .endContainer()
            .withVolumes(new VolumeBuilder()
                .withName("pg-socket")
                .withNewEmptyDir()
                .withMedium("Memory")
                .endEmptyDir()
                .build())
            .withTerminationGracePeriodSeconds(60L)
            .withInitContainers(new ContainerBuilder()
                .withName("data-permissions")
                .withImage("busybox")
                .withCommand("/bin/sh")
                .withArgs("-c", "chmod 755 /var/lib/postgresql "
                    + "&& chown 999:999 /var/lib/postgresql")
                .withVolumeMounts(pgData)
                .build())
            .endSpec()
            .build())
        .withVolumeClaimTemplates(new PersistentVolumeClaimBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName("pg-data")
                .withLabels(labels)
                .build())
            .withSpec(volumeClaimSpec.build())
            .build())
        .endSpec()
        .build();

    try (KubernetesClient client = kubClientFactory.create()) {
      if (resource.getSpec().getSidecars().contains("postgres-utils")) {
        PostgresUtil pgutils = new PostgresUtil();
        injectContainer(resource, statefulSet, pgutils);
        List<HasMetadata> listResources = pgutils.createDependencies(resource);
        applyDependencies(client, listResources, namespace);
      }
      if (resource.getSpec().getSidecars().contains("pgbouncer")) {
        PgBouncer pgbouncer = new PgBouncer(name, kubClientFactory::create);
        injectContainer(resource, statefulSet, pgbouncer);
        List<HasMetadata> listResources = pgbouncer.createDependencies(resource);
        applyDependencies(client, listResources, namespace);
        injertVolumeConfigMap(statefulSet, pgbouncer, listResources);
      }

      StatefulSet ss = client.apps().statefulSets().inNamespace(namespace).withName(name).get();
      if (ss == null) {
        statefulSet = client.apps().statefulSets().inNamespace(namespace).create(statefulSet);
        LOGGER.debug("Creating StatefulSet: {}", name);
      }
      ResourceUtils.logAsYaml(statefulSet);
    }

    Optional<StackGresPostgresConfig> pgConfig = getPostgresConfig(resource);
    LOGGER.debug("StackGresPostgresConfig: {}", pgConfig);
    pgConfig.ifPresent(c -> applyPostgresConf(resource, c));

    LOGGER.trace("StatefulSet: {}", statefulSet);
    return statefulSet;
  }

  private void applyPostgresConf(StackGresCluster resource, StackGresPostgresConfig config) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();
    try (KubernetesClient client = kubClientFactory.create()) {
      Endpoints endpoint = null;
      String conf = null;
      PatroniConfig patroniConf = new PatroniConfig();
      ObjectMapper mapper = new ObjectMapper();
      while (endpoint == null || conf == null) {
        endpoint = client.endpoints().inNamespace(namespace)
            .withName(name + SgServices.CONFIG_SERVICE).get();
        LOGGER.debug("Get config endpoint: {}", endpoint);
        if (endpoint != null) {
          conf = endpoint.getMetadata().getAnnotations().get("config");
          if (conf != null) {
            try {
              patroniConf = mapper.readValue(conf, PatroniConfig.class);
            } catch (IOException e) {
              LOGGER.error("IOException", e);
            }
          }
        }
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
          LOGGER.error("InterruptedException", e);
          Thread.currentThread().interrupt();
        }
      }

      LOGGER.debug("Configuration DCS: {}", conf);
      try {
        Map<String, String> newParams = config.getSpec().getPostgresqlConf();
        // Blacklist removal
        for (String bl : Blacklist.getBlacklistParameters()) {
          newParams.remove(bl);
        }

        PatroniConfig.PostgreSql postgres = patroniConf.getPostgresql();
        if (postgres == null) {
          postgres = new PatroniConfig.PostgreSql();
        }

        Map<String, String> params = postgres.getParameters();
        if (params == null) {
          params = new HashMap<>();
        }
        params.putAll(newParams);
        postgres.setParameters(params);

        endpoint.getMetadata().getAnnotations().put("config",
            mapper.writeValueAsString(patroniConf));
        endpoint = client.endpoints().inNamespace(namespace).createOrReplace(endpoint);

        LOGGER.debug("Modification EP {}", endpoint);

      } catch (IOException e) {
        LOGGER.error("IOException from Jackson on writing JSON", e);
      }
    }
  }

  private Optional<StackGresPostgresConfig> getPostgresConfig(StackGresCluster resource) {
    final String namespace = resource.getMetadata().getNamespace();
    final String pgConfig = resource.getSpec().getPostgresConfig();
    LOGGER.debug("PostgresConfig Name: {}", pgConfig);
    if (pgConfig != null) {
      try (KubernetesClient client = kubClientFactory.create()) {
        Optional<CustomResourceDefinition> crd =
            ResourceUtils.getCustomResource(client, StackGresPostgresConfigDefinition.NAME);
        if (crd.isPresent()) {
          return Optional.ofNullable(client
              .customResources(crd.get(),
                  StackGresPostgresConfig.class,
                  StackGresPostgresConfigList.class,
                  StackGresPostgresConfigDoneable.class)
              .inNamespace(namespace)
              .withName(pgConfig)
              .get());
        }
      }
    }
    return Optional.empty();
  }

  private Optional<StackGresProfile> getProfile(StackGresCluster resource) {
    final String namespace = resource.getMetadata().getNamespace();
    final String profileName = resource.getSpec().getResourceProfile();
    LOGGER.debug("StackGres Profile Name: {}", profileName);
    if (profileName != null) {
      try (KubernetesClient client = kubClientFactory.create()) {
        Optional<CustomResourceDefinition> crd =
            ResourceUtils.getCustomResource(client, StackGresProfileDefinition.NAME);
        if (crd.isPresent()) {
          return Optional.ofNullable(client
              .customResources(crd.get(),
                  StackGresProfile.class,
                  StackGresProfileList.class,
                  StackGresProfileDoneable.class)
              .inNamespace(namespace)
              .withName(profileName)
              .get());
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Update the specification of the cluster.
   */
  public StatefulSet update(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    try (KubernetesClient client = kubClientFactory.create()) {
      StatefulSet statefulSet =
          client.apps().statefulSets().inNamespace(namespace).withName(name).get();
      if (statefulSet != null) {
        int instances = resource.getSpec().getInstances();

        StatefulSetSpec spec = statefulSet.getSpec();
        if (spec.getReplicas() != instances) {
          spec.setReplicas(instances);
        }

        List<String> sidecars = resource.getSpec().getSidecars();
        removeContainer(statefulSet, sidecars);
        for (String sidecar : sidecars) {
          if (sidecar.contains("postgres-utils")) {
            PostgresUtil pgutils = new PostgresUtil();
            injectContainer(resource, statefulSet, pgutils);
            List<HasMetadata> listResources = pgutils.createDependencies(resource);
            applyDependencies(client, listResources, namespace);
          } else if (sidecar.contains("pgbouncer")) {
            PgBouncer pgbouncer = new PgBouncer(name, kubClientFactory::create);
            injectContainer(resource, statefulSet, pgbouncer);
            List<HasMetadata> listResources = pgbouncer.createDependencies(resource);
            applyDependencies(client, listResources, namespace);
            injertVolumeConfigMap(statefulSet, pgbouncer, listResources);
          }
        }

        getPostgresConfig(resource).ifPresent(c -> applyPostgresConf(resource, c));

        statefulSet = client.apps().statefulSets().inNamespace(namespace)
            .createOrReplace(statefulSet);
      }
      ResourceUtils.logAsYaml(statefulSet);

      LOGGER.debug("Updating StatefulSet: {}", name);
      return statefulSet;
    }
  }

  /**
   * Delete resource.
   */
  public Boolean delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public Boolean delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    Boolean deleted = client.apps().statefulSets().inNamespace(namespace)
        .withLabels(ResourceUtils.defaultLabels(name)).delete();

    LOGGER.debug("Deleting StatefulSet: {}, success: {}", name, deleted);
    return deleted;
  }

  private void injectContainer(StackGresCluster resource, StatefulSet sts, Sidecar sidecar) {
    List<Container> listContainers = sts.getSpec().getTemplate().getSpec().getContainers();
    for (Container c : listContainers) {
      if (c.getName().equals(sidecar.getName())) {
        // Sidecar already included
        return;
      }
    }
    listContainers.add(sidecar.create(resource));
    sts.getSpec().getTemplate().getSpec().setContainers(listContainers);
  }

  private void injertVolumeConfigMap(StatefulSet sts, Sidecar sidecar,
      List<HasMetadata> listResources) {
    List<Volume> listVolume = sts.getSpec().getTemplate().getSpec().getVolumes();
    for (Volume v : listVolume) {
      if (v.getName().equals(sidecar.getName())) {
        return;
      }
    }

    for (HasMetadata res : listResources) {
      if (res instanceof ConfigMap) {
        Volume vm = new VolumeBuilder()
            .withName(sidecar.getName())
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(res.getMetadata().getName()).build())
            .build();
        listVolume.add(vm);
      }
    }
    sts.getSpec().getTemplate().getSpec().setVolumes(listVolume);
  }

  private void applyDependencies(KubernetesClient client, List<HasMetadata> listResources,
      final String namespace) {
    for (HasMetadata dep : listResources) {
      ResourceUtils.logAsYaml(dep);
      client.resource(dep).inNamespace(namespace).createOrReplace();
    }
  }

  private void removeContainer(StatefulSet sts, List<String> sidecar) {
    LOGGER.debug("List of sidecars: {}", sidecar);
    List<Container> listContainers = sts.getSpec().getTemplate().getSpec().getContainers();
    List<Container> containers = new ArrayList<>(listContainers);
    for (Container c : listContainers) {
      if (!sidecar.contains(c.getName()) && !c.getName().equals("patroni")) {
        LOGGER.debug("Removing sidecar: {}", c.getName());
        containers.remove(c);
      }
    }
    sts.getSpec().getTemplate().getSpec().setContainers(containers);
  }

}

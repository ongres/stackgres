/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.DefaultComparator;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DistributedLogsRequiredResourcesGeneratorTest {

  @Inject
  DistributedLogsRequiredResourcesGenerator generator;

  @InjectMock
  ConnectedClustersScannerImpl clusterScanner;

  @Inject
  @ReconciliationScope(value = StackGresDistributedLogs.class, kind = "StatefulSet")
  DistributedLogsStatefulSetComparator stsComparator;

  DefaultComparator configMapComparator = new DefaultComparator();

  List<StackGresCluster> connectedClusters;

  StackGresDistributedLogs distributedLogsCluster;

  String randomNamespace = StringUtils.getRandomNamespace();
  String randomName = StringUtils.getRandomClusterName();
  String clusterUid = UUID.randomUUID().toString();

  public static void sortVolumes(StatefulSet sts) {
    sts.getSpec().getTemplate().getSpec().getContainers()
        .forEach(container -> container.getVolumeMounts()
            .sort(Comparator.comparing(VolumeMount::getName)));

    sts.getSpec().getTemplate().getSpec().getVolumes()
        .sort(Comparator.comparing(Volume::getName));
  }

  private static String getResourceAsString(String classpathResource) {

    try (InputStream is = ClassLoader.getSystemResourceAsStream(classpathResource)) {
      if (is == null) {
        throw new IllegalArgumentException("Resource " + classpathResource + " not found");
      }
      try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        return CharStreams.toString(reader);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot read " + classpathResource);
    }
  }

  @BeforeEach
  void setUp() {
    randomNamespace = StringUtils.getRandomNamespace();
    randomName = StringUtils.getRandomClusterName();
    clusterUid = UUID.randomUUID().toString();
    connectedClusters = JsonUtil.readFromJson("stackgres_cluster/list.json",
        StackGresClusterList.class)
        .getItems();
    connectedClusters.forEach(c -> {
      c.getMetadata().setName(StringUtils.getRandomClusterName());
      c.getMetadata().setNamespace(randomNamespace);
    });

    lenient().when(clusterScanner.getConnectedClusters(any())).thenReturn(connectedClusters);

    distributedLogsCluster = JsonUtil.readFromJson("distributedlogs/default.json",
        StackGresDistributedLogs.class);

  }

  @Test
  void getRequiredResources_shouldNotFail() {

    generator.getRequiredResources(distributedLogsCluster);
  }

  private StackGresDistributedLogs get095Cluster() {
    StackGresDistributedLogs distributedLogs095 = JsonUtil
        .readFromJson("distributedlogs/0.9.json",
            StackGresDistributedLogs.class);
    distributedLogs095.getMetadata().setNamespace(randomNamespace);
    distributedLogs095.getMetadata().setName(randomName);
    distributedLogs095.getMetadata().setUid(clusterUid);
    return distributedLogs095;
  }

  @Test
  void givenADistributedLogsIn095_shouldGenerateAStsCompatibleWithThatVersion() {

    StackGresDistributedLogs distributedLogs095 = get095Cluster();

    StatefulSet deployedS = get095Sts(distributedLogs095);

    StatefulSet generatedSts = generator.getRequiredResources(distributedLogs095)
        .stream().filter(r -> r.getKind().equals("StatefulSet"))
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst().orElseThrow();

    /*
     * Sorting volumes and volume mounts since it has no effect on the stability of the containers,
     * but it could throw false failures if unsorted //
     */
    sortVolumes(deployedS);
    sortVolumes(generatedSts);

    assertTrue(stsComparator.isTheSameResource(generatedSts, deployedS));
    assertTrue(stsComparator.isResourceContentEqual(generatedSts, deployedS));
  }

  @Test
  void givenADistributedLogsIn095_shouldGenerateAPatroniConfigMapCompatibleWithThatVersion() {
    StackGresDistributedLogs distributedLogs = get095Cluster();

    ConfigMap expectedPatroniConfigMap = get095PatroniConfigMap(distributedLogs);

    ConfigMap generatedConfigMap = generator.getRequiredResources(distributedLogs)
        .stream().filter(r -> r.getKind().equals("ConfigMap"))
        .filter(
            r -> r.getMetadata().getName().equals(expectedPatroniConfigMap.getMetadata().getName()))
        .filter(ConfigMap.class::isInstance)
        .map(ConfigMap.class::cast)
        .findFirst().orElseThrow();

    assertTrue(configMapComparator.isTheSameResource(generatedConfigMap, expectedPatroniConfigMap));
    assertTrue(
        configMapComparator.isResourceContentEqual(generatedConfigMap, expectedPatroniConfigMap));

  }

  @Test
  void givenADistributedLogsIn095_shouldGenerateAInitTemplateConfigmapCompatibleWithThatVersion() {

    StackGresDistributedLogs distributedLogs = get095Cluster();
    ConfigMap expectedConfigMap = get095InitTemplateConfigMap(distributedLogs);

    ConfigMap generatedConfigMap = generator.getRequiredResources(distributedLogs)
        .stream().filter(r -> r.getKind().equals("ConfigMap"))
        .filter(r -> r.getMetadata().getName().equals(expectedConfigMap.getMetadata().getName()))
        .filter(ConfigMap.class::isInstance)
        .map(ConfigMap.class::cast)
        .findFirst().orElseThrow();

    assertTrue(configMapComparator.isTheSameResource(generatedConfigMap, expectedConfigMap));
    assertTrue(configMapComparator.isResourceContentEqual(generatedConfigMap, expectedConfigMap));

  }

  @Test
  void givenADistributedLogsIn095_shouldGenerateATemplatesConfigmapCompatibleWithThatVersion() {

    StackGresDistributedLogs distributedLogs = get095Cluster();
    ConfigMap expectedConfigMap = get095TemplatesConfigMap(distributedLogs);

    ConfigMap generatedConfigMap = generator.getRequiredResources(distributedLogs)
        .stream().filter(r -> r.getKind().equals("ConfigMap"))
        .filter(r -> r.getMetadata().getName().equals(expectedConfigMap.getMetadata().getName()))
        .filter(ConfigMap.class::isInstance)
        .map(ConfigMap.class::cast)
        .findFirst().orElseThrow();

    assertTrue(configMapComparator.isTheSameResource(generatedConfigMap, expectedConfigMap));
    assertTrue(configMapComparator.isResourceContentEqual(generatedConfigMap, expectedConfigMap));
  }

  @Test
  void givenADistributedLogsIn095_shouldGenerateAFluentdConfigmapCompatibleWithThatVersion() {
    StackGresDistributedLogs distributedLogs = get095Cluster();
    ConfigMap expectedConfigMap = get095FluentdConfigMap(distributedLogs);

    ConfigMap generatedConfigMap = generator.getRequiredResources(distributedLogs)
        .stream().filter(r -> r.getKind().equals("ConfigMap"))
        .filter(r -> r.getMetadata().getName().equals(expectedConfigMap.getMetadata().getName()))
        .filter(ConfigMap.class::isInstance)
        .map(ConfigMap.class::cast)
        .findFirst().orElseThrow();

    assertTrue(configMapComparator.isTheSameResource(generatedConfigMap, expectedConfigMap));
    assertTrue(configMapComparator.isResourceContentEqual(generatedConfigMap, expectedConfigMap));
  }

  private void fix095OwnerReference(HasMetadata resource,
      StackGresDistributedLogs distributedLogs095) {
    resource.getMetadata().getOwnerReferences().forEach(or -> {
      or.setUid(distributedLogs095.getMetadata().getUid());
      or.setName(distributedLogs095.getMetadata().getName());
      or.setKind(distributedLogs095.getKind());
    });
  }

  private void fix095Labels(HasMetadata r1, StackGresDistributedLogs distributedLogs) {
    fix095Labels(r1.getMetadata(), distributedLogs);
  }

  private void fix095Labels(ObjectMeta r1, StackGresDistributedLogs distributedLogs) {
    fix095Labels(r1.getLabels(), distributedLogs);
  }

  private void fix095Labels(Map<String, String> labels, StackGresDistributedLogs distributedLogs) {
    final String uid = distributedLogs.getMetadata().getUid();
    final String distributedLogsName = distributedLogs.getMetadata().getName();
    if (labels.containsKey("distributed-logs-uid")) {
      labels.put("distributed-logs-uid", uid);
    }
    if (labels.containsKey("distributed-logs-name")) {
      labels.put("distributed-logs-name", distributedLogsName);
    }
  }

  @NotNull
  private StatefulSet get095Sts(StackGresDistributedLogs distributedLogs095) {
    StatefulSet sts = JsonUtil
        .readFromJson("statefulset/0.9.5-distributedlogs.json",
            StatefulSet.class);
    final String namespace = distributedLogs095.getMetadata().getNamespace();
    sts.getMetadata().setNamespace(namespace);
    final String distributedLogsName = distributedLogs095.getMetadata().getName();
    sts.getMetadata().setName(distributedLogsName);
    final String uid = distributedLogs095.getMetadata().getUid();
    fix095Labels(sts, distributedLogs095);
    fix095Labels(sts.getSpec().getTemplate().getMetadata(), distributedLogs095);

    fix095Labels(sts.getSpec().getSelector().getMatchLabels(), distributedLogs095);
    Container patroniContainer = sts.getSpec().getTemplate().getSpec().getContainers().get(0);
    patroniContainer.getEnv().forEach(envVar -> {

      Optional.ofNullable(envVar.getValueFrom())
          .map(EnvVarSource::getSecretKeyRef)
          .filter(secretKeySelector -> secretKeySelector.getName().equals("distributedlogs"))
          .ifPresent(secretKeySelector -> secretKeySelector.setName(distributedLogsName));

      Optional.ofNullable(envVar.getValueFrom())
          .map(EnvVarSource::getConfigMapKeyRef)
          .filter(configMapKeySelector -> configMapKeySelector.getName().equals("distributedlogs"))
          .ifPresent(configMapKeySelector -> configMapKeySelector.setName(distributedLogsName));

    });

    patroniContainer.getEnvFrom().stream().map(EnvFromSource::getConfigMapRef)
        .forEach(configMapEnvSource -> configMapEnvSource.setName(distributedLogsName));

    final List<Container> containers = sts.getSpec().getTemplate().getSpec().getContainers();
    final List<Container> initContainers =
        sts.getSpec().getTemplate().getSpec().getInitContainers();
    var allContainers = Stream.concat(containers.stream(), initContainers.stream());
    allContainers.forEach(container -> container.getVolumeMounts().stream()
        .filter(volumeMount -> volumeMount.getName().startsWith("distributedlogs"))
        .forEach(volumeMount -> {
          String name = volumeMount.getName()
              .replaceAll("distributedlogs", distributedLogsName);

          volumeMount.setName(name);
        }));

    final List<OwnerReference> ownerReferences = List.of(
        new OwnerReferenceBuilder()
            .withApiVersion(CommonDefinition.GROUP + "/v1beta1")
            .withKind(distributedLogs095.getKind())
            .withName(distributedLogsName)
            .withUid(uid)
            .withController(true)
            .build());
    sts.getMetadata().setOwnerReferences(ownerReferences);
    sts.getSpec().setServiceName(distributedLogsName);
    sts.getSpec().getTemplate().getSpec().setServiceAccount(distributedLogsName + "-patroni");
    sts.getSpec().getTemplate().getSpec().setServiceAccountName(
        distributedLogsName + "-patroni");

    sts.getSpec().getTemplate().getSpec().getVolumes().stream()
        .filter(volume -> volume.getName().startsWith("distributedlogs"))
        .forEach(volume -> {
          String name = volume.getName()
              .replaceAll("distributedlogs", distributedLogsName);
          volume.setName(name);
        });

    sts.getSpec().getTemplate().getSpec().getVolumes().stream()
        .map(Volume::getConfigMap)
        .filter(Objects::nonNull)
        .forEach(configMap -> {
          String configMapName = configMap.getName()
              .replaceAll("distributedlogs", distributedLogsName);
          configMap.setName(configMapName);
        });

    sts.getSpec().getVolumeClaimTemplates().forEach(vct -> {
      fix095Labels(vct, distributedLogs095);
      vct.getMetadata().setName(distributedLogsName + "-data");
      vct.getMetadata().setNamespace(namespace);
      vct.getMetadata().setOwnerReferences(ownerReferences);
    });

    return sts;
  }

  @NotNull
  private ConfigMap get095PatroniConfigMap(StackGresDistributedLogs distributedLogs095) {

    ConfigMap configMap = sanitizeConfigMap("configmap/0.9.5-distributedlogs-patroni.json",
        distributedLogs095);

    Map<String, String> patroniLabels = ImmutableMap.of(
        "app", "StackGresDistributedLogs",
        "distributed-logs-uid", distributedLogs095.getMetadata().getUid(),
        "distributed-logs-name", distributedLogs095.getMetadata().getName(),
        "cluster", "true");

    JsonNode labelsJson = JsonUtil.toJson(patroniLabels);
    configMap.getData().put("PATRONI_KUBERNETES_LABELS", labelsJson.toString());
    configMap.getData().remove("MD5SUM");

    var md5Data = StackGresUtil.addMd5Sum(configMap.getData());
    configMap.setData(md5Data);

    return configMap;
  }

  private ConfigMap get095InitTemplateConfigMap(StackGresDistributedLogs distributedLogs905) {
    return sanitizeConfigMap("configmap/0.9.5-distributedlogs-inittemplate.json",
        distributedLogs905);
  }

  private ConfigMap get095TemplatesConfigMap(StackGresDistributedLogs distributedLogs095) {
    return sanitizeConfigMap("configmap/0.9.5-distributedlogs-templates.json", distributedLogs095);
  }

  private ConfigMap get095FluentdConfigMap(StackGresDistributedLogs distributedLogs095) {
    final ConfigMap configMap =
        sanitizeConfigMap("configmap/0.9.5-distributedlogs-fluentd.json", distributedLogs095);
    String[] oldDatabases = configMap.getData().get("databases").split("\n");
    String[] newDatabases = connectedClusters.stream().map(CustomResource::getMetadata)
        .map(m -> m.getNamespace() + "_" + m.getName())
        .collect(Collectors.toUnmodifiableList()).toArray(new String[0]);

    String[] oldWorkers = Arrays.stream(configMap.getData().get("databases").split("\n"))
        .map(d -> String.join(".", d.split("_")))
        .collect(Collectors.toUnmodifiableList()).toArray(new String[0]);

    String[] newWorkers = connectedClusters.stream().map(CustomResource::getMetadata)
        .map(m -> m.getNamespace() + "." + m.getName())
        .collect(Collectors.toUnmodifiableList()).toArray(new String[0]);

    configMap.getData().put("databases", String.join("\n", newDatabases));
    Seq.zip(Arrays.asList(oldDatabases), Arrays.asList(newDatabases))
        .forEach(tuple -> {
          String config = configMap.getData().get("fluentd.conf");
          config = config.replaceAll("\\b" + tuple.v1 + "\\b", tuple.v2);
          configMap.getData().put("fluentd.conf", config);
        });

    Seq.zip(Arrays.asList(oldWorkers), Arrays.asList(newWorkers))
        .forEach(tuple -> {
          String config = configMap.getData().get("fluentd.conf");
          config = config.replaceAll("\\b" + tuple.v1 + "\\b", tuple.v2);
          configMap.getData().put("fluentd.conf", config);
        });

    return configMap;
  }

  private ConfigMap sanitizeConfigMap(String resource, StackGresDistributedLogs distributedLogs) {

    ConfigMap configMap = JsonUtil.readFromJson(resource, ConfigMap.class);
    String clusterName = distributedLogs.getMetadata().getName();
    String oldClusterName = configMap.getMetadata().getOwnerReferences().get(0)
        .getName();

    String fileContent = getResourceAsString(resource);

    String sanitizedContent = fileContent.replaceAll(oldClusterName, clusterName);
    String namespace = distributedLogs.getMetadata().getNamespace();

    configMap = JsonUtil.toJson(sanitizedContent, ConfigMap.class);

    configMap.getMetadata().setNamespace(namespace);
    configMap.getMetadata().getOwnerReferences().forEach(or -> {
      or.setUid(distributedLogs.getMetadata().getUid());
    });
    fix095Labels(configMap, distributedLogs);
    fix095OwnerReference(configMap, distributedLogs);
    return configMap;
  }
}

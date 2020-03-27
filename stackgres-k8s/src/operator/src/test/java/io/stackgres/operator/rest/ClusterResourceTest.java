/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ConfigProperty;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.ClusterDtoFinder;
import io.stackgres.operator.resource.ClusterDtoScanner;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.operator.rest.transformer.AbstractResourceTransformer;
import io.stackgres.operator.rest.transformer.ClusterPodTransformer;
import io.stackgres.operator.rest.transformer.ClusterTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClusterResourceTest extends AbstractCustomResourceTest<ClusterDto, StackGresCluster> {

  @Mock
  private CustomResourceFinder<ClusterResourceConsumtionDto> statusFinder;

  @Mock
  private ConfigContext configContext;

  @Mock
  private KubernetesClientFactory clientFactory;

  @Mock
  private KubernetesClient client;

  @Mock
  private MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> podsOperation;

  @Mock
  private FilterWatchListMultiDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> anyNamespacePodsList;

  @Mock
  private NonNamespaceOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> podsList;

  private PodList podList;

  @BeforeEach
  void setUp() {
    super.setUp();
    podList = JsonUtil.readFromJson("stackgres_cluster/pods.json", PodList.class);
  }

  @Test
  @Override
  void listShouldReturnAllBackupConfigs() {
    when(configContext.getProperty(ConfigProperty.GRAFANA_EMBEDDED))
        .thenReturn(Optional.of("true"));
    when(clientFactory.create()).thenReturn(client);
    when(client.pods()).thenReturn(podsOperation);
    when(podsOperation.inAnyNamespace()).thenReturn(podsList);
    when(podsList.withLabels(any())).thenReturn(podsList);
    when(podsList.list()).thenReturn(podList);
    super.listShouldReturnAllBackupConfigs();
  }

  @Test
  @Override
  void getOfAnExistingBackupConfigShouldReturnTheExistingBackupConfig() {
    when(configContext.getProperty(ConfigProperty.GRAFANA_EMBEDDED))
        .thenReturn(Optional.of("true"));
    when(clientFactory.create()).thenReturn(client);
    when(client.pods()).thenReturn(podsOperation);
    when(podsOperation.inNamespace(anyString())).thenReturn(podsList);
    when(podsList.withLabels(any())).thenReturn(podsList);
    when(podsList.list()).thenReturn(podList);
    super.getOfAnExistingBackupConfigShouldReturnTheExistingBackupConfig();
  }

  @Override
  protected CustomResourceList<StackGresCluster> getCustomResourceList() {
    return JsonUtil.readFromJson("stackgres_cluster/list.json", StackGresClusterList.class);
  }

  @Override
  protected ClusterDto getResourceDto() {
    return JsonUtil.readFromJson("stackgres_cluster/dto.json", ClusterDto.class);
  }

  @Override
  protected ClusterTransformer getTransformer() {
    final ClusterTransformer clusterTransformer = new ClusterTransformer();
    clusterTransformer.setContext(configContext);
    clusterTransformer.setClusterPodTransformer(new ClusterPodTransformer());
    return clusterTransformer;
  }

  @Override
  protected AbstractRestService<ClusterDto, StackGresCluster> getService(
      CustomResourceScanner<StackGresCluster> scanner,
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      AbstractResourceTransformer<ClusterDto, StackGresCluster> transformer) {
    final ClusterDtoFinder dtoFinder = new ClusterDtoFinder();
    dtoFinder.setClusterFinder(finder);
    dtoFinder.setClientFactory(clientFactory);
    dtoFinder.setClusterTransformer(getTransformer());
    final ClusterDtoScanner dtoScanner = new ClusterDtoScanner();
    dtoScanner.setClusterScanner(scanner);
    dtoScanner.setClientFactory(clientFactory);
    dtoScanner.setClusterTransformer(getTransformer());
    return new ClusterResource(
        dtoScanner,
        dtoFinder,
        scheduler, transformer,
        statusFinder);
  }

  @Override
  protected String getResourceNamespace() {
    return "postgresql";
  }

  @Override
  protected String getResourceName() {
    return "stackgres";
  }

  @Override
  protected void checkBackupConfig(ClusterDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("postgresql", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("backupconf", resource.getSpec().getConfigurations().getBackupConfig());
    assertEquals("pgbouncerconf", resource.getSpec().getConfigurations().getConnectionPoolingConfig());
    assertEquals("5Gi", resource.getSpec().getVolumeSize());
    assertEquals("standard", resource.getSpec().getStorageClass());
    assertEquals(true, resource.getSpec().getPrometheusAutobind());
    assertEquals(1, resource.getSpec().getInstances());
    assertEquals("11.5", resource.getSpec().getPostgresVersion());
    assertEquals("postgresconf", resource.getSpec().getConfigurations().getPostgresConfig());
    assertEquals("size-xs", resource.getSpec().getResourceProfile());
    assertNotNull(resource.getSpec().getRestore());
    assertEquals("d7e660a9-377c-11ea-b04b-0242ac110004", resource.getSpec().getRestore().getBackupUid());
    assertIterableEquals(ImmutableList.of(
        "connection-pooling",
        "postgres-util",
        "prometheus-postgres-exporter"),
        resource.getSpec().getSidecars());
    assertEquals(1, resource.getPodsReady());
    assertNotNull(resource.getPods());
    assertEquals(2, resource.getPods().size());
    assertEquals(4, resource.getPods().get(0).getContainers());
    assertEquals(4, resource.getPods().get(0).getContainersReady());
    assertEquals("10.244.3.23", resource.getPods().get(0).getIp());
    assertEquals("stackgres-0", resource.getPods().get(0).getName());
    assertEquals("postgresql", resource.getPods().get(0).getNamespace());
    assertEquals("primary", resource.getPods().get(0).getRole());
    assertEquals("Active", resource.getPods().get(0).getStatus());
    assertEquals(4, resource.getPods().get(1).getContainers());
    assertEquals(0, resource.getPods().get(1).getContainersReady());
    assertNull(resource.getPods().get(1).getIp());
    assertEquals("stackgres-1", resource.getPods().get(1).getName());
    assertEquals("postgresql", resource.getPods().get(1).getNamespace());
    assertNull(resource.getPods().get(1).getRole());
    assertEquals("Pending", resource.getPods().get(1).getStatus());
  }

  @Override
  protected void checkBackupConfig(StackGresCluster resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("postgresql", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
   assertNotNull(resource.getSpec());
    assertEquals("backupconf", resource.getSpec().getConfigurations().getBackupConfig());
    assertEquals("pgbouncerconf", resource.getSpec().getConfigurations().getConnectionPoolingConfig());
    assertEquals("5Gi", resource.getSpec().getVolumeSize());
    assertEquals("standard", resource.getSpec().getStorageClass());
    assertEquals(true, resource.getSpec().getPrometheusAutobind());
    assertEquals(1, resource.getSpec().getInstances());
    assertEquals("11.5", resource.getSpec().getPostgresVersion());
    assertEquals("postgresconf", resource.getSpec().getConfigurations().getPostgresConfig());
    assertEquals("size-xs", resource.getSpec().getResourceProfile());
    assertNotNull(resource.getSpec().getRestore());
    assertEquals("d7e660a9-377c-11ea-b04b-0242ac110004", resource.getSpec().getRestore().getBackupUid());
    assertIterableEquals(ImmutableList.of(
        "connection-pooling",
        "postgres-util",
        "prometheus-postgres-exporter"),
        resource.getSpec().getSidecars());
  }

}
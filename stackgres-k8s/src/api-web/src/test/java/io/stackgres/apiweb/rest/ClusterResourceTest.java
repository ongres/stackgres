/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.BadRequestException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.apiweb.distributedlogs.DistributedLogsFetcher;
import io.stackgres.apiweb.distributedlogs.DistributedLogsQueryParameters;
import io.stackgres.apiweb.distributedlogs.FullTextSearchQuery;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterLogEntryDto;
import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.resource.ClusterDtoFinder;
import io.stackgres.apiweb.resource.ClusterDtoScanner;
import io.stackgres.apiweb.resource.ClusterStatsDtoFinder;
import io.stackgres.apiweb.transformer.AbstractResourceTransformer;
import io.stackgres.apiweb.transformer.ClusterPodTransformer;
import io.stackgres.apiweb.transformer.ClusterStatsTransformer;
import io.stackgres.apiweb.transformer.ClusterTransformer;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.ClusterLabelMapper;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodExecutor;
import io.stackgres.common.resource.PodFinder;
import io.stackgres.testutil.JsonUtil;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClusterResourceTest
    extends AbstractCustomResourceTest<ClusterDto, StackGresCluster, ClusterResource> {

  @Mock
  private DistributedLogsFetcher distributedLogsFetcher;

  @Mock
  private StackGresPropertyContext<WebApiProperty> configContext;

  @Mock
  private PodFinder podFinder;

  @Mock
  private PodExecutor podExecutor;

  @Mock
  private PersistentVolumeClaimFinder persistentVolumeClaimFinder;

  @Mock
  ManagedExecutor managedExecutor;

  private ExecutorService executorService;

  private PodList podList;
  private List<ClusterLogEntryDto> logList;
  private StackGresCluster clusterWithoutDistributedLogs;

  @BeforeEach
  void setUp() {
    super.setUp();
    podList = JsonUtil.readFromJson("stackgres_cluster/pods.json", PodList.class);
    logList = new ArrayList<>();
    clusterWithoutDistributedLogs = JsonUtil.readFromJson(
        "stackgres_cluster/without_distributed_logs.json", StackGresCluster.class);
    executorService = Executors.newWorkStealingPool();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        executorService.execute(Runnable.class.cast(invocation.getArgument(0)));
        return null;
      }
    }).when(managedExecutor).execute(any());
  }

  @AfterEach
  public void tearDown() throws Exception {
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.SECONDS);
  }

  @Test
  @Override
  void listShouldReturnAllDtos() {
    when(configContext.get(WebApiProperty.GRAFANA_EMBEDDED))
        .thenReturn(Optional.of("true"));
    when(podFinder.findResourcesWithLabels(any())).thenReturn(podList.getItems());
    super.listShouldReturnAllDtos();
  }

  @Test
  @Override
  void getOfAnExistingDtoShouldReturnTheExistingDto() {
    clusterMocks();
    super.getOfAnExistingDtoShouldReturnTheExistingDto();
  }

  @Test
  void getOfAnExistingDtoStatsShouldReturnTheExistingDtoStats() {
    clusterMocks();
    mockPodExecutor();

    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));

    ClusterStatsDto dto = service.stats(getResourceNamespace(), getResourceName());

    checkStatsDto(dto);
  }

  private void clusterMocks() {
    when(configContext.get(WebApiProperty.GRAFANA_EMBEDDED))
        .thenReturn(Optional.of("true"));
    when(podFinder.findResourcesInNamespaceWithLabels(anyString(), any()))
        .thenReturn(podList.getItems());
    when(persistentVolumeClaimFinder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(new PersistentVolumeClaimBuilder()
            .withNewSpec()
            .withNewResources()
            .withRequests(ImmutableMap.of("storage", Quantity.parse("5Gi")))
            .endResources()
            .endSpec()
            .build()));
  }

  private void mockPodExecutor() {
    when(podExecutor.exec(any(), anyString(), any())).thenReturn(ImmutableList.of(
        "cpuFound:4",
        "cpuQuota:50000",
        "cpuPeriod:100000",
        "cpuPsiAvg10:0.50",
        "cpuPsiAvg60:cat: /sys/fs/cgroup/cpu.pressure: No such file or directory",
        "cpuPsiAvg300:1.50",
        "cpuPsiTotal:10000000000",
        "memoryFound:" + (512L * 1024 * 1024),
        "memoryUsed:" + (278L * 1024 * 1024),
        "memoryPsiAvg10:0.50",
        "memoryPsiAvg60:1.00",
        "memoryPsiAvg300:1.50",
        "memoryPsiTotal:10000000000",
        "memoryPsiFullAvg10:0.50",
        "memoryPsiFullAvg60:1.00",
        "memoryPsiFullAvg300:1.50",
        "memoryPsiFullTotal:10000000000",
        "diskFound:" + (5L * 1024 * 1024 * 1024),
        "diskUsed:" + (1L * 1024 * 1024 * 1024 + 410L * 1024 * 1024),
        "diskPsiAvg10:0.50",
        "diskPsiAvg60:1.00",
        "diskPsiAvg300:1.50",
        "diskPsiTotal:10000000000",
        "diskPsiFullAvg10:0.50",
        "diskPsiFullAvg60:1.00",
        "diskPsiFullAvg300:1.50",
        "diskPsiFullTotal:10000000000",
        "load1m:0.5",
        "load5m:1.0",
        "load10m:1.5"));
  }

  @Override
  protected CustomResourceList<StackGresCluster> getCustomResourceList() {
    return JsonUtil.readFromJson("stackgres_cluster/list.json", StackGresClusterList.class);
  }

  @Override
  protected ClusterDto getDto() {
    return JsonUtil.readFromJson("stackgres_cluster/dto.json", ClusterDto.class);
  }

  @Override
  protected ClusterTransformer getTransformer() {
    final ClusterTransformer clusterTransformer = new ClusterTransformer(
        configContext, new ClusterPodTransformer());
    return clusterTransformer;
  }

  @Override
  protected ClusterResource getService(
      CustomResourceScanner<StackGresCluster> scanner,
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      AbstractResourceTransformer<ClusterDto, StackGresCluster> transformer) {
    ClusterTransformer clusterTransformer = getTransformer();
    final ClusterLabelFactory labelFactory = new ClusterLabelFactory(new ClusterLabelMapper());
    final ClusterDtoFinder dtoFinder = new ClusterDtoFinder(
        finder, podFinder, clusterTransformer, labelFactory);
    final ClusterDtoScanner dtoScanner = new ClusterDtoScanner(
        scanner, podFinder, clusterTransformer, labelFactory);
    final ClusterStatsTransformer clusterStatsTransformer = new ClusterStatsTransformer(
        new ClusterPodTransformer());
    final ClusterStatsDtoFinder statsDtoFinder = new ClusterStatsDtoFinder(
        finder, podFinder, podExecutor, persistentVolumeClaimFinder,
        labelFactory, clusterStatsTransformer,
        managedExecutor);

    return new ClusterResource(
        finder,
        scheduler, clusterTransformer,
        dtoScanner,
        dtoFinder,
        statsDtoFinder,
        distributedLogsFetcher);
  }

  @Override
  protected String getResourceNamespace() {
    return "stackgres";
  }

  @Override
  protected String getResourceName() {
    return "stackgres";
  }

  @Override
  protected void checkDto(ClusterDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("backupconf", resource.getSpec().getConfigurations().getSgBackupConfig());
    assertEquals("pgbouncerconf", resource.getSpec().getConfigurations().getSgPoolingConfig());
    assertEquals("5Gi", resource.getSpec().getPods().getPersistentVolume().getVolumeSize());
    assertEquals("standard", resource.getSpec().getPods().getPersistentVolume().getStorageClass());
    assertEquals(true, resource.getSpec().getPrometheusAutobind());
    assertEquals(1, resource.getSpec().getInstances());
    assertEquals("11.5", resource.getSpec().getPostgresVersion());
    assertEquals("postgresconf", resource.getSpec().getConfigurations().getSgPostgresConfig());
    assertEquals("size-xs", resource.getSpec().getSgInstanceProfile());
    assertNotNull(resource.getSpec().getInitData().getRestore());
    assertEquals("d7e660a9-377c-11ea-b04b-0242ac110004",
        resource.getSpec().getInitData().getRestore().getBackupUid());
    assertNotNull(resource.getSpec().getDistributedLogs());
    assertEquals("distributedlogs", resource.getSpec().getDistributedLogs().getDistributedLogs());
    assertFalse(resource.getSpec().getPods().getDisableConnectionPooling());
    assertFalse(resource.getSpec().getPods().getDisableMetricsExporter());
    assertFalse(resource.getSpec().getPods().getDisableMetricsExporter());
    assertEquals(1, resource.getPodsReady());
    assertNotNull(resource.getPods());
    assertEquals(2, resource.getPods().size());
    assertEquals(4, resource.getPods().get(0).getContainers());
    assertEquals(4, resource.getPods().get(0).getContainersReady());
    assertEquals("10.244.3.23", resource.getPods().get(0).getIp());
    assertEquals("stackgres-0", resource.getPods().get(0).getName());
    assertEquals("stackgres", resource.getPods().get(0).getNamespace());
    assertEquals("primary", resource.getPods().get(0).getRole());
    assertEquals("Active", resource.getPods().get(0).getStatus());
    assertEquals(4, resource.getPods().get(1).getContainers());
    assertEquals(0, resource.getPods().get(1).getContainersReady());
    assertNull(resource.getPods().get(1).getIp());
    assertEquals("stackgres-1", resource.getPods().get(1).getName());
    assertEquals("stackgres", resource.getPods().get(1).getNamespace());
    assertNull(resource.getPods().get(1).getRole());
    assertEquals("Pending", resource.getPods().get(1).getStatus());
    assertEquals("customLabelValue", resource.getSpec().getPods()
        .getMetadata().getLabels().get("customLabel"));
  }

  @Override
  protected void checkCustomResource(StackGresCluster resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("backupconf", resource.getSpec().getConfiguration().getBackupConfig());
    assertEquals("pgbouncerconf",
        resource.getSpec().getConfiguration().getConnectionPoolingConfig());
    assertEquals("5Gi", resource.getSpec().getPod().getPersistentVolume().getVolumeSize());
    assertEquals("standard", resource.getSpec().getPod().getPersistentVolume().getStorageClass());
    assertEquals(true, resource.getSpec().getPrometheusAutobind());
    assertEquals(1, resource.getSpec().getInstances());
    assertEquals("11.5", resource.getSpec().getPostgresVersion());
    assertEquals("postgresconf", resource.getSpec().getConfiguration().getPostgresConfig());
    assertEquals("size-xs", resource.getSpec().getResourceProfile());
    assertNotNull(resource.getSpec().getInitData().getRestore());
    assertEquals("d7e660a9-377c-11ea-b04b-0242ac110004",
        resource.getSpec().getInitData().getRestore().getBackupUid());
    assertNotNull(resource.getSpec().getDistributedLogs());
    assertEquals("distributedlogs", resource.getSpec().getDistributedLogs().getDistributedLogs());
    assertFalse(resource.getSpec().getPod().getDisableConnectionPooling());
    assertFalse(resource.getSpec().getPod().getDisableMetricsExporter());
    assertFalse(resource.getSpec().getPod().getDisableMetricsExporter());
    assertEquals("customLabelValue", resource.getSpec().getPod()
        .getMetadata().getLabels().get("customLabel"));
  }

  private void checkStatsDto(ClusterStatsDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertEquals("1000m", resource.getCpuRequested());
    assertEquals("1000m", resource.getCpuFound());
    assertEquals("0.50", resource.getCpuPsiAvg10());
    assertNull(resource.getCpuPsiAvg60());
    assertEquals("1.50", resource.getCpuPsiAvg300());
    assertEquals("20000000000", resource.getCpuPsiTotal());
    assertEquals("1.00Gi", resource.getMemoryRequested());
    assertEquals("1.00Gi", resource.getMemoryFound());
    assertEquals("556.00Mi", resource.getMemoryUsed());
    assertEquals("0.50", resource.getMemoryPsiAvg10());
    assertEquals("1.00", resource.getMemoryPsiAvg60());
    assertEquals("1.50", resource.getMemoryPsiAvg300());
    assertEquals("20000000000", resource.getMemoryPsiTotal());
    assertEquals("0.50", resource.getMemoryPsiFullAvg10());
    assertEquals("1.00", resource.getMemoryPsiFullAvg60());
    assertEquals("1.50", resource.getMemoryPsiFullAvg300());
    assertEquals("20000000000", resource.getMemoryPsiFullTotal());
    assertEquals("10.00Gi", resource.getDiskRequested());
    assertEquals("10.00Gi", resource.getDiskFound());
    assertEquals("2.80Gi", resource.getDiskUsed());
    assertEquals("0.50", resource.getDiskPsiAvg10());
    assertEquals("1.00", resource.getDiskPsiAvg60());
    assertEquals("1.50", resource.getDiskPsiAvg300());
    assertEquals("20000000000", resource.getDiskPsiTotal());
    assertEquals("0.50", resource.getDiskPsiFullAvg10());
    assertEquals("1.00", resource.getDiskPsiFullAvg60());
    assertEquals("1.50", resource.getDiskPsiFullAvg300());
    assertEquals("20000000000", resource.getDiskPsiFullTotal());
    assertEquals("0.50", resource.getAverageLoad1m());
    assertEquals("1.00", resource.getAverageLoad5m());
    assertEquals("1.50", resource.getAverageLoad10m());
    assertEquals(1, resource.getPodsReady());
    assertNotNull(resource.getPods());
    assertEquals(2, resource.getPods().size());
    assertEquals(4, resource.getPods().get(0).getContainers());
    assertEquals(4, resource.getPods().get(0).getContainersReady());
    assertEquals("10.244.3.23", resource.getPods().get(0).getIp());
    assertEquals("stackgres-0", resource.getPods().get(0).getName());
    assertEquals("stackgres", resource.getPods().get(0).getNamespace());
    assertEquals("primary", resource.getPods().get(0).getRole());
    assertEquals("Active", resource.getPods().get(0).getStatus());
    assertEquals("500m", resource.getPods().get(0).getCpuRequested());
    assertEquals("500m", resource.getPods().get(0).getCpuFound());
    assertEquals("0.50", resource.getPods().get(0).getCpuPsiAvg10());
    assertNull(resource.getPods().get(0).getCpuPsiAvg60());
    assertEquals("1.50", resource.getPods().get(0).getCpuPsiAvg300());
    assertEquals("10000000000", resource.getPods().get(0).getCpuPsiTotal());
    assertEquals("512.00Mi", resource.getPods().get(0).getMemoryRequested());
    assertEquals("512.00Mi", resource.getPods().get(0).getMemoryFound());
    assertEquals("278.00Mi", resource.getPods().get(0).getMemoryUsed());
    assertEquals("0.50", resource.getPods().get(0).getMemoryPsiAvg10());
    assertEquals("1.00", resource.getPods().get(0).getMemoryPsiAvg60());
    assertEquals("1.50", resource.getPods().get(0).getMemoryPsiAvg300());
    assertEquals("10000000000", resource.getPods().get(0).getMemoryPsiTotal());
    assertEquals("0.50", resource.getPods().get(0).getMemoryPsiFullAvg10());
    assertEquals("1.00", resource.getPods().get(0).getMemoryPsiFullAvg60());
    assertEquals("1.50", resource.getPods().get(0).getMemoryPsiFullAvg300());
    assertEquals("10000000000", resource.getPods().get(0).getMemoryPsiFullTotal());
    assertEquals("5.00Gi", resource.getPods().get(0).getDiskRequested());
    assertEquals("5.00Gi", resource.getPods().get(0).getDiskFound());
    assertEquals("1.40Gi", resource.getPods().get(0).getDiskUsed());
    assertEquals("0.50", resource.getPods().get(0).getDiskPsiAvg10());
    assertEquals("1.00", resource.getPods().get(0).getDiskPsiAvg60());
    assertEquals("1.50", resource.getPods().get(0).getDiskPsiAvg300());
    assertEquals("10000000000", resource.getPods().get(0).getDiskPsiTotal());
    assertEquals("0.50", resource.getPods().get(0).getDiskPsiFullAvg10());
    assertEquals("1.00", resource.getPods().get(0).getDiskPsiFullAvg60());
    assertEquals("1.50", resource.getPods().get(0).getDiskPsiFullAvg300());
    assertEquals("10000000000", resource.getPods().get(0).getDiskPsiFullTotal());
    assertEquals("0.50", resource.getPods().get(0).getAverageLoad1m());
    assertEquals("1.00", resource.getPods().get(0).getAverageLoad5m());
    assertEquals("1.50", resource.getPods().get(0).getAverageLoad10m());
    assertEquals(4, resource.getPods().get(1).getContainers());
    assertEquals(0, resource.getPods().get(1).getContainersReady());
    assertNull(resource.getPods().get(1).getIp());
    assertEquals("stackgres-1", resource.getPods().get(1).getName());
    assertEquals("stackgres", resource.getPods().get(1).getNamespace());
    assertNull(resource.getPods().get(1).getRole());
    assertEquals("Pending", resource.getPods().get(1).getStatus());
    assertEquals("500m", resource.getPods().get(1).getCpuRequested());
    assertEquals("500m", resource.getPods().get(1).getCpuFound());
    assertEquals("0.50", resource.getPods().get(1).getCpuPsiAvg10());
    assertNull(resource.getPods().get(1).getCpuPsiAvg60());
    assertEquals("1.50", resource.getPods().get(1).getCpuPsiAvg300());
    assertEquals("10000000000", resource.getPods().get(1).getCpuPsiTotal());
    assertEquals("512.00Mi", resource.getPods().get(1).getMemoryRequested());
    assertEquals("512.00Mi", resource.getPods().get(1).getMemoryFound());
    assertEquals("278.00Mi", resource.getPods().get(1).getMemoryUsed());
    assertEquals("0.50", resource.getPods().get(1).getMemoryPsiAvg10());
    assertEquals("1.00", resource.getPods().get(1).getMemoryPsiAvg60());
    assertEquals("1.50", resource.getPods().get(1).getMemoryPsiAvg300());
    assertEquals("10000000000", resource.getPods().get(1).getMemoryPsiTotal());
    assertEquals("0.50", resource.getPods().get(1).getMemoryPsiFullAvg10());
    assertEquals("1.00", resource.getPods().get(1).getMemoryPsiFullAvg60());
    assertEquals("1.50", resource.getPods().get(1).getMemoryPsiFullAvg300());
    assertEquals("10000000000", resource.getPods().get(1).getMemoryPsiFullTotal());
    assertEquals("5.00Gi", resource.getPods().get(1).getDiskRequested());
    assertEquals("5.00Gi", resource.getPods().get(1).getDiskFound());
    assertEquals("1.40Gi", resource.getPods().get(1).getDiskUsed());
    assertEquals("0.50", resource.getPods().get(1).getDiskPsiAvg10());
    assertEquals("1.00", resource.getPods().get(1).getDiskPsiAvg60());
    assertEquals("1.50", resource.getPods().get(1).getDiskPsiAvg300());
    assertEquals("10000000000", resource.getPods().get(1).getDiskPsiTotal());
    assertEquals("0.50", resource.getPods().get(1).getDiskPsiFullAvg10());
    assertEquals("1.00", resource.getPods().get(1).getDiskPsiFullAvg60());
    assertEquals("1.50", resource.getPods().get(1).getDiskPsiFullAvg300());
    assertEquals("10000000000", resource.getPods().get(1).getDiskPsiFullTotal());
    assertEquals("0.50", resource.getPods().get(1).getAverageLoad1m());
    assertEquals("1.00", resource.getPods().get(1).getAverageLoad5m());
    assertEquals("1.50", resource.getPods().get(1).getAverageLoad10m());
  }

  @Test
  void getLogsShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithRecordsShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 1);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = 1;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithNegativeRecordsShouldFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    when(distributedLogsFetcher.logs(any()))
        .thenReturn(logList);

    Integer records = -1;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive));
  }

  @Test
  void getLogsWithFromShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.of(Tuple.tuple(Instant.EPOCH, 0)));
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = Instant.EPOCH.toString();
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithFromAndIndexShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.of(Tuple.tuple(Instant.EPOCH, 1)));
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = Instant.EPOCH.toString() + "," + 1;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithWrongFromShouldFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    when(distributedLogsFetcher.logs(any()))
        .thenReturn(logList);

    Integer records = null;
    String from = Instant.EPOCH.toString().substring(5) + "," + 1;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive));
  }

  @Test
  void getLogsWithWrongFromIndexShouldFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    when(distributedLogsFetcher.logs(any()))
        .thenReturn(logList);

    Integer records = null;
    String from = Instant.EPOCH.toString() + "," + "a";
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive));
  }

  @Test
  void getLogsWithToShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(),
            Optional.of(Tuple.tuple(Instant.EPOCH, Integer.MAX_VALUE)));
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = Instant.EPOCH.toString();
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithToAndIndexShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.of(Tuple.tuple(Instant.EPOCH, 1)));
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = Instant.EPOCH.toString() + "," + 1;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithWrongToShouldFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    when(distributedLogsFetcher.logs(any()))
        .thenReturn(logList);

    Integer records = null;
    String from = null;
    String to = Instant.EPOCH.toString().substring(5) + "," + 1;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive));
  }

  @Test
  void getLogsWithWrongToIndexShouldFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    when(distributedLogsFetcher.logs(any()))
        .thenReturn(logList);

    Integer records = null;
    String from = null;
    String to = Instant.EPOCH.toString() + "," + "a";
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive));
  }

  @Test
  void getLogsWithClusterWithoutDistributedLogsShouldFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(clusterWithoutDistributedLogs));
    when(distributedLogsFetcher.logs(any()))
        .thenReturn(logList);

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive));
  }

  @Test
  void getLogsWithDescSortShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = "desc";
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithAscSortShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertTrue(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = "asc";
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithWrongSortShouldFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    when(distributedLogsFetcher.logs(any()))
        .thenReturn(logList);

    Integer records = null;
    String from = null;
    String to = null;
    String sort = "down";
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive));
  }

  @Test
  void getLogsWithTextShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(),
            Optional.of(new FullTextSearchQuery("test")));
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = "test";
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithEmptyTextShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(),
            Optional.of(new FullTextSearchQuery("")));
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = "";
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithLogTypeFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("logType", Optional.of("test")));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = "test";
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithPodNameFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("podName", Optional.of("test")));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = "test";
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithRoleFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("role", Optional.of("test")));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = "test";
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithErrorLevelFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("errorLevel", Optional.of("test")));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = "test";
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithUserNameFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("userName", Optional.of("test")));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = "test";
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithDatabaseNameFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("databaseName", Optional.of("test")));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = "test";
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithEmptyLogTypeFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("logType", Optional.empty()));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = "";
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithEmptyPodNameFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("podName", Optional.empty()));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = "";
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithEmptyRoleFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("role", Optional.empty()));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = "";
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithEmptyErrorLevelFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("errorLevel", Optional.empty()));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = "";
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithEmptyUserNameFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("userName", Optional.empty()));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = "";
    String databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithEmptyDatabaseNameFilterShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of("databaseName", Optional.empty()));
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = "";
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithFromInclusiveShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertTrue(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = true;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithFromExclusiveShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer(new Answer<List<ClusterLogEntryDto>>() {
      @Override
      public List<ClusterLogEntryDto> answer(InvocationOnMock invocation) throws Throwable {
        DistributedLogsQueryParameters parameters = invocation.getArgument(0);

        assertNotNull(parameters);
        checkDto(parameters.getCluster());
        assertEquals(parameters.getRecords(), 50);
        assertEquals(parameters.getFromTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getToTimeAndIndex(), Optional.empty());
        assertEquals(parameters.getFilters(), ImmutableMap.of());
        assertEquals(parameters.getFullTextSearchQuery(), Optional.empty());
        assertFalse(parameters.isSortAsc());
        assertFalse(parameters.isFromInclusive());

        return logList;
      }
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    String logType = null;
    String podName = null;
    String role = null;
    String errorLevel = null;
    String userName = null;
    String databaseName = null;
    Boolean fromInclusive = false;
    List<ClusterLogEntryDto> logs =
        service.logs(getResourceNamespace(), getResourceName(), records, from, to, sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

}

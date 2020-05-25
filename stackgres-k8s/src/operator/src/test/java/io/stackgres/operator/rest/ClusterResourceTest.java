/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;


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

import javax.ws.rs.BadRequestException;

import com.google.common.collect.ImmutableMap;
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
import io.stackgres.apiweb.ClusterResource;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.ClusterLabelMapper;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.ConfigContext;
import io.stackgres.common.OperatorProperty;
import io.stackgres.apiweb.resource.ClusterDtoFinder;
import io.stackgres.apiweb.resource.ClusterDtoScanner;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.apiweb.distributedlogs.DistributedLogsFetcher;
import io.stackgres.apiweb.distributedlogs.DistributedLogsQueryParameters;
import io.stackgres.apiweb.distributedlogs.FullTextSearchQuery;
import io.stackgres.apiweb.distributedlogs.dto.cluster.ClusterDto;
import io.stackgres.apiweb.distributedlogs.dto.cluster.ClusterLogEntryDto;
import io.stackgres.apiweb.distributedlogs.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.apiweb.transformer.AbstractResourceTransformer;
import io.stackgres.apiweb.transformer.ClusterPodTransformer;
import io.stackgres.apiweb.transformer.ClusterTransformer;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.tuple.Tuple;
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
  private CustomResourceFinder<ClusterResourceConsumtionDto> statusFinder;

  @Mock
  private DistributedLogsFetcher distributedLogsFetcher;

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
  private List<ClusterLogEntryDto> logList;
  private StackGresCluster clusterWithoutDistributedLogs;

  @BeforeEach
  void setUp() {
    super.setUp();
    podList = JsonUtil.readFromJson("stackgres_cluster/pods.json", PodList.class);
    logList = new ArrayList<>();
    clusterWithoutDistributedLogs = JsonUtil.readFromJson(
        "stackgres_cluster/without_distributed_logs.json", StackGresCluster.class);
  }

  @Test
  @Override
  void listShouldReturnAllDtos() {
    when(configContext.getProperty(OperatorProperty.GRAFANA_EMBEDDED))
        .thenReturn(Optional.of("true"));
    when(clientFactory.create()).thenReturn(client);
    when(client.pods()).thenReturn(podsOperation);
    when(podsOperation.inAnyNamespace()).thenReturn(podsList);
    when(podsList.withLabels(any())).thenReturn(podsList);
    when(podsList.list()).thenReturn(podList);
    super.listShouldReturnAllDtos();
  }

  @Test
  @Override
  void getOfAnExistingDtoShouldReturnTheExistingDto() {
    mockPodList();
    super.getOfAnExistingDtoShouldReturnTheExistingDto();
  }

  private void mockPodList() {
    when(configContext.getProperty(OperatorProperty.GRAFANA_EMBEDDED))
        .thenReturn(Optional.of("true"));
    when(clientFactory.create()).thenReturn(client);
    when(client.pods()).thenReturn(podsOperation);
    when(podsOperation.inNamespace(anyString())).thenReturn(podsList);
    when(podsList.withLabels(any())).thenReturn(podsList);
    when(podsList.list()).thenReturn(podList);
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
  protected ClusterResource getService(
      CustomResourceScanner<StackGresCluster> scanner,
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      AbstractResourceTransformer<ClusterDto, StackGresCluster> transformer) {
    final ClusterDtoFinder dtoFinder = new ClusterDtoFinder();
    dtoFinder.setClusterFinder(finder);
    dtoFinder.setClientFactory(clientFactory);
    dtoFinder.setClusterTransformer(getTransformer());
    final ClusterLabelFactory labelFactory = new ClusterLabelFactory();
    labelFactory.setLabelMapper(new ClusterLabelMapper());
    dtoFinder.setLabelFactory(labelFactory);
    final ClusterDtoScanner dtoScanner = new ClusterDtoScanner();
    dtoScanner.setClusterScanner(scanner);
    dtoScanner.setClientFactory(clientFactory);
    dtoScanner.setClusterTransformer(getTransformer());
    dtoScanner.setLabelFactory(labelFactory);

    return new ClusterResource(
        finder,
        scheduler, transformer,
        dtoScanner,
        dtoFinder,
        statusFinder,
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
    assertEquals("d7e660a9-377c-11ea-b04b-0242ac110004", resource.getSpec().getInitData().getRestore().getBackupUid());
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
    assertEquals("customValue", resource.getSpec().getPods()
        .getMetadata().getAnnotations().get("customAnnotation"));
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
    assertEquals("pgbouncerconf", resource.getSpec().getConfiguration().getConnectionPoolingConfig());
    assertEquals("5Gi", resource.getSpec().getPod().getPersistentVolume().getVolumeSize());
    assertEquals("standard", resource.getSpec().getPod().getPersistentVolume().getStorageClass());
    assertEquals(true, resource.getSpec().getPrometheusAutobind());
    assertEquals(1, resource.getSpec().getInstances());
    assertEquals("11.5", resource.getSpec().getPostgresVersion());
    assertEquals("postgresconf", resource.getSpec().getConfiguration().getPostgresConfig());
    assertEquals("size-xs", resource.getSpec().getResourceProfile());
    assertNotNull(resource.getSpec().getInitData().getRestore());
    assertEquals("d7e660a9-377c-11ea-b04b-0242ac110004", resource.getSpec().getInitData().getRestore().getBackupUid());
    assertNotNull(resource.getSpec().getDistributedLogs());
    assertEquals("distributedlogs", resource.getSpec().getDistributedLogs().getDistributedLogs());
    assertFalse(resource.getSpec().getPod().getDisableConnectionPooling());
    assertFalse(resource.getSpec().getPod().getDisableMetricsExporter());
    assertFalse(resource.getSpec().getPod().getDisableMetricsExporter());
    assertEquals("customValue", resource.getSpec().getPod()
        .getMetadata().getAnnotations().get("customAnnotation"));
    assertEquals("customLabelValue", resource.getSpec().getPod()
        .getMetadata().getLabels().get("customLabel"));
  }

  @Test
  void getLogsShouldNotFail() {
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
        assertEquals(parameters.getToTimeAndIndex(), Optional.of(Tuple.tuple(Instant.EPOCH, Integer.MAX_VALUE)));
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
    mockPodList();
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
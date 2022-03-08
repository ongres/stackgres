/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.BadRequestException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.apiweb.distributedlogs.DistributedLogsFetcher;
import io.stackgres.apiweb.distributedlogs.DistributedLogsQueryParameters;
import io.stackgres.apiweb.distributedlogs.FullTextSearchQuery;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.cluster.ClusterConfiguration;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterInitData;
import io.stackgres.apiweb.dto.cluster.ClusterLogEntryDto;
import io.stackgres.apiweb.dto.cluster.ClusterManagedScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterManagedSql;
import io.stackgres.apiweb.dto.cluster.ClusterPod;
import io.stackgres.apiweb.dto.cluster.ClusterPodPersistentVolume;
import io.stackgres.apiweb.dto.cluster.ClusterPodScheduling;
import io.stackgres.apiweb.dto.cluster.ClusterRestore;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.dto.cluster.ClusterSpecLabels;
import io.stackgres.apiweb.dto.cluster.ClusterSpecMetadata;
import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.resource.ClusterDtoFinder;
import io.stackgres.apiweb.resource.ClusterDtoScanner;
import io.stackgres.apiweb.resource.ClusterStatsDtoFinder;
import io.stackgres.apiweb.transformer.ClusterPodTransformer;
import io.stackgres.apiweb.transformer.ClusterStatsTransformer;
import io.stackgres.apiweb.transformer.ClusterTransformer;
import io.stackgres.apiweb.transformer.ScriptTransformer;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.ClusterLabelMapper;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodExecutor;
import io.stackgres.common.resource.PodFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.JsonUtil;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClusterResourceMockedTest extends
    AbstractCustomResourceTest<ClusterDto, StackGresCluster,
        ClusterResource, NamespacedClusterResource> {

  @Mock
  ManagedExecutor managedExecutor;
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
  private CustomResourceScheduler<StackGresScript> scriptScheduler;
  @Mock
  private ResourceWriter<ConfigMap> configMapWriter;
  @Mock
  private ResourceWriter<Secret> secretWriter;
  @Mock
  private CustomResourceFinder<StackGresScript> scriptFinder;
  @Mock
  private ResourceFinder<ConfigMap> configMapFinder;
  @Mock
  private ResourceFinder<Secret> secretFinder;
  @Mock
  private ResourceFinder<Service> serviceFinder;

  private ScriptTransformer scriptTransformer;

  private ExecutorService executorService;

  private Service servicePrimary;
  private Service serviceReplicas;
  private ConfigMap configMap;
  private PodList podList;
  private List<ClusterLogEntryDto> logList;
  private StackGresCluster clusterWithoutDistributedLogs;

  @Override
  @BeforeEach
  void setUp() {
    scriptTransformer = new ScriptTransformer(JsonUtil.jsonMapper());
    super.setUp();
    servicePrimary = new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(getResourceNamespace())
        .withName(PatroniUtil.readWriteName(getResourceName()))
        .endMetadata()
        .withNewSpec()
        .withType("ClusterIP")
        .endSpec()
        .build();
    serviceReplicas = new ServiceBuilder(servicePrimary)
        .editMetadata()
        .withName(PatroniUtil.readOnlyName(getResourceName()))
        .endMetadata()
        .withNewSpec().withType("LoadBalancer").endSpec()
        .withNewStatus().withNewLoadBalancer().addNewIngress()
        .withHostname("f4611c56942064ed5a468d8ce0a894ec.us-east-1.elb.amazonaws.com")
        .endIngress().endLoadBalancer().endStatus()
        .build();
    configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(getResourceNamespace())
        .withName("script")
        .endMetadata()
        .withData(ImmutableMap.of(
            "script", "CREATE DATABASE test WITH OWNER test"))
        .build();
    podList = Fixtures.cluster().podList().loadDefault().get();
    logList = new ArrayList<>();
    clusterWithoutDistributedLogs = Fixtures.cluster().loadWithoutDistributedLogs().get();
    executorService = Executors.newWorkStealingPool();
    doAnswer((Answer<Void>) invocation -> {
      executorService.execute(Runnable.class.cast(invocation.getArgument(0)));
      return null;
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
    clusterMocks();
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

    ClusterStatsDto dto =
        getClusterStatsResource().stats(getResourceNamespace(), getResourceName());

    checkStatsDto(dto);
  }

  @Test
  void createClusterWithScriptReference_shouldNotFail() {
    dto = getClusterScriptReference();

    super.createShouldNotFail();

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(0)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void updateClusterWithScriptReference_shouldNotFail() {
    dto = getClusterScriptReference();

    super.updateShouldNotFail();

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(1)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(0)).create(any());
    verify(scheduler, times(1)).update(any(), any());
    verify(scriptScheduler, times(0)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void createClusterWithExistingInlineScript_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildInlineScriptSpec();
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    when(scriptFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new StackGresScript()));

    super.createShouldNotFail();

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(0)).create(any());
    verify(scriptScheduler, times(1)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void createClusterWithInlineScript_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildInlineScriptSpec();
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(1)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void createClusterWithInlineScriptWithoutName_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildInlineScriptSpec();
    dto.getSpec().getManagedSql().getScripts().get(0).setSgScript(null);
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(1)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void createClusterWithSecretScript_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildSecretScriptSpec();
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    ArgumentCaptor<Secret> secretArgument = ArgumentCaptor.forClass(Secret.class);

    verify(secretWriter).create(secretArgument.capture());

    Secret createdSecret = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(), createdSecret.getMetadata().getNamespace());

    final ScriptFrom scriptFrom = scriptSpec.getScripts().get(0).getScriptFrom();
    final SecretKeySelector secretKeyRef = scriptFrom.getSecretKeyRef();

    assertEquals(secretKeyRef.getName(), createdSecret.getMetadata().getName());
    assertTrue(createdSecret.getData().containsKey(secretKeyRef.getKey()));
    final String actualScript = new String(Base64.getDecoder()
        .decode(createdSecret.getData().get(secretKeyRef.getKey())), StandardCharsets.UTF_8);
    assertEquals(scriptFrom.getSecretScript(), actualScript);

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(1)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(1)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void createClusterWithExistingSecretScript_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildSecretScriptSpec();
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new Secret()));

    super.createShouldNotFail();

    ArgumentCaptor<Secret> secretArgument = ArgumentCaptor.forClass(Secret.class);

    verify(secretWriter).update(secretArgument.capture());

    Secret createdSecret = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(), createdSecret.getMetadata().getNamespace());

    final ScriptFrom scriptFrom = scriptSpec.getScripts().get(0).getScriptFrom();
    final SecretKeySelector secretKeyRef = scriptFrom.getSecretKeyRef();

    assertEquals(secretKeyRef.getName(), createdSecret.getMetadata().getName());
    assertTrue(createdSecret.getData().containsKey(secretKeyRef.getKey()));
    final String actualScript = new String(Base64.getDecoder()
        .decode(createdSecret.getData().get(secretKeyRef.getKey())), StandardCharsets.UTF_8);
    assertEquals(scriptFrom.getSecretScript(), actualScript);

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(1)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(1)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void createClusterWithSecretScriptWithoutName_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildSecretScriptSpec();
    scriptSpec.getScripts().get(0).getScriptFrom().setSecretKeyRef(null);
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    ArgumentCaptor<Secret> secretArgument = ArgumentCaptor.forClass(Secret.class);

    verify(secretWriter).create(secretArgument.capture());

    Secret createdSecret = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(), createdSecret.getMetadata().getNamespace());

    final ScriptFrom scriptFrom = scriptSpec.getScripts().get(0).getScriptFrom();
    final SecretKeySelector secretKeyRef = scriptFrom.getSecretKeyRef();

    assertEquals(secretKeyRef.getName(), createdSecret.getMetadata().getName());
    assertTrue(createdSecret.getData().containsKey(secretKeyRef.getKey()));
    final String actualScript = new String(Base64.getDecoder().decode(
        createdSecret.getData().get(secretKeyRef.getKey())), StandardCharsets.UTF_8);
    assertEquals(scriptFrom.getSecretScript(), actualScript);

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(1)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(1)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void createClusterWithConfigMapScript_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildConfigMapScriptSpec();
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    ArgumentCaptor<ConfigMap> secretArgument = ArgumentCaptor.forClass(ConfigMap.class);

    verify(configMapWriter).create(secretArgument.capture());

    ConfigMap createdConfigMap = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(), createdConfigMap.getMetadata().getNamespace());

    final ScriptFrom scriptFrom = scriptSpec.getScripts().get(0).getScriptFrom();
    final ConfigMapKeySelector configMapKeyRef = scriptFrom.getConfigMapKeyRef();
    assertEquals(configMapKeyRef.getName(), createdConfigMap.getMetadata().getName());
    assertTrue(createdConfigMap.getData().containsKey(configMapKeyRef.getKey()));
    assertEquals(scriptFrom.getConfigMapScript(),
        createdConfigMap.getData().get(configMapKeyRef.getKey()));

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(1)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(1)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  @Test
  void createClusterWithExistingConfigMapScript_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildConfigMapScriptSpec();
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    when(configMapFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new ConfigMap()));

    super.createShouldNotFail();

    ArgumentCaptor<ConfigMap> secretArgument = ArgumentCaptor.forClass(ConfigMap.class);

    verify(configMapWriter).update(secretArgument.capture());

    ConfigMap createdConfigMap = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(), createdConfigMap.getMetadata().getNamespace());

    final ScriptFrom scriptFrom = scriptSpec.getScripts().get(0).getScriptFrom();
    final ConfigMapKeySelector configMapKeyRef = scriptFrom.getConfigMapKeyRef();
    assertEquals(configMapKeyRef.getName(), createdConfigMap.getMetadata().getName());
    assertTrue(createdConfigMap.getData().containsKey(configMapKeyRef.getKey()));
    assertEquals(scriptFrom.getConfigMapScript(),
        createdConfigMap.getData().get(configMapKeyRef.getKey()));

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(1)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(0)).create(any());
    verify(configMapWriter, times(1)).update(any());
  }

  @Test
  void createClusterWithConfigMapScriptWithoutName_shouldNotFail() {
    dto = getClusterScriptReference();
    ScriptSpec scriptSpec = buildConfigMapScriptSpec();
    scriptSpec.getScripts().get(0).getScriptFrom().setConfigMapKeyRef(null);
    dto.getSpec().getManagedSql().getScripts().get(0).setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    ArgumentCaptor<ConfigMap> secretArgument = ArgumentCaptor.forClass(ConfigMap.class);

    verify(configMapWriter).create(secretArgument.capture());

    ConfigMap createdConfigMap = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(), createdConfigMap.getMetadata().getNamespace());

    final ScriptFrom scriptFrom = scriptSpec.getScripts().get(0).getScriptFrom();
    final ConfigMapKeySelector configMapKeyRef = scriptFrom.getConfigMapKeyRef();
    assertEquals(configMapKeyRef.getName(), createdConfigMap.getMetadata().getName());
    assertTrue(createdConfigMap.getData().containsKey(configMapKeyRef.getKey()));
    assertEquals(scriptFrom.getConfigMapScript(),
        createdConfigMap.getData().get(configMapKeyRef.getKey()));

    verify(scanner, times(0)).findResources();
    verify(scanner, times(0)).findResources(any());
    verify(scanner, times(0)).getResources();
    verify(scanner, times(0)).getResources(any());
    verify(finder, times(0)).findByNameAndNamespace(any(), any());
    verify(scheduler, times(1)).create(any());
    verify(scheduler, times(0)).update(any(), any());
    verify(scriptScheduler, times(1)).create(any());
    verify(scriptScheduler, times(0)).update(any());
    verify(secretWriter, times(0)).create(any());
    verify(secretWriter, times(0)).update(any());
    verify(configMapWriter, times(1)).create(any());
    verify(configMapWriter, times(0)).update(any());
  }

  private ScriptSpec buildInlineScriptSpec() {
    ScriptSpec scriptSpec = new ScriptSpec();
    ScriptEntry entry = new ScriptEntry();
    scriptSpec.setScripts(List.of(entry));
    entry.setScript("CREATE DATABASE test");
    return scriptSpec;
  }

  private ScriptSpec buildSecretScriptSpec() {
    ScriptSpec scriptSpec = new ScriptSpec();
    ScriptEntry entry = new ScriptEntry();
    scriptSpec.setScripts(List.of(entry));
    entry.setScript(null);

    final ScriptFrom scriptFrom = new ScriptFrom();
    entry.setScriptFrom(scriptFrom);
    scriptFrom.setSecretScript("CREATE DATABASE test");

    final SecretKeySelector secretKeyRef = new SecretKeySelector();
    scriptFrom.setSecretKeyRef(secretKeyRef);

    final String randomKey = StringUtil.generateRandom(30);
    final String randomSecretName = StringUtil.generateRandom(30);

    secretKeyRef.setKey(randomKey);
    secretKeyRef.setName(randomSecretName);
    return scriptSpec;
  }

  private ScriptSpec buildConfigMapScriptSpec() {
    ScriptSpec scriptSpec = new ScriptSpec();
    ScriptEntry entry = new ScriptEntry();
    scriptSpec.setScripts(List.of(entry));

    entry.setScript(null);

    final ScriptFrom scriptFrom = new ScriptFrom();
    entry.setScriptFrom(scriptFrom);
    scriptFrom.setConfigMapScript("CREATE DATABASE test");

    final ConfigMapKeySelector configMapKeyRef = new ConfigMapKeySelector();
    scriptFrom.setConfigMapKeyRef(configMapKeyRef);

    final String randomKey = StringUtil.generateRandom(30);
    final String randomSecretName = StringUtil.generateRandom(30);

    configMapKeyRef.setKey(randomKey);
    configMapKeyRef.setName(randomSecretName);
    return scriptSpec;
  }

  private void clusterMocks() {
    when(configContext.get(WebApiProperty.GRAFANA_EMBEDDED))
        .thenReturn(Optional.of("true"));
    when(serviceFinder.findByNameAndNamespace(eq(PatroniUtil.name(getResourceName())),
        anyString()))
        .thenReturn(Optional.of(servicePrimary));
    when(serviceFinder.findByNameAndNamespace(eq(PatroniUtil.readOnlyName(getResourceName())),
        anyString()))
        .thenReturn(Optional.of(serviceReplicas));
    when(configMapFinder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(configMap));
    when(podFinder.findResourcesWithLabels(any())).thenReturn(podList.getItems());
    when(podFinder.findByLabelsAndNamespace(anyString(), any()))
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
        "load10m:1.5",
        "connections:2000"));
  }

  @Override
  protected CustomResourceList<StackGresCluster> getCustomResourceList() {
    return Fixtures.clusterList().loadDefault().get();
  }

  @Override
  protected ClusterDto getDto() {
    return DtoFixtures.cluster().loadDefault().get();
  }

  private ClusterDto getClusterScriptReference() {
    return DtoFixtures.cluster().loadInlineScripts().get();
  }

  @Override
  protected ClusterTransformer getTransformer() {
    return new ClusterTransformer(
        configContext, new ClusterPodTransformer(), JsonUtil.jsonMapper());
  }

  @Override
  protected ClusterResource getService() {
    ClusterTransformer clusterTransformer = getTransformer();
    final ClusterLabelFactory labelFactory = new ClusterLabelFactory(new ClusterLabelMapper());

    final ClusterDtoScanner dtoScanner = new ClusterDtoScanner();
    dtoScanner.setClusterScanner(scanner);
    dtoScanner.setPodFinder(podFinder);
    dtoScanner.setClusterTransformer(clusterTransformer);
    dtoScanner.setLabelFactory(labelFactory);

    final ClusterStatsTransformer clusterStatsTransformer = new ClusterStatsTransformer(
        new ClusterPodTransformer());
    final ClusterStatsDtoFinder statsDtoFinder = new ClusterStatsDtoFinder();
    statsDtoFinder.setClusterFinder(finder);
    statsDtoFinder.setPodFinder(podFinder);
    statsDtoFinder.setPodExecutor(podExecutor);
    statsDtoFinder.setPersistentVolumeClaimFinder(persistentVolumeClaimFinder);
    statsDtoFinder.setClusterLabelFactory(labelFactory);
    statsDtoFinder.setClusterStatsTransformer(clusterStatsTransformer);
    statsDtoFinder.setManagedExecutor(managedExecutor);

    return new ClusterResource(
        dtoScanner, scriptScheduler, secretWriter, configMapWriter,
        scriptFinder, scriptTransformer, secretFinder, configMapFinder, serviceFinder);
  }

  @Override
  protected NamespacedClusterResource getNamespacedService() {
    ClusterTransformer clusterTransformer = getTransformer();
    final ClusterLabelFactory labelFactory = new ClusterLabelFactory(new ClusterLabelMapper());
    final ClusterDtoFinder dtoFinder = new ClusterDtoFinder();
    dtoFinder.setClusterFinder(finder);
    dtoFinder.setPodFinder(podFinder);
    dtoFinder.setClusterTransformer(clusterTransformer);
    dtoFinder.setLabelFactory(labelFactory);
    return new NamespacedClusterResource(dtoFinder, getService());
  }

  private NamespacedClusterStatsResource getClusterStatsResource() {
    final ClusterLabelFactory labelFactory = new ClusterLabelFactory(new ClusterLabelMapper());
    final ClusterStatsTransformer clusterStatsTransformer = new ClusterStatsTransformer(
        new ClusterPodTransformer());
    final ClusterStatsDtoFinder statsDtoFinder = new ClusterStatsDtoFinder();
    statsDtoFinder.setClusterFinder(finder);
    statsDtoFinder.setPodFinder(podFinder);
    statsDtoFinder.setPodExecutor(podExecutor);
    statsDtoFinder.setPersistentVolumeClaimFinder(persistentVolumeClaimFinder);
    statsDtoFinder.setClusterLabelFactory(labelFactory);
    statsDtoFinder.setClusterStatsTransformer(clusterStatsTransformer);
    statsDtoFinder.setManagedExecutor(managedExecutor);

    return new NamespacedClusterStatsResource(statsDtoFinder);
  }

  private NamespacedClusterLogsResource getClusterLogsResource() {
    ClusterTransformer clusterTransformer = getTransformer();
    final ClusterLabelFactory labelFactory = new ClusterLabelFactory(new ClusterLabelMapper());
    final ClusterDtoFinder dtoFinder = new ClusterDtoFinder();
    dtoFinder.setClusterFinder(finder);
    dtoFinder.setPodFinder(podFinder);
    dtoFinder.setClusterTransformer(clusterTransformer);
    dtoFinder.setLabelFactory(labelFactory);

    return new NamespacedClusterLogsResource(dtoFinder, distributedLogsFetcher);
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
  protected void checkDto(ClusterDto dto, StackGresCluster resource) {
    if (dto.getPods() != null) {
      assertEquals(1, dto.getPodsReady());
      assertEquals(2, dto.getPods().size());
      assertEquals(4, dto.getPods().get(0).getContainers());
      assertEquals(4, dto.getPods().get(0).getContainersReady());
      assertEquals("10.244.3.23", dto.getPods().get(0).getIp());
      assertEquals("stackgres-0", dto.getPods().get(0).getName());
      assertEquals("stackgres", dto.getPods().get(0).getNamespace());
      assertEquals("primary", dto.getPods().get(0).getRole());
      assertEquals("Active", dto.getPods().get(0).getStatus());
      assertEquals(4, dto.getPods().get(0).getComponentVersions().size());
      assertEquals("12.2",
          dto.getPods().get(0).getComponentVersions().get("postgresql"));
      assertEquals("1.6.4",
          dto.getPods().get(0).getComponentVersions().get("patroni"));
      assertEquals("1.13.1",
          dto.getPods().get(0).getComponentVersions().get("envoy"));
      assertEquals("0.8",
          dto.getPods().get(0).getComponentVersions().get("prometheus-postgres-exporter"));
      assertEquals(4, dto.getPods().get(1).getContainers());
      assertEquals(0, dto.getPods().get(1).getContainersReady());
      assertNull(dto.getPods().get(1).getIp());
      assertEquals("stackgres-1", dto.getPods().get(1).getName());
      assertEquals("stackgres", dto.getPods().get(1).getNamespace());
      assertNull(dto.getPods().get(1).getRole());
      assertEquals("Pending", dto.getPods().get(1).getStatus());
      assertEquals(4, dto.getPods().get(1).getComponentVersions().size());
      assertEquals("12.2",
          dto.getPods().get(1).getComponentVersions().get("postgresql"));
      assertEquals("1.6.4",
          dto.getPods().get(1).getComponentVersions().get("patroni"));
      assertEquals("1.13.1",
          dto.getPods().get(1).getComponentVersions().get("envoy"));
      assertEquals("0.8",
          dto.getPods().get(1).getComponentVersions().get("prometheus-postgres-exporter"));
    }

    if (dto.getInfo() != null) {
      String appendDns = "." + resource.getMetadata().getNamespace();
      String expectedPrimaryDns =
          PatroniUtil.readWriteName(resource.getMetadata().getName()) + appendDns;
      String expectedReplicasDns = "f4611c56942064ed5a468d8ce0a894ec.us-east-1.elb.amazonaws.com";
      assertEquals(expectedPrimaryDns, dto.getInfo().getPrimaryDns());
      assertEquals(expectedReplicasDns, dto.getInfo().getReplicasDns());
      assertEquals("postgres", dto.getInfo().getSuperuserUsername());
      assertEquals("superuser-password", dto.getInfo().getSuperuserPasswordKey());
      assertEquals(resource.getMetadata().getName(), dto.getInfo().getSuperuserSecretName());
    }

  }

  @Override
  protected void checkCustomResource(StackGresCluster resource,
                                     ClusterDto resourceDto,
                                     Operation operation) {

    final Metadata dtoMetadata = resourceDto.getMetadata();
    final ObjectMeta resourceMetadata = resource.getMetadata();
    if (dtoMetadata != null) {
      assertNotNull(resourceMetadata);
      assertEquals(dtoMetadata.getName(), resourceMetadata.getName());
      assertEquals(dtoMetadata.getNamespace(), resourceMetadata.getNamespace());
      assertEquals(dtoMetadata.getUid(), resourceMetadata.getUid());
    } else {
      assertNull(resourceMetadata);
    }

    final ClusterSpec dtoSpec = resourceDto.getSpec();
    final StackGresClusterSpec resourceSpec = resource.getSpec();

    if (dtoSpec != null) {
      assertNotNull(resourceSpec);
      assertEquals(dtoSpec.getPrometheusAutobind(), resourceSpec.getPrometheusAutobind());
      assertEquals(dtoSpec.getInstances(), resourceSpec.getInstances());
      assertEquals(dtoSpec.getPostgres().getVersion(), resourceSpec.getPostgres().getVersion());
      assertEquals(dtoSpec.getSgInstanceProfile(), resourceSpec.getResourceProfile());

      final ClusterConfiguration dtoSpecConfigurations = dtoSpec.getConfigurations();

      final StackGresClusterConfiguration resourceSpecConfiguration = resourceSpec
          .getConfiguration();

      if (dtoSpecConfigurations != null) {
        assertNotNull(resourceSpecConfiguration);
        assertEquals(dtoSpecConfigurations.getSgBackupConfig(),
            resourceSpecConfiguration.getBackupConfig());
        assertEquals(dtoSpecConfigurations.getSgPoolingConfig(),
            resourceSpecConfiguration.getConnectionPoolingConfig());
        assertEquals(dtoSpecConfigurations.getSgPostgresConfig(),
            resourceSpecConfiguration.getPostgresConfig());
      } else {
        assertNull(resourceSpecConfiguration);
      }

      final ClusterPod dtoSpecPods = dtoSpec.getPods();
      if (dtoSpecPods != null) {
        final StackGresClusterPod resourceSpecPod = resourceSpec.getPod();
        assertNotNull(resourceSpecPod);
        assertEquals(dtoSpecPods.getDisableConnectionPooling(),
            resourceSpecPod.getDisableConnectionPooling());
        assertEquals(dtoSpecPods.getDisableMetricsExporter(),
            resourceSpecPod.getDisableMetricsExporter());
        assertEquals(dtoSpecPods.getDisableMetricsExporter(),
            resourceSpecPod.getDisableMetricsExporter());

        final ClusterPodPersistentVolume dtoPV = dtoSpecPods.getPersistentVolume();
        final StackGresPodPersistentVolume resourcePV = resourceSpecPod.getPersistentVolume();
        if (dtoPV != null) {
          assertNotNull(resourcePV);
          assertEquals(dtoPV.getSize(), resourcePV.getSize());
          assertEquals(dtoPV.getStorageClass(), resourcePV.getStorageClass());
        } else {
          assertNull(resourcePV);
        }

        final StackGresClusterSpecLabels resourceMetadataLabels =
            Optional.ofNullable(resourceSpec.getMetadata())
                .map(StackGresClusterSpecMetadata::getLabels)
                .orElse(null);
        final ClusterSpecLabels dtoMetadataLabels =
            Optional.ofNullable(dtoSpec.getMetadata())
                .map(ClusterSpecMetadata::getLabels)
                .orElse(null);
        if (dtoMetadataLabels != null) {
          assertNotNull(resourceMetadataLabels);
          assertEquals(dtoMetadataLabels.getClusterPods(), resourceMetadataLabels.getClusterPods());
        } else {
          assertNull(resourceMetadataLabels);
        }

        final ClusterPodScheduling podScheduling = dtoSpecPods.getScheduling();
        final StackGresClusterPodScheduling resourceScheduling = resourceSpecPod.getScheduling();
        if (podScheduling != null) {
          assertNotNull(resourceScheduling);
          assertEquals(podScheduling.getNodeSelector(), resourceScheduling.getNodeSelector());
          assertEquals(podScheduling.getNodeAffinity(), resourceScheduling.getNodeAffinity());
        } else {
          assertNull(resourceScheduling);
        }

      }

      final ClusterInitData dtoInitData = dtoSpec.getInitData();
      final StackGresClusterInitData resourceInitData = resourceSpec.getInitData();
      if (dtoInitData != null) {
        assertNotNull(resourceInitData);
        final ClusterRestore dtoRestore = dtoInitData.getRestore();
        final StackGresClusterRestore resourceRestore = resourceInitData.getRestore();
        if (dtoRestore != null) {
          assertNotNull(resourceRestore);
          assertEquals(dtoRestore.getFromBackup().getUid(),
              resourceRestore.getFromBackup().getUid());
          assertEquals(dtoRestore.getFromBackup().getName(),
              resourceRestore.getFromBackup().getName());
        } else {
          assertNull(resourceRestore);
        }
      }

      final ClusterManagedSql dtoManagedSql = dtoSpec.getManagedSql();
      final StackGresClusterManagedSql resourceManagedSql = resourceSpec.getManagedSql();
      if (dtoManagedSql != null) {
        if (dtoManagedSql.getScripts() != null) {
          assertNotNull(resourceManagedSql.getScripts());
          assertEquals(dtoManagedSql.getScripts().size(), resourceManagedSql.getScripts().size());

          Seq.zip(dtoManagedSql.getScripts(), resourceManagedSql.getScripts())
              .forEach(entryTuple -> {
                ClusterManagedScriptEntry dtoEntry = entryTuple.v1;
                StackGresClusterManagedScriptEntry resourceEntry = entryTuple.v2;
                assertEquals(dtoEntry.getId(), resourceEntry.getId());
                assertEquals(dtoEntry.getSgScript(), resourceEntry.getSgScript());
              });
        }
      } else {
        assertNull(resourceManagedSql);
      }

      if (dtoSpec.getDistributedLogs() != null) {
        assertNotNull(resourceSpec.getDistributedLogs());
        assertEquals(dtoSpec.getDistributedLogs().getDistributedLogs(),
            resourceSpec.getDistributedLogs().getDistributedLogs());
      } else {
        assertNull(resourceSpec.getDistributedLogs());
      }

    } else {
      assertNull(resourceSpec);
    }
  }

  private void checkStatsDto(ClusterStatsDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertEquals("1000m", resource.getCpuRequested());
    assertEquals("500m", resource.getCpuFound());
    assertEquals("0.50", resource.getCpuPsiAvg10());
    assertNull(resource.getCpuPsiAvg60());
    assertEquals("1.50", resource.getCpuPsiAvg300());
    assertEquals("10000000000", resource.getCpuPsiTotal());
    assertEquals("1.00Gi", resource.getMemoryRequested());
    assertEquals("512.00Mi", resource.getMemoryFound());
    assertEquals("278.00Mi", resource.getMemoryUsed());
    assertEquals("0.50", resource.getMemoryPsiAvg10());
    assertEquals("1.00", resource.getMemoryPsiAvg60());
    assertEquals("1.50", resource.getMemoryPsiAvg300());
    assertEquals("10000000000", resource.getMemoryPsiTotal());
    assertEquals("0.50", resource.getMemoryPsiFullAvg10());
    assertEquals("1.00", resource.getMemoryPsiFullAvg60());
    assertEquals("1.50", resource.getMemoryPsiFullAvg300());
    assertEquals("10000000000", resource.getMemoryPsiFullTotal());
    assertEquals("10.00Gi", resource.getDiskRequested());
    assertEquals("5.00Gi", resource.getDiskFound());
    assertEquals("1.40Gi", resource.getDiskUsed());
    assertEquals("0.50", resource.getDiskPsiAvg10());
    assertEquals("1.00", resource.getDiskPsiAvg60());
    assertEquals("1.50", resource.getDiskPsiAvg300());
    assertEquals("10000000000", resource.getDiskPsiTotal());
    assertEquals("0.50", resource.getDiskPsiFullAvg10());
    assertEquals("1.00", resource.getDiskPsiFullAvg60());
    assertEquals("1.50", resource.getDiskPsiFullAvg300());
    assertEquals("10000000000", resource.getDiskPsiFullTotal());
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
    assertNull(resource.getPods().get(1).getCpuFound());
    assertNull(resource.getPods().get(1).getCpuPsiAvg10());
    assertNull(resource.getPods().get(1).getCpuPsiAvg60());
    assertNull(resource.getPods().get(1).getCpuPsiAvg300());
    assertNull(resource.getPods().get(1).getCpuPsiTotal());
    assertEquals("512.00Mi", resource.getPods().get(1).getMemoryRequested());
    assertNull(resource.getPods().get(1).getMemoryFound());
    assertNull(resource.getPods().get(1).getMemoryUsed());
    assertNull(resource.getPods().get(1).getMemoryPsiAvg10());
    assertNull(resource.getPods().get(1).getMemoryPsiAvg60());
    assertNull(resource.getPods().get(1).getMemoryPsiAvg300());
    assertNull(resource.getPods().get(1).getMemoryPsiTotal());
    assertNull(resource.getPods().get(1).getMemoryPsiFullAvg10());
    assertNull(resource.getPods().get(1).getMemoryPsiFullAvg60());
    assertNull(resource.getPods().get(1).getMemoryPsiFullAvg300());
    assertNull(resource.getPods().get(1).getMemoryPsiFullTotal());
    assertEquals("5.00Gi", resource.getPods().get(1).getDiskRequested());
    assertNull(resource.getPods().get(1).getDiskFound());
    assertNull(resource.getPods().get(1).getDiskUsed());
    assertNull(resource.getPods().get(1).getDiskPsiAvg10());
    assertNull(resource.getPods().get(1).getDiskPsiAvg60());
    assertNull(resource.getPods().get(1).getDiskPsiAvg300());
    assertNull(resource.getPods().get(1).getDiskPsiFullAvg10());
    assertNull(resource.getPods().get(1).getDiskPsiFullAvg60());
    assertNull(resource.getPods().get(1).getDiskPsiFullAvg300());
    assertNull(resource.getPods().get(1).getDiskPsiFullTotal());
    assertNull(resource.getPods().get(1).getAverageLoad1m());
    assertNull(resource.getPods().get(1).getAverageLoad5m());
    assertNull(resource.getPods().get(1).getAverageLoad10m());
  }

  @Test
  void getLogsShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer((Answer<List<ClusterLogEntryDto>>) invocation -> {
      DistributedLogsQueryParameters parameters = invocation.getArgument(0);

      assertNotNull(parameters);
      checkDto(parameters.getCluster(), customResources.getItems().get(0));
      assertEquals(50, parameters.getRecords());
      assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
      assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
      assertEquals(ImmutableMap.of(), parameters.getFilters());
      assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
      assertFalse(parameters.isSortAsc());
      assertFalse(parameters.isFromInclusive());

      return logList;
    }).when(distributedLogsFetcher).logs(any());

    Integer records = null;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

  @Test
  void getLogsWithRecordsShouldNotFail() {
    clusterMocks();
    when(finder.findByNameAndNamespace(getResourceName(), getResourceNamespace()))
        .thenReturn(Optional.of(customResources.getItems().get(0)));
    doAnswer((Answer<List<ClusterLogEntryDto>>) invocation -> {
      DistributedLogsQueryParameters parameters = invocation.getArgument(0);

      assertNotNull(parameters);
      checkDto(parameters.getCluster(), customResources.getItems().get(0));
      assertEquals(1, parameters.getRecords());
      assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
      assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
      assertEquals(ImmutableMap.of(), parameters.getFilters());
      assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
      assertFalse(parameters.isSortAsc());
      assertFalse(parameters.isFromInclusive());

      return logList;
    }).when(distributedLogsFetcher).logs(any());

    Integer records = 1;
    String from = null;
    String to = null;
    String sort = null;
    String text = null;
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records,
            from, to, sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.of(Tuple.tuple(Instant.EPOCH, 0)), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(ImmutableMap.of(), parameters.getFilters());
        assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records,
            from, to, sort, text,
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records,
            from, to, sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.of(Tuple.tuple(Instant.EPOCH, 1)), parameters.getToTimeAndIndex());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records,
            from, to, sort, text,
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records,
            from, to, sort, text,
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records,
            from, to, sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    assertThrows(BadRequestException.class,
        () -> getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records,
            from, to, sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(parameters.getFilters(), ImmutableMap.of("logType", List.of("test")));
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
    List<String> logType = List.of("test");
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(parameters.getFilters(), ImmutableMap.of("podName", List.of("test")));
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
    List<String> logType = null;
    List<String> podName = List.of("test");
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(parameters.getFilters(), ImmutableMap.of("role", List.of("test")));
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = List.of("test");
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(parameters.getFilters(), ImmutableMap.of("errorLevel", List.of("test")));
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = List.of("test");
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(parameters.getFilters(), ImmutableMap.of("userName", List.of("test")));
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = List.of("test");
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(ImmutableMap.of("databaseName", List.of("test", "demo")),
            parameters.getFilters());
        assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = List.of("test", "demo");
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(parameters.getFilters(), ImmutableMap.of("logType", List.of()));
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
    List<String> logType = List.of("");
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(parameters.getFilters(), ImmutableMap.of("podName", List.of()));
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
    List<String> logType = null;
    List<String> podName = List.of("");
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(parameters.getFilters(), ImmutableMap.of("role", List.of()));
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = List.of("");
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(ImmutableMap.of("errorLevel", List.of()), parameters.getFilters());
        assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = List.of("");
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(ImmutableMap.of("userName", List.of()), parameters.getFilters());
        assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = List.of("");
    List<String> databaseName = null;
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(ImmutableMap.of("databaseName", List.of()), parameters.getFilters());
        assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = List.of("");
    Boolean fromInclusive = null;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(ImmutableMap.of(), parameters.getFilters());
        assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = true;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
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
        checkDto(parameters.getCluster(), customResources.getItems().get(0));
        assertEquals(50, parameters.getRecords());
        assertEquals(Optional.empty(), parameters.getFromTimeAndIndex());
        assertEquals(Optional.empty(), parameters.getToTimeAndIndex());
        assertEquals(ImmutableMap.of(), parameters.getFilters());
        assertEquals(Optional.empty(), parameters.getFullTextSearchQuery());
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
    List<String> logType = null;
    List<String> podName = null;
    List<String> role = null;
    List<String> errorLevel = null;
    List<String> userName = null;
    List<String> databaseName = null;
    Boolean fromInclusive = false;
    List<ClusterLogEntryDto> logs =
        getClusterLogsResource().logs(getResourceNamespace(), getResourceName(), records, from, to,
            sort, text,
            logType, podName, role, errorLevel, userName, databaseName, fromInclusive);

    assertIterableEquals(logList, logs);
  }

}

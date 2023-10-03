/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.cluster.ClusterConfigurations;
import io.stackgres.apiweb.dto.cluster.ClusterManagedScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterManagedSql;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterSpec;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatsDto;
import io.stackgres.apiweb.resource.ShardedClusterStatsDtoFinder;
import io.stackgres.apiweb.transformer.ClusterPodTransformer;
import io.stackgres.apiweb.transformer.ScriptTransformer;
import io.stackgres.apiweb.transformer.ShardedClusterStatsTransformer;
import io.stackgres.apiweb.transformer.ShardedClusterTransformer;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.PersistentVolumeClaimFinder;
import io.stackgres.common.resource.PodExecutor;
import io.stackgres.common.resource.PodFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.JsonUtil;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShardedClusterResourceMockedTest extends
    AbstractCustomResourceTest<ShardedClusterDto, StackGresShardedCluster,
      ShardedClusterResource, NamespacedShardedClusterResource> {

  @Mock
  ManagedExecutor managedExecutor;
  @Mock
  private StackGresPropertyContext<WebApiProperty> configContext;
  @Mock
  private CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder;
  @Mock
  protected CustomResourceScanner<StackGresCluster> clusterScanner;
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
  @Mock
  private PodExecutor podExecutor;
  @Mock
  private PodFinder podFinder;
  @Mock
  private PersistentVolumeClaimFinder persistentVolumeClaimFinder;

  private ScriptTransformer scriptTransformer;

  private ExecutorService executorService;

  private StackGresShardedCluster cluster;
  private Service servicePrimary;
  private ConfigMap configMap;
  private PodList podList;

  @Override
  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    scriptTransformer = new ScriptTransformer(JsonUtil.jsonMapper());
    super.setUp();
    servicePrimary = new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(getResourceNamespace())
        .withName(StackGresShardedClusterUtil
            .primaryCoordinatorServiceName(getResourceName()))
        .endMetadata()
        .withNewSpec()
        .withType("ClusterIP")
        .endSpec()
        .build();
    configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(getResourceNamespace())
        .withName("script")
        .endMetadata()
        .withData(ImmutableMap.of(
            "script", "CREATE DATABASE test WITH OWNER test"))
        .build();
    when(clusterScanner.getResourcesWithLabels(any(), any()))
        .thenReturn(List.of(Fixtures.cluster().loadDefault().get()));
    podList = Fixtures.cluster().podList().loadDefault().get();
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

    ShardedClusterStatsDto dto =
        getShardedClusterStatsResource().stats(getResourceNamespace(), getResourceName());

    checkStatsDto(dto);
  }

  @Test
  void createShardedClusterWithScriptReference_shouldNotFail() {
    dto = getShardedClusterScriptReference();

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
  void updateShardedClusterWithScriptReference_shouldNotFail() {
    dto = getShardedClusterScriptReference();

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
  void createShardedClusterWithExistingInlineScript_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildInlineScriptSpec();
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

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
  void createShardedClusterWithInlineScript_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildInlineScriptSpec();
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

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
  void createShardedClusterWithInlineScriptWithoutName_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildInlineScriptSpec();
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0).setSgScript(null);
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

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
  void createShardedClusterWithSecretScript_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildSecretScriptSpec();
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    ArgumentCaptor<Secret> secretArgument = ArgumentCaptor.forClass(Secret.class);

    verify(secretWriter).create(secretArgument.capture());

    Secret createdSecret = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(),
        createdSecret.getMetadata().getNamespace());

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
  void createShardedClusterWithExistingSecretScript_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildSecretScriptSpec();
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

    when(secretFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new Secret()));

    super.createShouldNotFail();

    ArgumentCaptor<Secret> secretArgument = ArgumentCaptor.forClass(Secret.class);

    verify(secretWriter).update(secretArgument.capture());

    Secret createdSecret = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(),
        createdSecret.getMetadata().getNamespace());

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
  void createShardedClusterWithSecretScriptWithoutName_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildSecretScriptSpec();
    scriptSpec.getScripts().get(0).getScriptFrom().setSecretKeyRef(null);
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    ArgumentCaptor<Secret> secretArgument = ArgumentCaptor.forClass(Secret.class);

    verify(secretWriter).create(secretArgument.capture());

    Secret createdSecret = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(),
        createdSecret.getMetadata().getNamespace());

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
  void createShardedClusterWithConfigMapScript_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildConfigMapScriptSpec();
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    ArgumentCaptor<ConfigMap> secretArgument = ArgumentCaptor.forClass(ConfigMap.class);

    verify(configMapWriter).create(secretArgument.capture());

    ConfigMap createdConfigMap = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(),
        createdConfigMap.getMetadata().getNamespace());

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
  void createShardedClusterWithExistingConfigMapScript_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildConfigMapScriptSpec();
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

    when(configMapFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(new ConfigMap()));

    super.createShouldNotFail();

    ArgumentCaptor<ConfigMap> secretArgument = ArgumentCaptor.forClass(ConfigMap.class);

    verify(configMapWriter).update(secretArgument.capture());

    ConfigMap createdConfigMap = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(),
        createdConfigMap.getMetadata().getNamespace());

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
  void createShardedClusterWithConfigMapScriptWithoutName_shouldNotFail() {
    dto = getShardedClusterScriptReference();
    ScriptSpec scriptSpec = buildConfigMapScriptSpec();
    scriptSpec.getScripts().get(0).getScriptFrom().setConfigMapKeyRef(null);
    dto.getSpec().getCoordinator().getManagedSql().getScripts().get(0)
        .setScriptSpec(scriptSpec);

    super.createShouldNotFail();

    ArgumentCaptor<ConfigMap> secretArgument = ArgumentCaptor.forClass(ConfigMap.class);

    verify(configMapWriter).create(secretArgument.capture());

    ConfigMap createdConfigMap = secretArgument.getValue();
    assertEquals(dto.getMetadata().getNamespace(),
        createdConfigMap.getMetadata().getNamespace());

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
    when(shardedClusterFinder.findByNameAndNamespace(
        eq(getResourceName()),
        anyString()))
        .thenReturn(Optional.of(cluster));
    when(serviceFinder.findByNameAndNamespace(
        eq(getResourceName()),
        anyString()))
        .thenReturn(Optional.of(servicePrimary));
    when(configMapFinder.findByNameAndNamespace(anyString(), anyString()))
        .thenReturn(Optional.of(configMap));
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
  protected DefaultKubernetesResourceList<StackGresShardedCluster> getCustomResourceList() {
    return Fixtures.shardedClusterList().loadDefault().get();
  }

  @Override
  protected ShardedClusterDto getDto() {
    return DtoFixtures.shardedCluster().loadDefault().get();
  }

  private ShardedClusterDto getShardedClusterScriptReference() {
    return DtoFixtures.shardedCluster().loadInlineScripts().get();
  }

  @Override
  protected ShardedClusterTransformer getTransformer() {
    return new ShardedClusterTransformer(
        configContext, JsonUtil.jsonMapper());
  }

  @Override
  protected ShardedClusterResource getService() {
    return new ShardedClusterResource(
        shardedClusterFinder, scriptScheduler, secretWriter, configMapWriter,
        scriptFinder, scriptTransformer, secretFinder, configMapFinder, serviceFinder);
  }

  @Override
  protected NamespacedShardedClusterResource getNamespacedService() {
    ShardedClusterTransformer clusterTransformer = getTransformer();
    ShardedClusterResource service = getService();
    service.finder = finder;
    service.transformer = clusterTransformer;
    return new NamespacedShardedClusterResource(service);
  }

  private NamespacedShardedClusterStatsResource getShardedClusterStatsResource() {
    final ShardedClusterLabelFactory shardedClusterLabelFactory = new ShardedClusterLabelFactory(
        new ShardedClusterLabelMapper());
    final ClusterLabelFactory clusterLabelFactory = new ClusterLabelFactory(
        new ClusterLabelMapper());
    final ShardedClusterStatsTransformer shardedClusterStatsTransformer =
        new ShardedClusterStatsTransformer(new ClusterPodTransformer());
    final ShardedClusterStatsDtoFinder statsDtoFinder = new ShardedClusterStatsDtoFinder(
        shardedClusterFinder, clusterScanner, managedExecutor, podFinder,
        podExecutor, persistentVolumeClaimFinder, shardedClusterLabelFactory,
        clusterLabelFactory, shardedClusterStatsTransformer);

    return new NamespacedShardedClusterStatsResource(statsDtoFinder);
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
  protected void checkDto(ShardedClusterDto dto, StackGresShardedCluster customResource) {
    if (dto.getInfo() != null) {
      String appendDns = "." + "stackgres";
      String expectedPrimaryDns = StackGresShardedClusterUtil
          .primaryCoordinatorServiceName("stackgres") + appendDns;
      assertEquals(expectedPrimaryDns, dto.getInfo().getPrimaryDns());
      assertEquals("postgres", dto.getInfo().getSuperuserUsername());
      assertEquals("superuser-password", dto.getInfo().getSuperuserPasswordKey());
      assertEquals("stackgres", dto.getInfo().getSuperuserSecretName());
    }

  }

  @Override
  protected void checkCustomResource(
      StackGresShardedCluster resource,
      ShardedClusterDto dto,
      Operation operation) {
    final Metadata dtoMetadata = dto.getMetadata();
    final ObjectMeta resourceMetadata = resource.getMetadata();
    if (dtoMetadata != null) {
      assertNotNull(resourceMetadata);
      assertEquals(dtoMetadata.getName(), resourceMetadata.getName());
      assertEquals(dtoMetadata.getNamespace(), resourceMetadata.getNamespace());
      assertEquals(dtoMetadata.getUid(), resourceMetadata.getUid());
    } else {
      assertNull(resourceMetadata);
    }

    final ShardedClusterSpec dtoSpec = dto.getSpec();
    final StackGresShardedClusterSpec resourceSpec = resource.getSpec();

    if (dtoSpec != null) {
      assertNotNull(resourceSpec);
      assertEquals(dtoSpec.getPrometheusAutobind(), resourceSpec.getPrometheusAutobind());
      assertEquals(dtoSpec.getCoordinator().getInstances(),
          resourceSpec.getCoordinator().getInstances());
      assertEquals(dtoSpec.getPostgres().getVersion(), resourceSpec.getPostgres().getVersion());
      assertEquals(dtoSpec.getCoordinator().getSgInstanceProfile(),
          resourceSpec.getCoordinator().getSgInstanceProfile());

      final ClusterConfigurations dtoSpecConfigurations =
          dtoSpec.getCoordinator().getConfigurationsForCoordinator();

      final StackGresClusterConfigurations resourceSpecConfiguration = resourceSpec
          .getCoordinator().getConfigurations();

      if (dtoSpecConfigurations != null) {
        assertNotNull(resourceSpecConfiguration);
        assertEquals(dtoSpecConfigurations.getSgBackupConfig(),
            resourceSpecConfiguration.getSgBackupConfig());
        assertEquals(dtoSpecConfigurations.getSgPoolingConfig(),
            resourceSpecConfiguration.getSgPoolingConfig());
        assertEquals(dtoSpecConfigurations.getSgPostgresConfig(),
            resourceSpecConfiguration.getSgPostgresConfig());
      } else {
        assertNull(resourceSpecConfiguration);
      }

      final ClusterManagedSql dtoManagedSql = dtoSpec.getCoordinator().getManagedSql();
      final StackGresClusterManagedSql resourceManagedSql =
          resourceSpec.getCoordinator().getManagedSql();
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
        assertEquals(dtoSpec.getDistributedLogs().getSgDistributedLogs(),
            resourceSpec.getDistributedLogs().getSgDistributedLogs());
      } else {
        assertNull(resourceSpec.getDistributedLogs());
      }

    } else {
      assertNull(resourceSpec);
    }
  }

  private void checkStatsDto(ShardedClusterStatsDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("6fe0edf5-8a6d-43b7-99bd-131e2efeab66", resource.getMetadata().getUid());

    assertEquals("1000m", resource.getCoordinator().getCpuRequested());
    assertEquals("500m", resource.getCoordinator().getCpuFound());
    assertEquals("0.50", resource.getCoordinator().getCpuPsiAvg10());
    assertNull(resource.getCoordinator().getCpuPsiAvg60());
    assertEquals("1.50", resource.getCoordinator().getCpuPsiAvg300());
    assertEquals("10000000000", resource.getCoordinator().getCpuPsiTotal());
    assertEquals("1.00Gi", resource.getCoordinator().getMemoryRequested());
    assertEquals("512.00Mi", resource.getCoordinator().getMemoryFound());
    assertEquals("278.00Mi", resource.getCoordinator().getMemoryUsed());
    assertEquals("0.50", resource.getCoordinator().getMemoryPsiAvg10());
    assertEquals("1.00", resource.getCoordinator().getMemoryPsiAvg60());
    assertEquals("1.50", resource.getCoordinator().getMemoryPsiAvg300());
    assertEquals("10000000000", resource.getCoordinator().getMemoryPsiTotal());
    assertEquals("0.50", resource.getCoordinator().getMemoryPsiFullAvg10());
    assertEquals("1.00", resource.getCoordinator().getMemoryPsiFullAvg60());
    assertEquals("1.50", resource.getCoordinator().getMemoryPsiFullAvg300());
    assertEquals("10000000000", resource.getCoordinator().getMemoryPsiFullTotal());
    assertEquals("10.00Gi", resource.getCoordinator().getDiskRequested());
    assertEquals("5.00Gi", resource.getCoordinator().getDiskFound());
    assertEquals("1.40Gi", resource.getCoordinator().getDiskUsed());
    assertEquals("0.50", resource.getCoordinator().getDiskPsiAvg10());
    assertEquals("1.00", resource.getCoordinator().getDiskPsiAvg60());
    assertEquals("1.50", resource.getCoordinator().getDiskPsiAvg300());
    assertEquals("10000000000", resource.getCoordinator().getDiskPsiTotal());
    assertEquals("0.50", resource.getCoordinator().getDiskPsiFullAvg10());
    assertEquals("1.00", resource.getCoordinator().getDiskPsiFullAvg60());
    assertEquals("1.50", resource.getCoordinator().getDiskPsiFullAvg300());
    assertEquals("10000000000", resource.getCoordinator().getDiskPsiFullTotal());
    assertEquals("0.50", resource.getCoordinator().getAverageLoad1m());
    assertEquals("1.00", resource.getCoordinator().getAverageLoad5m());
    assertEquals("1.50", resource.getCoordinator().getAverageLoad10m());
    assertEquals(1, resource.getCoordinator().getPodsReady());
    assertNotNull(resource.getCoordinator().getPods());
    assertEquals(2, resource.getCoordinator().getPods().size());
    assertEquals(4, resource.getCoordinator().getPods().get(0).getContainers());
    assertEquals(4, resource.getCoordinator().getPods().get(0).getContainersReady());
    assertEquals("10.244.3.23", resource.getCoordinator().getPods().get(0).getIp());
    assertEquals("stackgres-0", resource.getCoordinator().getPods().get(0).getName());
    assertEquals("stackgres", resource.getCoordinator().getPods().get(0).getNamespace());
    assertEquals("primary", resource.getCoordinator().getPods().get(0).getRole());
    assertEquals("Active", resource.getCoordinator().getPods().get(0).getStatus());
    assertEquals("500m", resource.getCoordinator().getPods().get(0).getCpuRequested());
    assertEquals("500m", resource.getCoordinator().getPods().get(0).getCpuFound());
    assertEquals("0.50", resource.getCoordinator().getPods().get(0).getCpuPsiAvg10());
    assertNull(resource.getCoordinator().getPods().get(0).getCpuPsiAvg60());
    assertEquals("1.50", resource.getCoordinator().getPods().get(0).getCpuPsiAvg300());
    assertEquals("10000000000", resource.getCoordinator().getPods().get(0).getCpuPsiTotal());
    assertEquals("512.00Mi", resource.getCoordinator().getPods().get(0).getMemoryRequested());
    assertEquals("512.00Mi", resource.getCoordinator().getPods().get(0).getMemoryFound());
    assertEquals("278.00Mi", resource.getCoordinator().getPods().get(0).getMemoryUsed());
    assertEquals("0.50", resource.getCoordinator().getPods().get(0).getMemoryPsiAvg10());
    assertEquals("1.00", resource.getCoordinator().getPods().get(0).getMemoryPsiAvg60());
    assertEquals("1.50", resource.getCoordinator().getPods().get(0).getMemoryPsiAvg300());
    assertEquals("10000000000", resource.getCoordinator().getPods().get(0).getMemoryPsiTotal());
    assertEquals("0.50", resource.getCoordinator().getPods().get(0).getMemoryPsiFullAvg10());
    assertEquals("1.00", resource.getCoordinator().getPods().get(0).getMemoryPsiFullAvg60());
    assertEquals("1.50", resource.getCoordinator().getPods().get(0).getMemoryPsiFullAvg300());
    assertEquals("10000000000", resource.getCoordinator().getPods().get(0).getMemoryPsiFullTotal());
    assertEquals("5.00Gi", resource.getCoordinator().getPods().get(0).getDiskRequested());
    assertEquals("5.00Gi", resource.getCoordinator().getPods().get(0).getDiskFound());
    assertEquals("1.40Gi", resource.getCoordinator().getPods().get(0).getDiskUsed());
    assertEquals("0.50", resource.getCoordinator().getPods().get(0).getDiskPsiAvg10());
    assertEquals("1.00", resource.getCoordinator().getPods().get(0).getDiskPsiAvg60());
    assertEquals("1.50", resource.getCoordinator().getPods().get(0).getDiskPsiAvg300());
    assertEquals("10000000000", resource.getCoordinator().getPods().get(0).getDiskPsiTotal());
    assertEquals("0.50", resource.getCoordinator().getPods().get(0).getDiskPsiFullAvg10());
    assertEquals("1.00", resource.getCoordinator().getPods().get(0).getDiskPsiFullAvg60());
    assertEquals("1.50", resource.getCoordinator().getPods().get(0).getDiskPsiFullAvg300());
    assertEquals("10000000000", resource.getCoordinator().getPods().get(0).getDiskPsiFullTotal());
    assertEquals("0.50", resource.getCoordinator().getPods().get(0).getAverageLoad1m());
    assertEquals("1.00", resource.getCoordinator().getPods().get(0).getAverageLoad5m());
    assertEquals("1.50", resource.getCoordinator().getPods().get(0).getAverageLoad10m());
    assertEquals(4, resource.getCoordinator().getPods().get(1).getContainers());
    assertEquals(0, resource.getCoordinator().getPods().get(1).getContainersReady());
    assertNull(resource.getCoordinator().getPods().get(1).getIp());
    assertEquals("stackgres-1", resource.getCoordinator().getPods().get(1).getName());
    assertEquals("stackgres", resource.getCoordinator().getPods().get(1).getNamespace());
    assertNull(resource.getCoordinator().getPods().get(1).getRole());
    assertEquals("Pending", resource.getCoordinator().getPods().get(1).getStatus());
    assertEquals("500m", resource.getCoordinator().getPods().get(1).getCpuRequested());
    assertNull(resource.getCoordinator().getPods().get(1).getCpuFound());
    assertNull(resource.getCoordinator().getPods().get(1).getCpuPsiAvg10());
    assertNull(resource.getCoordinator().getPods().get(1).getCpuPsiAvg60());
    assertNull(resource.getCoordinator().getPods().get(1).getCpuPsiAvg300());
    assertNull(resource.getCoordinator().getPods().get(1).getCpuPsiTotal());
    assertEquals("512.00Mi", resource.getCoordinator().getPods().get(1).getMemoryRequested());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryFound());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryUsed());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryPsiAvg10());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryPsiAvg60());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryPsiAvg300());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryPsiTotal());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryPsiFullAvg10());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryPsiFullAvg60());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryPsiFullAvg300());
    assertNull(resource.getCoordinator().getPods().get(1).getMemoryPsiFullTotal());
    assertEquals("5.00Gi", resource.getCoordinator().getPods().get(1).getDiskRequested());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskFound());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskUsed());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskPsiAvg10());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskPsiAvg60());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskPsiAvg300());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskPsiFullAvg10());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskPsiFullAvg60());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskPsiFullAvg300());
    assertNull(resource.getCoordinator().getPods().get(1).getDiskPsiFullTotal());
    assertNull(resource.getCoordinator().getPods().get(1).getAverageLoad1m());
    assertNull(resource.getCoordinator().getPods().get(1).getAverageLoad5m());
    assertNull(resource.getCoordinator().getPods().get(1).getAverageLoad10m());

    assertEquals("1000m", resource.getShards().getCpuRequested());
    assertEquals("500m", resource.getShards().getCpuFound());
    assertEquals("0.50", resource.getShards().getCpuPsiAvg10());
    assertNull(resource.getShards().getCpuPsiAvg60());
    assertEquals("1.50", resource.getShards().getCpuPsiAvg300());
    assertEquals("10000000000", resource.getShards().getCpuPsiTotal());
    assertEquals("1.00Gi", resource.getShards().getMemoryRequested());
    assertEquals("512.00Mi", resource.getShards().getMemoryFound());
    assertEquals("278.00Mi", resource.getShards().getMemoryUsed());
    assertEquals("0.50", resource.getShards().getMemoryPsiAvg10());
    assertEquals("1.00", resource.getShards().getMemoryPsiAvg60());
    assertEquals("1.50", resource.getShards().getMemoryPsiAvg300());
    assertEquals("10000000000", resource.getShards().getMemoryPsiTotal());
    assertEquals("0.50", resource.getShards().getMemoryPsiFullAvg10());
    assertEquals("1.00", resource.getShards().getMemoryPsiFullAvg60());
    assertEquals("1.50", resource.getShards().getMemoryPsiFullAvg300());
    assertEquals("10000000000", resource.getShards().getMemoryPsiFullTotal());
    assertEquals("10.00Gi", resource.getShards().getDiskRequested());
    assertEquals("5.00Gi", resource.getShards().getDiskFound());
    assertEquals("1.40Gi", resource.getShards().getDiskUsed());
    assertEquals("0.50", resource.getShards().getDiskPsiAvg10());
    assertEquals("1.00", resource.getShards().getDiskPsiAvg60());
    assertEquals("1.50", resource.getShards().getDiskPsiAvg300());
    assertEquals("10000000000", resource.getShards().getDiskPsiTotal());
    assertEquals("0.50", resource.getShards().getDiskPsiFullAvg10());
    assertEquals("1.00", resource.getShards().getDiskPsiFullAvg60());
    assertEquals("1.50", resource.getShards().getDiskPsiFullAvg300());
    assertEquals("10000000000", resource.getShards().getDiskPsiFullTotal());
    assertEquals("0.50", resource.getShards().getAverageLoad1m());
    assertEquals("1.00", resource.getShards().getAverageLoad5m());
    assertEquals("1.50", resource.getShards().getAverageLoad10m());
    assertEquals(1, resource.getShards().getPodsReady());
    assertNotNull(resource.getShards().getPods());
    assertEquals(2, resource.getShards().getPods().size());
    assertEquals(4, resource.getShards().getPods().get(0).getContainers());
    assertEquals(4, resource.getShards().getPods().get(0).getContainersReady());
    assertEquals("10.244.3.23", resource.getShards().getPods().get(0).getIp());
    assertEquals("stackgres-0", resource.getShards().getPods().get(0).getName());
    assertEquals("stackgres", resource.getShards().getPods().get(0).getNamespace());
    assertEquals("primary", resource.getShards().getPods().get(0).getRole());
    assertEquals("Active", resource.getShards().getPods().get(0).getStatus());
    assertEquals("500m", resource.getShards().getPods().get(0).getCpuRequested());
    assertEquals("500m", resource.getShards().getPods().get(0).getCpuFound());
    assertEquals("0.50", resource.getShards().getPods().get(0).getCpuPsiAvg10());
    assertNull(resource.getShards().getPods().get(0).getCpuPsiAvg60());
    assertEquals("1.50", resource.getShards().getPods().get(0).getCpuPsiAvg300());
    assertEquals("10000000000", resource.getShards().getPods().get(0).getCpuPsiTotal());
    assertEquals("512.00Mi", resource.getShards().getPods().get(0).getMemoryRequested());
    assertEquals("512.00Mi", resource.getShards().getPods().get(0).getMemoryFound());
    assertEquals("278.00Mi", resource.getShards().getPods().get(0).getMemoryUsed());
    assertEquals("0.50", resource.getShards().getPods().get(0).getMemoryPsiAvg10());
    assertEquals("1.00", resource.getShards().getPods().get(0).getMemoryPsiAvg60());
    assertEquals("1.50", resource.getShards().getPods().get(0).getMemoryPsiAvg300());
    assertEquals("10000000000", resource.getShards().getPods().get(0).getMemoryPsiTotal());
    assertEquals("0.50", resource.getShards().getPods().get(0).getMemoryPsiFullAvg10());
    assertEquals("1.00", resource.getShards().getPods().get(0).getMemoryPsiFullAvg60());
    assertEquals("1.50", resource.getShards().getPods().get(0).getMemoryPsiFullAvg300());
    assertEquals("10000000000", resource.getShards().getPods().get(0).getMemoryPsiFullTotal());
    assertEquals("5.00Gi", resource.getShards().getPods().get(0).getDiskRequested());
    assertEquals("5.00Gi", resource.getShards().getPods().get(0).getDiskFound());
    assertEquals("1.40Gi", resource.getShards().getPods().get(0).getDiskUsed());
    assertEquals("0.50", resource.getShards().getPods().get(0).getDiskPsiAvg10());
    assertEquals("1.00", resource.getShards().getPods().get(0).getDiskPsiAvg60());
    assertEquals("1.50", resource.getShards().getPods().get(0).getDiskPsiAvg300());
    assertEquals("10000000000", resource.getShards().getPods().get(0).getDiskPsiTotal());
    assertEquals("0.50", resource.getShards().getPods().get(0).getDiskPsiFullAvg10());
    assertEquals("1.00", resource.getShards().getPods().get(0).getDiskPsiFullAvg60());
    assertEquals("1.50", resource.getShards().getPods().get(0).getDiskPsiFullAvg300());
    assertEquals("10000000000", resource.getShards().getPods().get(0).getDiskPsiFullTotal());
    assertEquals("0.50", resource.getShards().getPods().get(0).getAverageLoad1m());
    assertEquals("1.00", resource.getShards().getPods().get(0).getAverageLoad5m());
    assertEquals("1.50", resource.getShards().getPods().get(0).getAverageLoad10m());
    assertEquals(4, resource.getShards().getPods().get(1).getContainers());
    assertEquals(0, resource.getShards().getPods().get(1).getContainersReady());
    assertNull(resource.getShards().getPods().get(1).getIp());
    assertEquals("stackgres-1", resource.getShards().getPods().get(1).getName());
    assertEquals("stackgres", resource.getShards().getPods().get(1).getNamespace());
    assertNull(resource.getShards().getPods().get(1).getRole());
    assertEquals("Pending", resource.getShards().getPods().get(1).getStatus());
    assertEquals("500m", resource.getShards().getPods().get(1).getCpuRequested());
    assertNull(resource.getShards().getPods().get(1).getCpuFound());
    assertNull(resource.getShards().getPods().get(1).getCpuPsiAvg10());
    assertNull(resource.getShards().getPods().get(1).getCpuPsiAvg60());
    assertNull(resource.getShards().getPods().get(1).getCpuPsiAvg300());
    assertNull(resource.getShards().getPods().get(1).getCpuPsiTotal());
    assertEquals("512.00Mi", resource.getShards().getPods().get(1).getMemoryRequested());
    assertNull(resource.getShards().getPods().get(1).getMemoryFound());
    assertNull(resource.getShards().getPods().get(1).getMemoryUsed());
    assertNull(resource.getShards().getPods().get(1).getMemoryPsiAvg10());
    assertNull(resource.getShards().getPods().get(1).getMemoryPsiAvg60());
    assertNull(resource.getShards().getPods().get(1).getMemoryPsiAvg300());
    assertNull(resource.getShards().getPods().get(1).getMemoryPsiTotal());
    assertNull(resource.getShards().getPods().get(1).getMemoryPsiFullAvg10());
    assertNull(resource.getShards().getPods().get(1).getMemoryPsiFullAvg60());
    assertNull(resource.getShards().getPods().get(1).getMemoryPsiFullAvg300());
    assertNull(resource.getShards().getPods().get(1).getMemoryPsiFullTotal());
    assertEquals("5.00Gi", resource.getShards().getPods().get(1).getDiskRequested());
    assertNull(resource.getShards().getPods().get(1).getDiskFound());
    assertNull(resource.getShards().getPods().get(1).getDiskUsed());
    assertNull(resource.getShards().getPods().get(1).getDiskPsiAvg10());
    assertNull(resource.getShards().getPods().get(1).getDiskPsiAvg60());
    assertNull(resource.getShards().getPods().get(1).getDiskPsiAvg300());
    assertNull(resource.getShards().getPods().get(1).getDiskPsiFullAvg10());
    assertNull(resource.getShards().getPods().get(1).getDiskPsiFullAvg60());
    assertNull(resource.getShards().getPods().get(1).getDiskPsiFullAvg300());
    assertNull(resource.getShards().getPods().get(1).getDiskPsiFullTotal());
    assertNull(resource.getShards().getPods().get(1).getAverageLoad1m());
    assertNull(resource.getShards().getPods().get(1).getAverageLoad5m());
    assertNull(resource.getShards().getPods().get(1).getAverageLoad10m());
  }

}

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

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
import io.stackgres.apiweb.dto.cluster.ClusterConfiguration;
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
import io.stackgres.apiweb.transformer.ScriptTransformer;
import io.stackgres.apiweb.transformer.ShardedClusterStatsTransformer;
import io.stackgres.apiweb.transformer.ShardedClusterTransformer;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.StackGresShardedClusterForCitusUtil;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
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
import io.stackgres.common.resource.PodFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShardedClusterResourceMockedTest extends
    AbstractCustomResourceTest<ShardedClusterDto, StackGresShardedCluster,
      ShardedClusterResource, NamespacedShardedClusterResource> {

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
  private PodFinder podFinder;
  @Mock
  private PersistentVolumeClaimFinder persistentVolumeClaimFinder;

  private ScriptTransformer scriptTransformer;

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
        .withName(StackGresShardedClusterForCitusUtil
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
        new ShardedClusterStatsTransformer();
    final ShardedClusterStatsDtoFinder statsDtoFinder = new ShardedClusterStatsDtoFinder(
        shardedClusterFinder, clusterScanner, podFinder, persistentVolumeClaimFinder,
        shardedClusterLabelFactory, clusterLabelFactory, shardedClusterStatsTransformer);

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
      String expectedPrimaryDns = StackGresShardedClusterForCitusUtil
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
          resourceSpec.getCoordinator().getResourceProfile());

      final ClusterConfiguration dtoSpecConfigurations =
          dtoSpec.getCoordinator().getConfigurationForCoordinator();

      final StackGresClusterConfiguration resourceSpecConfiguration = resourceSpec
          .getCoordinator().getConfiguration();

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
        assertEquals(dtoSpec.getDistributedLogs().getDistributedLogs(),
            resourceSpec.getDistributedLogs().getDistributedLogs());
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
    assertEquals("1.00Gi", resource.getCoordinator().getMemoryRequested());
    assertEquals("10.00Gi", resource.getCoordinator().getDiskRequested());
    assertEquals("1000m", resource.getShards().getCpuRequested());
    assertEquals("1.00Gi", resource.getShards().getMemoryRequested());
    assertEquals("10.00Gi", resource.getShards().getDiskRequested());
  }

}

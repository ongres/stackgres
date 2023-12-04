/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.cluster.ClusterManagedScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterManagedSql;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterCoordinator;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterInfo;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterShard;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterShards;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterSpec;
import io.stackgres.apiweb.transformer.ScriptTransformer;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

@Path("sgshardedclusters")
@RequestScoped
@Authenticated
public class ShardedClusterResource
    extends AbstractCustomResourceService<ShardedClusterDto, StackGresShardedCluster> {

  public static final String DEFAULT_SCRIPT_KEY = ScriptResource.DEFAULT_SCRIPT_KEY;

  private final CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder;
  private final CustomResourceScheduler<StackGresScript> scriptScheduler;
  private final ResourceWriter<Secret> secretWriter;
  private final ResourceWriter<ConfigMap> configMapWriter;
  private final CustomResourceFinder<StackGresScript> scriptFinder;
  private final ScriptTransformer scriptTransformer;
  private final ResourceFinder<Secret> secretFinder;
  private final ResourceFinder<ConfigMap> configMapFinder;
  private final ResourceFinder<Service> serviceFinder;

  @Inject
  public ShardedClusterResource(
      CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder,
      CustomResourceScheduler<StackGresScript> scriptScheduler,
      ResourceWriter<Secret> secretWriter,
      ResourceWriter<ConfigMap> configMapWriter,
      CustomResourceFinder<StackGresScript> scriptFinder,
      ScriptTransformer scriptTransformer,
      ResourceFinder<Secret> secretFinder,
      ResourceFinder<ConfigMap> configMapFinder,
      ResourceFinder<Service> serviceFinder) {
    this.shardedClusterFinder = shardedClusterFinder;
    this.scriptScheduler = scriptScheduler;
    this.secretWriter = secretWriter;
    this.configMapWriter = configMapWriter;
    this.scriptFinder = scriptFinder;
    this.scriptTransformer = scriptTransformer;
    this.secretFinder = secretFinder;
    this.configMapFinder = configMapFinder;
    this.serviceFinder = serviceFinder;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ShardedClusterDto.class))})
  @Override
  public List<ShardedClusterDto> list() {
    return Seq.seq(super.list())
        .map(this::setScripts)
        .map(this::setConfigMaps)
        .map(this::setInfo)
        .toList();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedClusterDto.class))})
  @Override
  public ShardedClusterDto create(ShardedClusterDto resource, @Nullable Boolean dryRun) {
    if (!Optional.ofNullable(dryRun).orElse(false)) {
      createOrUpdateScripts(resource);
    }
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedClusterDto.class))})
  @Override
  public ShardedClusterDto update(ShardedClusterDto resource, @Nullable Boolean dryRun) {
    if (!Optional.ofNullable(dryRun).orElse(false)) {
      createOrUpdateScripts(resource);
    }
    return super.update(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Override
  public void delete(ShardedClusterDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  ShardedClusterDto setInfo(ShardedClusterDto resource) {
    if (resource.getMetadata() == null) {
      return resource;
    }
    final String namespace = resource.getMetadata().getNamespace();
    final String clusterName = resource.getMetadata().getName();
    final ShardedClusterInfo info = new ShardedClusterInfo();

    var foundCluster = shardedClusterFinder.findByNameAndNamespace(clusterName, namespace);
    foundCluster
        .flatMap(cluster -> serviceFinder.findByNameAndNamespace(
            StackGresShardedClusterUtil.primaryCoordinatorServiceName(cluster), namespace))
        .ifPresent(service -> info.setPrimaryDns(StackGresUtil.getServiceDnsName(service)));
    foundCluster
        .flatMap(cluster -> serviceFinder.findByNameAndNamespace(
            StackGresShardedClusterUtil.anyCoordinatorServiceName(cluster), namespace))
        .ifPresent(service -> info.setReadsDns(StackGresUtil.getServiceDnsName(service)));
    foundCluster
        .flatMap(cluster -> serviceFinder.findByNameAndNamespace(
            StackGresShardedClusterUtil.primariesShardsServiceName(cluster), namespace))
        .ifPresent(service -> info.setPrimariesDns(StackGresUtil.getServiceDnsName(service)));

    info.setSuperuserUsername(SUPERUSER_USERNAME);
    info.setSuperuserSecretName(clusterName);
    info.setSuperuserUsernameKey(SUPERUSER_USERNAME_KEY);
    info.setSuperuserPasswordKey(SUPERUSER_PASSWORD_KEY);

    resource.setInfo(info);
    return resource;
  }

  ShardedClusterDto setScripts(ShardedClusterDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    getScriptEntries(resource)
        .forEach(managedScriptEntry -> {
          var script = scriptFinder
              .findByNameAndNamespace(managedScriptEntry.getSgScript(), namespace);
          managedScriptEntry.setScriptSpec(script
              .map(s -> scriptTransformer.toResource(s, List.of()))
              .map(ScriptDto::getSpec)
              .orElse(null));
        });
    return resource;
  }

  ShardedClusterDto setConfigMaps(ShardedClusterDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    Seq.of(Optional.ofNullable(resource.getSpec())
        .map(ShardedClusterSpec::getCoordinator)
        .map(ShardedClusterCoordinator::getManagedSql)
        .map(ClusterManagedSql::getScripts),
        Optional.ofNullable(resource.getSpec())
            .map(ShardedClusterSpec::getShards)
            .map(ShardedClusterShards::getManagedSql)
            .map(ClusterManagedSql::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .flatMap(managedScriptEntry -> Seq.seq(
            Optional.ofNullable(managedScriptEntry.getScriptSpec())
                .map(ScriptSpec::getScripts)
                .stream()
                .flatMap(List::stream))
            .zipWithIndex()
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null
            && t.v2.getScriptFrom().getConfigMapKeyRef() != null)
        .map(t -> extractConfigMapInfo(t.v1, t.v2, t.v3.intValue()))
        .filter(t -> t.v2.v3 != null)
        .grouped(t -> t.v2.v3.getName())
        .flatMap(t -> {
          var configMaps = configMapFinder
              .findByNameAndNamespace(t.v1, namespace)
              .map(ConfigMap::getData);
          return configMaps
              .map(s -> t.v2.map(tt -> Tuple.tuple(
                  s.get(tt.v2.v3.getKey()), tt.v2.v2)))
              .orElse(Seq.empty());
        })
        .forEach(t -> t.v2.accept(t.v1));
    return resource;
  }

  private void createOrUpdateScripts(ShardedClusterDto resource) {
    var scriptsToCreate = getScriptsToCreate(resource)
        .stream()
        .filter(t -> isNotDefaultScript(t.v2))
        .map(t -> t.concat(
            scriptFinder.findByNameAndNamespace(
                t.v2.getMetadata().getName(),
                t.v2.getMetadata().getNamespace())))
        .toList();
    var secretsToCreate = getSecretsToCreate(resource)
        .stream()
        .map(secret -> Tuple.tuple(secret,
            secretFinder.findByNameAndNamespace(
                secret.getMetadata().getName(),
                secret.getMetadata().getNamespace())))
        .toList();
    var configMapsToCreate = getConfigMapsToCreate(resource)
        .stream()
        .map(configMap -> Tuple.tuple(configMap,
            configMapFinder.findByNameAndNamespace(
                configMap.getMetadata().getName(),
                configMap.getMetadata().getNamespace())))
        .toList();

    configMapsToCreate.stream()
        .filter(t -> t.v2.isEmpty())
        .map(Tuple2::v1)
        .forEach(configMapWriter::create);
    configMapsToCreate.stream()
        .filter(t -> t.v2.isPresent())
        .map(Tuple2::v1)
        .forEach(configMapWriter::update);
    secretsToCreate.stream()
        .filter(t -> t.v2.isEmpty())
        .map(Tuple2::v1)
        .forEach(secretWriter::create);
    secretsToCreate.stream()
        .filter(t -> t.v2.isPresent())
        .map(Tuple2::v1)
        .forEach(secretWriter::update);
    scriptsToCreate.stream()
        .filter(t -> t.v3.isEmpty())
        .forEach(t -> addFieldPrefixOnScriptValidationError(t.v1, t.v2, scriptScheduler::create));
    scriptsToCreate.stream()
        .filter(t -> t.v3.isPresent())
        .forEach(t -> addFieldPrefixOnScriptValidationError(t.v1, t.v2, scriptScheduler::update));
  }

  private boolean isNotDefaultScript(StackGresScript script) {
    return !script.getMetadata().getName().endsWith(ManagedSqlUtil.DEFAULT_SCRIPT_NAME_SUFFIX);
  }

  private void addFieldPrefixOnScriptValidationError(Integer sgScriptIndex, StackGresScript script,
      Consumer<StackGresScript> consumer) {
    try {
      consumer.accept(script);
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 422
          && ex.getStatus() != null
          && ex.getStatus().getDetails() != null
          && ex.getStatus().getDetails().getName() != null
          && ex.getStatus().getDetails().getName().startsWith("spec.")) {
        final String fieldPrefix = "spec.managedSql.scripts[" + sgScriptIndex + "].scriptSpec.";
        ex.getStatus().getDetails().setName(
            fieldPrefix + ex.getStatus().getDetails().getName().substring("spec.".length()));
        Optional.ofNullable(ex.getStatus().getDetails().getCauses())
            .orElse(List.of())
            .stream()
            .filter(cause -> cause.getField().startsWith("spec."))
            .forEach(cause -> cause.setField(
                fieldPrefix + cause.getField().substring("spec.".length())));
      }
      throw ex;
    }
  }

  private List<Tuple2<Integer, StackGresScript>> getScriptsToCreate(ShardedClusterDto resource) {
    return getScriptEntries(resource)
        .zipWithIndex()
        .filter(t -> t.v1.getScriptSpec() != null)
        .map(t -> {
          StackGresScript script = new StackGresScript();
          script.setMetadata(new ObjectMeta());
          if (t.v1.getSgScript() == null) {
            t.v1.setSgScript(scriptResourceName(
                resource, t.v2.intValue()));
          }
          script.getMetadata().setName(t.v1.getSgScript());
          script.getMetadata().setNamespace(resource.getMetadata().getNamespace());
          script.setSpec(new StackGresScriptSpec());
          script.getSpec().setContinueOnError(
              t.v1.getScriptSpec().isContinueOnError());
          if (t.v1.getScriptSpec().getScripts() != null) {
            script.getSpec().setScripts(t.v1.getScriptSpec().getScripts().stream()
                .map(scriptTransformer::getCustomResourceScriptEntry)
                .toList());
          }
          return Tuple.tuple(t.v2.intValue(), script);
        })
        .toList();
  }

  private List<ConfigMap> getConfigMapsToCreate(ShardedClusterDto resource) {
    return getScriptEntries(resource)
        .flatMap(managedScriptEntry -> Seq.seq(
            Optional.ofNullable(managedScriptEntry.getScriptSpec())
                .map(ScriptSpec::getScripts)
                .stream()
                .flatMap(List::stream))
            .zipWithIndex()
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null)
        .filter(t -> t.v2.getScriptFrom().getConfigMapScript() != null)
        .map(t -> {
          ScriptFrom clusterScriptFrom = t.v2.getScriptFrom();
          final String configMapScript = clusterScriptFrom.getConfigMapScript();
          if (clusterScriptFrom.getConfigMapKeyRef() == null) {
            String configMapName = scriptEntryResourceName(t.v1, t.v3.intValue());
            ConfigMapKeySelector configMapKeyRef = new ConfigMapKeySelector();
            configMapKeyRef.setName(configMapName);
            configMapKeyRef.setKey(DEFAULT_SCRIPT_KEY);
            clusterScriptFrom.setConfigMapKeyRef(configMapKeyRef);
          }
          return new ConfigMapBuilder()
              .withNewMetadata()
              .withName(clusterScriptFrom.getConfigMapKeyRef().getName())
              .withNamespace(resource.getMetadata().getNamespace())
              .endMetadata()
              .withData(Map.of(clusterScriptFrom.getConfigMapKeyRef().getKey(),
                  configMapScript))
              .build();
        })
        .toList();
  }

  private List<Secret> getSecretsToCreate(ShardedClusterDto resource) {
    return getScriptEntries(resource)
        .flatMap(managedScriptEntry -> Seq.seq(
            Optional.ofNullable(managedScriptEntry.getScriptSpec())
                .map(ScriptSpec::getScripts)
                .stream()
                .flatMap(List::stream))
            .zipWithIndex()
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null)
        .filter(t -> t.v2.getScriptFrom().getSecretScript() != null)
        .map(t -> {
          ScriptFrom clusterScriptFrom = t.v2.getScriptFrom();
          final String secretScript = ResourceUtil
              .encodeSecret(clusterScriptFrom.getSecretScript());
          if (clusterScriptFrom.getSecretKeyRef() == null) {
            String secretName = scriptEntryResourceName(t.v1, t.v3.intValue());
            SecretKeySelector secretKeyRef = new SecretKeySelector();
            secretKeyRef.setName(secretName);
            secretKeyRef.setKey(DEFAULT_SCRIPT_KEY);
            clusterScriptFrom.setSecretKeyRef(secretKeyRef);
          }
          return new SecretBuilder()
              .withNewMetadata()
              .withName(clusterScriptFrom.getSecretKeyRef().getName())
              .withNamespace(resource.getMetadata().getNamespace())
              .endMetadata()
              .withData(Map.of(clusterScriptFrom.getSecretKeyRef().getKey(),
                  secretScript))
              .build();
        })
        .toList();
  }

  private
      Tuple2<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>>
      extractConfigMapInfo(
          ClusterManagedScriptEntry managedScriptEntry,
          ScriptEntry scriptEntry,
          int index) {
    return Tuple
        .<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>>tuple(
            scriptEntryResourceName(managedScriptEntry, index),
            Tuple
                .<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>tuple(
                    scriptEntry.getScriptFrom().getConfigMapScript(),
                    scriptEntry.getScriptFrom()::setConfigMapScript,
                    scriptEntry.getScriptFrom().getConfigMapKeyRef(),
                    scriptEntry.getScriptFrom()::setConfigMapKeyRef));
  }

  private String scriptResourceName(ShardedClusterDto cluster,
      int index) {
    return cluster.getMetadata().getName() + "-managed-sql-" + index;
  }

  private String scriptEntryResourceName(ClusterManagedScriptEntry managedScriptEntry, int index) {
    return ScriptResource.scriptEntryResourceName(managedScriptEntry.getSgScript(), index);
  }

  private Seq<ClusterManagedScriptEntry> getScriptEntries(ShardedClusterDto resource) {
    return Seq.of(Optional.ofNullable(resource.getSpec())
        .map(ShardedClusterSpec::getCoordinator)
        .map(ShardedClusterCoordinator::getManagedSql)
        .map(ClusterManagedSql::getScripts),
        Optional.ofNullable(resource.getSpec())
            .map(ShardedClusterSpec::getShards)
            .map(ShardedClusterShards::getManagedSql)
            .map(ClusterManagedSql::getScripts))
        .append(Optional.ofNullable(resource.getSpec())
            .map(ShardedClusterSpec::getShards)
            .map(ShardedClusterShards::getOverrides)
            .stream()
            .flatMap(List::stream)
            .map(override -> Optional.of(override)
                .map(ShardedClusterShard::getManagedSql)
                .map(ClusterManagedSql::getScripts)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream);
  }

  @Override
  protected void updateSpec(
      StackGresShardedCluster resourceToUpdate, StackGresShardedCluster resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}

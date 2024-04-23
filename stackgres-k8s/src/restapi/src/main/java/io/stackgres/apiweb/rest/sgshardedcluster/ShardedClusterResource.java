/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgshardedcluster;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractCustomResourceService;
import io.stackgres.apiweb.rest.sgscripts.ScriptResource;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

@Path("sgshardedclusters")
@RequestScoped
@Authenticated
@Tag(name = "sgshardedcluster")
@APIResponse(responseCode = "400", description = "Bad Request",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "401", description = "Unauthorized",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "403", description = "Forbidden",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "500", description = "Internal Server Error",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
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
  @Operation(summary = "List sgshardedclusters", description = """
      List sgshardedclusters.

      ### RBAC permissions required

      * sgshardedclusters list
      * configmaps get
      """)
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
  @Operation(summary = "Create a sgshardedclusters", description = """
      Create a sgshardedclusters.

      ### RBAC permissions required

      * sgshardedclusters create
      * secrets get, create, patch
      * configmaps get, create, patch
      """)
  @Override
  public ShardedClusterDto create(ShardedClusterDto resource, @Nullable Boolean dryRun) {
    createOrUpdateScripts(resource, Optional.ofNullable(dryRun).orElse(false));
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedClusterDto.class))})
  @Operation(summary = "Update a sgshardedclusters", description = """
      Update a sgshardedclusters.

      ### RBAC permissions required

      * sgshardedclusters patch
      * secrets get, create, patch
      * configmaps get, create, patch
      """)
  @Override
  public ShardedClusterDto update(ShardedClusterDto resource, @Nullable Boolean dryRun) {
    createOrUpdateScripts(resource, Optional.ofNullable(dryRun).orElse(false));
    return super.update(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a sgshardedclusters", description = """
      Delete a sgshardedclusters.

      ### RBAC permissions required

      * sgshardedclusters delete
      """)
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
    Seq
        .concat(
            getScriptEntriesForCoordinator(resource).stream(),
            getScriptEntriesForShards(resource).stream(),
            getScriptEntriesForShardsOverride(resource).stream().map(Tuple2::v2).flatMap(List::stream))
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
    setConfigMaps(
        resource,
        "coord",
        getScriptEntriesForCoordinator(resource));
    setConfigMaps(
        resource,
        "shards",
        getScriptEntriesForShards(resource));
    getScriptEntriesForShardsOverride(resource)
        .forEach(override -> setConfigMaps(
            resource,
            "shard" + override.v1.intValue(),
            override.v2));
    return resource;
  }

  void setConfigMaps(
      ShardedClusterDto resource,
      String suffix,
      List<ClusterManagedScriptEntry> scripts) {
    String namespace = resource.getMetadata().getNamespace();
    Seq.seq(scripts)
        .flatMap(managedScriptEntry -> Seq.seq(
            Optional.ofNullable(managedScriptEntry.getScriptSpec())
                .map(ScriptSpec::getScripts)
                .stream()
                .flatMap(List::stream))
            .zipWithIndex()
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null
            && t.v2.getScriptFrom().getConfigMapKeyRef() != null)
        .map(t -> extractConfigMapInfo(t.v1, t.v2, suffix, t.v3.intValue()))
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
  }

  void createOrUpdateScripts(ShardedClusterDto resource, boolean dryRun) {
    createOrUpdateScripts(
        resource,
        "coordinator",
        "coord",
        "spec.coordinator",
        Optional.ofNullable(resource.getSpec())
        .map(ShardedClusterSpec::getCoordinator)
        .map(ShardedClusterCoordinator::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .toList(),
        dryRun);
    createOrUpdateScripts(
        resource,
        "shards",
        "shards",
        "spec.shards",
        Optional.ofNullable(resource.getSpec())
        .map(ShardedClusterSpec::getShards)
        .map(ShardedClusterShards::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .toList(),
        dryRun);
    getScriptEntriesForShardsOverride(resource)
        .forEach(override -> createOrUpdateScripts(
            resource,
            "shards override " + (override.v1.intValue() + 1),
            "shard" + override.v1.intValue(),
            "spec.shards.overrides[" + override.v1.intValue() + "]",
            override.v2,
            dryRun));
  }

  void createOrUpdateScripts(
      ShardedClusterDto resource,
      String section,
      String prefix,
      String path,
      List<ClusterManagedScriptEntry> scripts,
      boolean dryRun) {
    var scriptsToCreate = getScriptsToCreate(resource, prefix, scripts)
        .stream()
        .filter(t -> isNotDefaultScript(t.v2))
        .map(t -> t.concat(
            scriptFinder.findByNameAndNamespace(
                t.v2.getMetadata().getName(),
                t.v2.getMetadata().getNamespace())))
        .toList();
    if (Seq.seq(scriptsToCreate)
        .grouped(t -> t.v2.getMetadata().getName())
        .anyMatch(t -> t.v2.count() > 1)) {
      throw new IllegalArgumentException(
          "script entries can not reference the same script more than once for " + section + "."
              + " Repeated SGScripts are: "
              + Seq.seq(scriptsToCreate)
              .grouped(t -> t.v2.getMetadata().getName())
              .map(t -> t.map2(Stream::toList))
              .filter(t -> t.v2.size() > 1)
              .map(Tuple2::v1)
              .toString(", "));
    }
    var secretsToCreate = Seq.seq(getSecretsToCreate(resource, prefix, scripts))
        .grouped(secret -> secret.getMetadata().getName())
        .flatMap(t -> t.v2.reduce(
            Optional.<Secret>empty(),
            (merged, secret) -> merged
            .or(() -> Optional.of(secret))
            .map(mergedSecret -> {
              mergedSecret.getData().putAll(secret.getData());
              return mergedSecret;
            }),
            (u, v) -> v)
            .stream())
        .map(secret -> Tuple.tuple(secret,
            secretFinder.findByNameAndNamespace(
                secret.getMetadata().getName(),
                secret.getMetadata().getNamespace())))
        .toList();
    var configMapsToCreate = Seq.seq(getConfigMapsToCreate(resource, prefix, scripts))
        .grouped(configMap -> configMap.getMetadata().getName())
        .flatMap(t -> t.v2.reduce(
            Optional.<ConfigMap>empty(),
            (merged, configMap) -> merged
            .or(() -> Optional.of(configMap))
            .map(mergedConfigMap -> {
              mergedConfigMap.getData().putAll(configMap.getData());
              return mergedConfigMap;
            }),
            (u, v) -> v)
            .stream())
        .map(configMap -> Tuple.tuple(configMap,
            configMapFinder.findByNameAndNamespace(
                configMap.getMetadata().getName(),
                configMap.getMetadata().getNamespace())))
        .toList();

    configMapsToCreate.stream()
        .filter(t -> t.v2.isEmpty())
        .map(Tuple2::v1)
        .forEach(configMap -> configMapWriter.create(configMap, dryRun));
    configMapsToCreate.stream()
        .filter(t -> t.v2.isPresent())
        .map(Tuple2::v1)
        .forEach(configMap -> configMapWriter.update(configMap, dryRun));
    secretsToCreate.stream()
        .filter(t -> t.v2.isEmpty())
        .map(Tuple2::v1)
        .forEach(secret -> secretWriter.create(secret, dryRun));
    secretsToCreate.stream()
        .filter(t -> t.v2.isPresent())
        .map(Tuple2::v1)
        .forEach(secret -> secretWriter.update(secret, dryRun));
    scriptsToCreate.stream()
        .filter(t -> t.v3.isEmpty())
        .forEach(t -> addFieldPrefixOnScriptValidationError(
            path, t.v1, t.v2, script -> scriptScheduler.create(script, dryRun)));
    scriptsToCreate.stream()
        .filter(t -> t.v3.isPresent())
        .forEach(t -> addFieldPrefixOnScriptValidationError(
            path, t.v1, t.v2, script -> scriptScheduler.update(script, dryRun)));
  }

  boolean isNotDefaultScript(StackGresScript script) {
    return !script.getMetadata().getName().endsWith(ManagedSqlUtil.DEFAULT_SCRIPT_NAME_SUFFIX);
  }

  void addFieldPrefixOnScriptValidationError(
      String path,
      Integer sgScriptIndex,
      StackGresScript script,
      Consumer<StackGresScript> consumer) {
    try {
      consumer.accept(script);
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 422
          && ex.getStatus() != null
          && ex.getStatus().getDetails() != null
          && ex.getStatus().getDetails().getName() != null
          && ex.getStatus().getDetails().getName().startsWith("spec.")) {
        final String fieldPrefix = path + ".managedSql.scripts[" + sgScriptIndex + "].scriptSpec.";
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

  List<Tuple2<Integer, StackGresScript>> getScriptsToCreate(
      ShardedClusterDto resource,
      String prefix,
      List<ClusterManagedScriptEntry> scripts) {
    return Seq.seq(scripts)
        .zipWithIndex()
        .filter(t -> t.v1.getScriptSpec() != null)
        .map(t -> {
          StackGresScript script = new StackGresScript();
          script.setMetadata(new ObjectMeta());
          if (t.v1.getSgScript() == null) {
            t.v1.setSgScript(scriptResourceName(
                resource, prefix, t.v2.intValue()));
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

  List<ConfigMap> getConfigMapsToCreate(
      ShardedClusterDto resource,
      String prefix,
      List<ClusterManagedScriptEntry> scripts) {
    return Seq.seq(scripts)
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
            String configMapName = scriptEntryResourceName(t.v1, prefix, t.v3.intValue());
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

  List<Secret> getSecretsToCreate(
      ShardedClusterDto resource,
      String prefix,
      List<ClusterManagedScriptEntry> scripts) {
    return Seq.seq(scripts)
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
            String secretName = scriptEntryResourceName(t.v1, prefix, t.v3.intValue());
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
          String suffix,
          int index) {
    return Tuple
        .<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>>tuple(
            scriptEntryResourceName(managedScriptEntry, suffix, index),
            Tuple
                .<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>tuple(
                    scriptEntry.getScriptFrom().getConfigMapScript(),
                    scriptEntry.getScriptFrom()::setConfigMapScript,
                    scriptEntry.getScriptFrom().getConfigMapKeyRef(),
                    scriptEntry.getScriptFrom()::setConfigMapKeyRef));
  }

  String scriptResourceName(
      ShardedClusterDto cluster,
      String suffix,
      int index) {
    return cluster.getMetadata().getName() + "-managed-sql-" + suffix + "-" + index;
  }

  String scriptEntryResourceName(ClusterManagedScriptEntry managedScriptEntry, String suffix, int index) {
    return ScriptResource.scriptEntryResourceName(managedScriptEntry.getSgScript(), suffix, index);
  }

  List<ClusterManagedScriptEntry> getScriptEntriesForCoordinator(ShardedClusterDto resource) {
    return Seq
        .of(Optional.ofNullable(resource.getSpec())
            .map(ShardedClusterSpec::getCoordinator)
            .map(ShardedClusterCoordinator::getManagedSql)
            .map(ClusterManagedSql::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .toList();
  }

  List<ClusterManagedScriptEntry> getScriptEntriesForShards(ShardedClusterDto resource) {
    return Seq
        .of(Optional.ofNullable(resource.getSpec())
            .map(ShardedClusterSpec::getShards)
            .map(ShardedClusterShards::getManagedSql)
            .map(ClusterManagedSql::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .toList();
  }

  List<Tuple2<Integer, List<ClusterManagedScriptEntry>>> getScriptEntriesForShardsOverride(ShardedClusterDto resource) {
    return Seq
        .seq(Optional.ofNullable(resource.getSpec())
            .map(ShardedClusterSpec::getShards)
            .map(ShardedClusterShards::getOverrides))
        .flatMap(List::stream)
        .zipWithIndex()
        .map(override -> Tuple.tuple(
            override.v2.intValue(),
            Optional.of(override.v1)
            .map(ShardedClusterShard::getManagedSql)
            .map(ClusterManagedSql::getScripts)
            .orElse(List.of())))
        .toList();
  }

  @Override
  protected void updateSpec(
      StackGresShardedCluster resourceToUpdate, StackGresShardedCluster resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}

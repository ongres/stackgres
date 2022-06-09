/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.SUPERUSER_USER_NAME;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterInfoDto;
import io.stackgres.apiweb.dto.cluster.ClusterManagedScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterManagedSql;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.transformer.ScriptTransformer;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

@Path("sgclusters")
@RequestScoped
@Authenticated
public class ClusterResource
    extends AbstractRestService<ClusterDto, StackGresCluster> {

  public static final String DEFAULT_SCRIPT_KEY = ScriptResource.DEFAULT_SCRIPT_KEY;

  private final CustomResourceScanner<ClusterDto> clusterScanner;
  private final CustomResourceScheduler<StackGresScript> scriptScheduler;
  private final ResourceWriter<Secret> secretWriter;
  private final ResourceWriter<ConfigMap> configMapWriter;
  private final CustomResourceFinder<StackGresScript> scriptFinder;
  private final ScriptTransformer scriptTransformer;
  private final ResourceFinder<Secret> secretFinder;
  private final ResourceFinder<ConfigMap> configMapFinder;
  private final ResourceFinder<Service> serviceFinder;

  @Inject
  public ClusterResource(
      CustomResourceScanner<ClusterDto> clusterScanner,
      CustomResourceScheduler<StackGresScript> scriptScheduler,
      ResourceWriter<Secret> secretWriter,
      ResourceWriter<ConfigMap> configMapWriter,
      CustomResourceFinder<StackGresScript> scriptFinder,
      ScriptTransformer scriptTransformer,
      ResourceFinder<Secret> secretFinder,
      ResourceFinder<ConfigMap> configMapFinder,
      ResourceFinder<Service> serviceFinder) {
    this.clusterScanner = clusterScanner;
    this.scriptScheduler = scriptScheduler;
    this.secretWriter = secretWriter;
    this.configMapWriter = configMapWriter;
    this.scriptFinder = scriptFinder;
    this.scriptTransformer = scriptTransformer;
    this.secretFinder = secretFinder;
    this.configMapFinder = configMapFinder;
    this.serviceFinder = serviceFinder;
  }

  public ClusterResource() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.clusterScanner = null;
    this.scriptScheduler = null;
    this.secretWriter = null;
    this.configMapWriter = null;
    this.scriptFinder = null;
    this.scriptTransformer = null;
    this.secretFinder = null;
    this.configMapFinder = null;
    this.serviceFinder = null;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = ClusterDto.class)))})
      })
  @Override
  public List<ClusterDto> list() {
    return Seq.seq(clusterScanner.getResources())
        .map(this::setScripts)
        .map(this::setConfigMaps)
        .map(this::setInfo)
        .toList();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void create(ClusterDto resource) {
    createOrUpdateScripts(resource);
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void update(ClusterDto resource) {
    createOrUpdateScripts(resource);
    super.update(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(ClusterDto resource) {
    super.delete(resource);
  }

  ClusterDto setInfo(ClusterDto resource) {
    if (resource.getMetadata() == null) {
      return resource;
    }
    final String namespace = resource.getMetadata().getNamespace();
    final String clusterName = resource.getMetadata().getName();
    final ClusterInfoDto info = new ClusterInfoDto();

    serviceFinder.findByNameAndNamespace(PatroniUtil.readWriteName(clusterName), namespace)
        .ifPresent(service -> info.setPrimaryDns(StackGresUtil.getServiceDnsName(service)));
    serviceFinder.findByNameAndNamespace(PatroniUtil.readOnlyName(clusterName), namespace)
        .ifPresent(service -> info.setReplicasDns(StackGresUtil.getServiceDnsName(service)));

    info.setSuperuserUsername(SUPERUSER_USER_NAME);
    info.setSuperuserSecretName(clusterName);
    info.setSuperuserPasswordKey(SUPERUSER_PASSWORD_KEY);

    resource.setInfo(info);
    return resource;
  }

  ClusterDto setScripts(ClusterDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    Seq.of(Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .forEach(managedScriptEntry -> {
          var script = scriptFinder
              .findByNameAndNamespace(managedScriptEntry.getSgScript(), namespace);
          managedScriptEntry.setScriptSpec(script
              .map(scriptTransformer::toDto)
              .map(ScriptDto::getSpec)
              .orElse(null));
        });
    return resource;
  }

  ClusterDto setConfigMaps(ClusterDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    Seq.of(Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .flatMap(managedScriptEntry -> Optional.ofNullable(managedScriptEntry.getScriptSpec())
            .map(ScriptSpec::getScripts)
            .stream()
            .flatMap(List::stream)
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null
            && t.v2.getScriptFrom().getConfigMapKeyRef() != null)
        .map(t -> extractConfigMapInfo(t.v1, t.v2))
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

  private void createOrUpdateScripts(ClusterDto resource) {
    var scriptsToCreate = getScriptsToCreate(resource)
        .stream()
        .map(script -> Tuple.tuple(script,
            scriptFinder.findByNameAndNamespace(
                script.getMetadata().getName(),
                script.getMetadata().getNamespace())))
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
        .filter(t -> t.v2.isEmpty())
        .map(Tuple2::v1)
        .forEach(scriptScheduler::create);
    scriptsToCreate.stream()
        .filter(t -> t.v2.isPresent())
        .map(Tuple2::v1)
        .forEach(scriptScheduler::update);
  }

  private List<StackGresScript> getScriptsToCreate(ClusterDto resource) {
    return Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream))
        .filter(managedScriptEntry -> managedScriptEntry.getScriptSpec() != null)
        .map(managedScriptEntry -> {
          StackGresScript script = new StackGresScript();
          script.setMetadata(new ObjectMeta());
          if (managedScriptEntry.getSgScript() == null) {
            managedScriptEntry.setSgScript(
                scriptResourceName(resource, managedScriptEntry));
          }
          script.getMetadata().setName(managedScriptEntry.getSgScript());
          script.getMetadata().setNamespace(resource.getMetadata().getNamespace());
          script.setSpec(new StackGresScriptSpec());
          script.getSpec().setContinueOnError(
              managedScriptEntry.getScriptSpec().isContinueOnError());
          if (managedScriptEntry.getScriptSpec().getScripts() != null) {
            script.getSpec().setScripts(managedScriptEntry.getScriptSpec().getScripts().stream()
                .map(scriptTransformer::getCustomResourceScriptEntry)
                .collect(ImmutableList.toImmutableList()));
          }
          return script;
        })
        .toList();
  }

  private List<ConfigMap> getConfigMapsToCreate(ClusterDto resource) {
    return Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream))
        .flatMap(managedScriptEntry -> Optional.ofNullable(managedScriptEntry.getScriptSpec())
            .map(ScriptSpec::getScripts)
            .stream()
            .flatMap(List::stream)
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null)
        .filter(t -> t.v2.getScriptFrom().getConfigMapScript() != null)
        .map(t -> {
          ScriptFrom clusterScriptFrom = t.v2.getScriptFrom();
          final String configMapScript = clusterScriptFrom.getConfigMapScript();
          if (clusterScriptFrom.getConfigMapKeyRef() == null) {
            String configMapName = scriptEntryResourceName(t.v1, t.v2);
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

  private List<Secret> getSecretsToCreate(ClusterDto resource) {
    return Seq.seq(Optional
        .ofNullable(resource.getSpec())
        .map(ClusterSpec::getManagedSql)
        .map(ClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream))
        .flatMap(managedScriptEntry -> Optional.ofNullable(managedScriptEntry.getScriptSpec())
            .map(ScriptSpec::getScripts)
            .stream()
            .flatMap(List::stream)
            .map(Tuple.tuple(managedScriptEntry)::concat))
        .filter(t -> t.v2.getScriptFrom() != null)
        .filter(t -> t.v2.getScriptFrom().getSecretScript() != null)
        .map(t -> {
          ScriptFrom clusterScriptFrom = t.v2.getScriptFrom();
          final String secretScript = ResourceUtil
              .encodeSecret(clusterScriptFrom.getSecretScript());
          if (clusterScriptFrom.getSecretKeyRef() == null) {
            String secretName = scriptEntryResourceName(t.v1, t.v2);
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

  private Tuple2<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector,
      Consumer<ConfigMapKeySelector>>> extractConfigMapInfo(
      ClusterManagedScriptEntry managedScriptEntry,
      ScriptEntry scriptEntry) {
    return Tuple.<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector,
        Consumer<ConfigMapKeySelector>>>tuple(
        scriptEntryResourceName(managedScriptEntry, scriptEntry),
        Tuple.<String, Consumer<String>, ConfigMapKeySelector,
            Consumer<ConfigMapKeySelector>>tuple(
            scriptEntry.getScriptFrom().getConfigMapScript(),
            scriptEntry.getScriptFrom()::setConfigMapScript,
            scriptEntry.getScriptFrom().getConfigMapKeyRef(),
            scriptEntry.getScriptFrom()::setConfigMapKeyRef));
  }

  private String scriptResourceName(ClusterDto cluster,
      ClusterManagedScriptEntry managedScriptEntry) {
    return cluster.getMetadata().getName() + "-managed-sql-" + managedScriptEntry.getId();
  }

  private String scriptEntryResourceName(ClusterManagedScriptEntry managedScriptEntry,
      ScriptEntry scriptEntry) {
    return ScriptResource.scriptEntryResourceName(managedScriptEntry.getSgScript(), scriptEntry);
  }

  @Override
  protected void updateSpec(StackGresCluster resourceToUpdate, StackGresCluster resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}

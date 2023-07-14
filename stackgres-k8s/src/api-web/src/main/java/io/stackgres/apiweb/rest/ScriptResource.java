/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.common.resource.ResourceWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

@Path("sgscripts")
@RequestScoped
@Authenticated
public class ScriptResource
    extends AbstractRestServiceDependency<ScriptDto, StackGresScript> {

  public static final String DEFAULT_SCRIPT_KEY = "script";

  private final ResourceWriter<Secret> secretWriter;
  private final ResourceWriter<ConfigMap> configMapWriter;
  private final ResourceFinder<ConfigMap> configMapFinder;

  @Inject
  public ScriptResource(ResourceWriter<Secret> secretWriter,
      ResourceWriter<ConfigMap> configMapWriter,
      ResourceFinder<ConfigMap> configMapFinder) {
    super();
    this.secretWriter = secretWriter;
    this.configMapWriter = configMapWriter;
    this.configMapFinder = configMapFinder;
  }

  @Override
  public boolean belongsToCluster(StackGresScript resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .map(StackGresClusterManagedScriptEntry::getSgScript)
        .anyMatch(sgScript -> Objects.equals(sgScript, resource.getMetadata().getName()));
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = ScriptDto.class))) })
      })
  @Override
  public List<ScriptDto> list() {
    return Seq.seq(super.list())
        .map(this::setConfigMaps)
        .toList();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void create(ScriptDto resource) {
    List<Secret> secretsToCreate = getSecretsToCreate(resource);
    List<ConfigMap> configMapsToCreate = getConfigMapsToCreate(resource);

    secretsToCreate.forEach(secretWriter::create);
    configMapsToCreate.forEach(configMapWriter::create);
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(ScriptDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void update(ScriptDto resource) {
    List<Secret> secretsToCreate = getSecretsToCreate(resource);
    List<ConfigMap> configMapsToCreate = getConfigMapsToCreate(resource);

    secretsToCreate.forEach(secretWriter::create);
    configMapsToCreate.forEach(configMapWriter::create);
    super.update(resource);
  }

  ScriptDto setConfigMaps(ScriptDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream))
        .zipWithIndex()
        .filter(t -> t.v1.getScriptFrom() != null
            && t.v1.getScriptFrom().getConfigMapKeyRef() != null)
        .map(t -> extractConfigMapInfo(resource, t.v1, t.v2.intValue()))
        .filter(t -> t.v2.v3 != null)
        .grouped(t -> t.v2.v3.getName())
        .flatMap(t -> {
          Optional<Map<String, String>> configMaps = configMapFinder
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

  private List<ConfigMap> getConfigMapsToCreate(ScriptDto resource) {
    return Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream))
        .zipWithIndex()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getConfigMapScript() != null)
        .map(t -> {
          ScriptFrom clusterScriptFrom = t.v1.getScriptFrom();
          final String configMapScript = clusterScriptFrom.getConfigMapScript();
          if (clusterScriptFrom.getConfigMapKeyRef() == null) {
            String configMapName = scriptEntryResourceName(resource, t.v2.intValue());
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

  private List<Secret> getSecretsToCreate(ScriptDto resource) {
    return Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream))
        .zipWithIndex()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getSecretScript() != null)
        .map(t -> {
          ScriptFrom clusterScriptFrom = t.v1.getScriptFrom();
          final String secretScript = ResourceUtil
              .encodeSecret(clusterScriptFrom.getSecretScript());
          if (clusterScriptFrom.getSecretKeyRef() == null) {
            String secretName = scriptEntryResourceName(resource, t.v2.intValue());
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
      ScriptDto resource, ScriptEntry scriptEntry, int index) {
    return Tuple.<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector,
        Consumer<ConfigMapKeySelector>>>tuple(
        scriptEntryResourceName(resource, index),
        Tuple.<String, Consumer<String>, ConfigMapKeySelector,
            Consumer<ConfigMapKeySelector>>tuple(
            scriptEntry.getScriptFrom().getConfigMapScript(),
            scriptEntry.getScriptFrom()::setConfigMapScript,
            scriptEntry.getScriptFrom().getConfigMapKeyRef(),
            scriptEntry.getScriptFrom()::setConfigMapKeyRef));
  }

  private String scriptEntryResourceName(ScriptDto resource, int index) {
    return scriptEntryResourceName(resource.getMetadata().getName(), index);
  }

  public static String scriptEntryResourceName(String scriptName, int index) {
    return scriptName + "-" + index;
  }

  @Override
  protected void updateSpec(StackGresScript resourceToUpdate, StackGresScript resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgscripts;

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
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractCustomResourceServiceDependency;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgscript.StackGresScript;
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

@Path("sgscripts")
@RequestScoped
@Authenticated
@Tag(name = "sgscripts")
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
public class ScriptResource
    extends AbstractCustomResourceServiceDependency<ScriptDto, StackGresScript> {

  public static final String DEFAULT_SCRIPT_KEY = "script";

  private final ResourceWriter<Secret> secretWriter;
  private final ResourceFinder<Secret> secretFinder;
  private final ResourceWriter<ConfigMap> configMapWriter;
  private final ResourceFinder<ConfigMap> configMapFinder;

  @Inject
  public ScriptResource(ResourceWriter<Secret> secretWriter,
      ResourceFinder<Secret> secretFinder,
      ResourceWriter<ConfigMap> configMapWriter,
      ResourceFinder<ConfigMap> configMapFinder) {
    super();
    this.secretWriter = secretWriter;
    this.secretFinder = secretFinder;
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

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ScriptDto.class))})
  @Operation(summary = "List sgscripts", description = """
      List sgscripts.

      ### RBAC permissions required

      * sgscripts list
      * configmaps get
      """)
  @Override
  public List<ScriptDto> list() {
    return Seq.seq(super.list())
        .map(this::setConfigMaps)
        .toList();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ScriptDto.class))})
  @Operation(summary = "Create a sgscripts", description = """
      Create a sgscripts.

      ### RBAC permissions required

      * sgscripts create
      * secrets get, create, patch
      * configmaps get, create, patch
      """)
  @Override
  public ScriptDto create(ScriptDto resource, @Nullable Boolean dryRun) {
    createOrUpdateSecret(resource, Optional.ofNullable(dryRun).orElse(false));
    createOrUpdateConfigMap(resource, Optional.ofNullable(dryRun).orElse(false));
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a sgscripts", description = """
      Delete a sgscripts.

      ### RBAC permissions required

      * sgscripts delete
      """)
  @Override
  public void delete(ScriptDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ScriptDto.class))})
  @Operation(summary = "Update a sgscripts", description = """
      Update a sgscripts.

      ### RBAC permissions required

      * sgscripts patch
      * secrets get, create, patch
      * configmaps get, create, patch
      """)
  @Override
  public ScriptDto update(ScriptDto resource, @Nullable Boolean dryRun) {
    createOrUpdateSecret(resource, Optional.ofNullable(dryRun).orElse(false));
    createOrUpdateConfigMap(resource, Optional.ofNullable(dryRun).orElse(false));
    return super.update(resource, dryRun);
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

  private void createOrUpdateConfigMap(ScriptDto resource, boolean dryRun) {
    String namespace = resource.getMetadata().getNamespace();
    Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream))
        .zipWithIndex()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getConfigMapScript() != null)
        .forEach(t -> {
          ScriptFrom clusterScriptFrom = t.v1.getScriptFrom();
          final String configMapScript = clusterScriptFrom.getConfigMapScript();
          if (clusterScriptFrom.getConfigMapKeyRef() == null) {
            String name = scriptEntryResourceName(resource, t.v2.intValue());
            ConfigMapKeySelector configMapKeyRef = new ConfigMapKeySelector();
            configMapKeyRef.setName(name);
            configMapKeyRef.setKey(DEFAULT_SCRIPT_KEY);
            clusterScriptFrom.setConfigMapKeyRef(configMapKeyRef);
          }
          String name = clusterScriptFrom.getConfigMapKeyRef().getName();
          var configMaps = Map.of(
              clusterScriptFrom.getConfigMapKeyRef().getKey(),
              configMapScript);
          configMapFinder.findByNameAndNamespace(name, namespace)
              .map(configMap -> {
                configMap.setData(configMaps);
                configMapWriter.update(configMap, dryRun);
                return configMap;
              })
              .orElseGet(() -> {
                configMapWriter.create(new ConfigMapBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace)
                    .withName(name)
                    .withOwnerReferences(finder.findByNameAndNamespace(
                        resource.getMetadata().getName(), resource.getMetadata().getNamespace())
                        .map(ResourceUtil::getOwnerReference)
                        .map(List::of)
                        .orElse(List.of()))
                    .endMetadata()
                    .withData(configMaps)
                    .build(), dryRun);
                return null;
              });
        });
  }

  private void createOrUpdateSecret(ScriptDto resource, boolean dryRun) {
    String namespace = resource.getMetadata().getNamespace();
    Seq.seq(Optional.ofNullable(resource.getSpec())
        .map(ScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream))
        .zipWithIndex()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getSecretScript() != null)
        .forEach(t -> {
          ScriptFrom clusterScriptFrom = t.v1.getScriptFrom();
          final String secretScript = ResourceUtil
              .encodeSecret(clusterScriptFrom.getSecretScript());
          if (clusterScriptFrom.getSecretKeyRef() == null) {
            String name = scriptEntryResourceName(resource, t.v2.intValue());
            SecretKeySelector secretKeyRef = new SecretKeySelector();
            secretKeyRef.setName(name);
            secretKeyRef.setKey(DEFAULT_SCRIPT_KEY);
            clusterScriptFrom.setSecretKeyRef(secretKeyRef);
          }
          String name = clusterScriptFrom.getSecretKeyRef().getName();
          var secrets = Map.of(
              clusterScriptFrom.getSecretKeyRef().getKey(),
              secretScript);
          secretFinder.findByNameAndNamespace(name, namespace)
              .map(secret -> {
                secret.setData(secrets);
                secretWriter.update(secret, dryRun);
                return secret;
              })
              .orElseGet(() -> {
                secretWriter.create(new SecretBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace)
                    .withName(name)
                    .withOwnerReferences(finder.findByNameAndNamespace(
                        resource.getMetadata().getName(), resource.getMetadata().getNamespace())
                        .map(ResourceUtil::getOwnerReference)
                        .map(List::of)
                        .orElse(List.of()))
                    .endMetadata()
                    .withData(secrets)
                    .build(), dryRun);
                return null;
              });
        });
  }

  private
      Tuple2<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>>
      extractConfigMapInfo(
          ScriptDto resource, ScriptEntry scriptEntry, int index) {
    return Tuple
        .<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>>tuple(
            scriptEntryResourceName(resource, index),
            Tuple
                .<String, Consumer<String>, ConfigMapKeySelector, Consumer<ConfigMapKeySelector>>tuple(
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

  public static String scriptEntryResourceName(String scriptName, String suffix, int index) {
    return scriptName + "-" + suffix + "-" + index;
  }

  @Override
  protected void updateSpec(StackGresScript resourceToUpdate, StackGresScript resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}

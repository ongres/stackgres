/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.resource.ResourceTransactionHandler;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

@Path("sgscripts")
@RequestScoped
@Authenticated
public class ScriptResource
    extends AbstractRestService<ScriptDto, StackGresScript> {

  private static final String DEFAULT_SCRIPT_KEY = "script";

  private final ResourceTransactionHandler<Secret> secretTransactionHandler;
  private final ResourceTransactionHandler<ConfigMap> configMapTransactionHandler;
  private final ResourceFinder<ConfigMap> configMapFinder;

  @Inject
  public ScriptResource(ResourceTransactionHandler<Secret> secretTransactionHandler,
      ResourceTransactionHandler<ConfigMap> configMapTransactionHandler,
      ResourceFinder<ConfigMap> configMapFinder) {
    super();
    this.secretTransactionHandler = secretTransactionHandler;
    this.configMapTransactionHandler = configMapTransactionHandler;
    this.configMapFinder = configMapFinder;
  }

  public ScriptResource() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.secretTransactionHandler = null;
    this.configMapTransactionHandler = null;
    this.configMapFinder = null;
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
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void create(ScriptDto resource) {
    Deque<Secret> secretsToCreate = getSecretsToCreate(resource);
    Deque<ConfigMap> configMapsToCreate = getConfigMapsToCreate(resource);

    createSecrets(secretsToCreate,
        () -> createConfigMaps(configMapsToCreate,
            () -> super.create(resource)));
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
    super.update(resource);
  }

  ScriptDto setConfigMaps(ScriptDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    Seq.of(Optional.ofNullable(resource.getSpec())
        .map(ScriptSpec::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(Collection::stream)
        .zipWithIndex()
        .filter(t -> t.v1.getScriptFrom() != null
            && t.v1.getScriptFrom().getConfigMapKeyRef() != null)
        .map(script -> extractConfigMapInfo(resource, script))
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

  private void createSecrets(Deque<Secret> secrets, Runnable transaction) {
    Secret secret = secrets.poll();
    if (secret != null) {
      secretTransactionHandler.create(secret, () -> createSecrets(secrets, transaction));
    } else {
      transaction.run();
    }
  }

  private void createConfigMaps(Deque<ConfigMap> configMaps, Runnable transaction) {
    ConfigMap configMap = configMaps.poll();
    if (configMap != null) {
      configMapTransactionHandler.create(configMap,
          () -> createConfigMaps(configMaps, transaction));
    } else {
      transaction.run();
    }
  }

  private Deque<ConfigMap> getConfigMapsToCreate(ScriptDto resource) {
    return Optional.ofNullable(resource.getSpec())
        .map(ScriptSpec::getScripts)
        .map(clusterScriptEntries -> Seq.zipWithIndex(clusterScriptEntries)
            .filter(entry -> entry.v1.getScriptFrom() != null)
            .filter(entry -> entry.v1.getScriptFrom().getConfigMapScript() != null)
            .map(tuple -> {
              ScriptFrom clusterScriptFrom = tuple.v1.getScriptFrom();
              ConfigMapKeySelector configMapKeyRef = clusterScriptFrom.getConfigMapKeyRef();
              final String configMapScript = clusterScriptFrom.getConfigMapScript();
              if (configMapKeyRef != null) {
                return new ConfigMapBuilder()
                    .withNewMetadata()
                    .withName(configMapKeyRef.getName())
                    .withNamespace(resource.getMetadata().getNamespace())
                    .endMetadata()
                    .withData(ImmutableMap.of(configMapKeyRef.getKey(),
                        configMapScript))
                    .build();
              } else {
                final String configMapName = scriptResourceName(resource, tuple);
                configMapKeyRef = new ConfigMapKeySelector();
                configMapKeyRef.setName(configMapName);
                configMapKeyRef.setKey(DEFAULT_SCRIPT_KEY);
                clusterScriptFrom.setConfigMapKeyRef(configMapKeyRef);
                return new ConfigMapBuilder()
                    .withNewMetadata()
                    .withName(configMapName)
                    .withNamespace(resource.getMetadata().getNamespace())
                    .endMetadata()
                    .withData(ImmutableMap.of(DEFAULT_SCRIPT_KEY,
                        configMapScript))
                    .build();
              }
            }).collect(Collectors.toCollection(ArrayDeque::new)))
        .orElse(new ArrayDeque<>());
  }

  private Deque<Secret> getSecretsToCreate(ScriptDto resource) {
    return Optional
        .ofNullable(resource.getSpec())
        .map(ScriptSpec::getScripts)
        .map(clusterScriptEntries -> Seq.zipWithIndex(clusterScriptEntries)
            .filter(entry -> entry.v1.getScriptFrom() != null)
            .filter(entry -> entry.v1.getScriptFrom().getSecretScript() != null)
            .map(tuple -> {
              ScriptFrom clusterScriptFrom = tuple.v1.getScriptFrom();
              SecretKeySelector secretKeyRef = clusterScriptFrom.getSecretKeyRef();

              final String secretScript = ResourceUtil
                  .encodeSecret(clusterScriptFrom.getSecretScript());

              if (secretKeyRef != null) {
                return new SecretBuilder()
                    .withNewMetadata()
                    .withName(clusterScriptFrom.getSecretKeyRef().getName())
                    .withNamespace(resource.getMetadata().getNamespace())
                    .endMetadata()
                    .withData(ImmutableMap.of(clusterScriptFrom.getSecretKeyRef().getKey(),
                        secretScript))
                    .build();
              } else {
                final String secretName = scriptResourceName(resource, tuple);
                secretKeyRef = new SecretKeySelector();
                secretKeyRef.setName(secretName);
                secretKeyRef.setKey(DEFAULT_SCRIPT_KEY);
                clusterScriptFrom.setSecretKeyRef(secretKeyRef);
                return new SecretBuilder()
                    .withNewMetadata()
                    .withName(secretName)
                    .withNamespace(resource.getMetadata().getNamespace())
                    .endMetadata()
                    .withData(ImmutableMap.of(DEFAULT_SCRIPT_KEY,
                        secretScript))
                    .build();
              }
            }).collect(Collectors.toCollection(ArrayDeque::new)))
        .orElse(new ArrayDeque<>());
  }

  private Tuple2<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector,
      Consumer<ConfigMapKeySelector>>> extractConfigMapInfo(
      ScriptDto resource, Tuple2<ScriptEntry, Long> script) {
    return Tuple.<String, Tuple4<String, Consumer<String>, ConfigMapKeySelector,
        Consumer<ConfigMapKeySelector>>>tuple(
        scriptResourceName(resource, script),
        Tuple.<String, Consumer<String>, ConfigMapKeySelector,
            Consumer<ConfigMapKeySelector>>tuple(
            script.v1.getScriptFrom().getConfigMapScript(),
            script.v1.getScriptFrom()::setConfigMapScript,
            script.v1.getScriptFrom().getConfigMapKeyRef(),
            script.v1.getScriptFrom()::setConfigMapKeyRef));
  }

  private String scriptResourceName(ScriptDto resource, Tuple2<ScriptEntry, Long> tuple) {
    return tuple.v1.getName() != null
        ? tuple.v1.getName()
        : resource.getMetadata().getName() + "-script-" + tuple.v2;
  }

}

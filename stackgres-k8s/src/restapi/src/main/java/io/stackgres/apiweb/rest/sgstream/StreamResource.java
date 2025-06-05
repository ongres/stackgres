/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgstream;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.stream.StreamDto;
import io.stackgres.apiweb.dto.stream.StreamSource;
import io.stackgres.apiweb.dto.stream.StreamSpec;
import io.stackgres.apiweb.dto.stream.StreamTarget;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractCustomResourceService;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgstream.StackGresStream;
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

@Path("sgstreams")
@RequestScoped
@Authenticated
@Tag(name = "sgstream")
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
public class StreamResource
    extends AbstractCustomResourceService<StreamDto, StackGresStream> {

  public static final String DEFAULT_SOURCE_USERNAME_KEY = "source-username";
  public static final String DEFAULT_SOURCE_PASSWORD_KEY = "source-password";
  public static final String DEFAULT_TARGET_USERNAME_KEY = "target-username";
  public static final String DEFAULT_TARGET_PASSWORD_KEY = "target-password";

  private final ResourceWriter<Secret> secretWriter;
  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public StreamResource(
      ResourceWriter<Secret> secretWriter,
      ResourceFinder<Secret> secretFinder) {
    this.secretWriter = secretWriter;
    this.secretFinder = secretFinder;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(type = SchemaType.ARRAY, implementation = StreamDto.class))})
  @Operation(summary = "List sgstreams", description = """
      List sgstreams and read values from the referenced secrets and configmaps.

      ### RBAC permissions required

      * sgstreams list
      """)
  @Override
  public List<StreamDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = StreamDto.class))})
  @Operation(summary = "Create a sgstream", description = """
      Create a sgstream.
      If values for some script configmap or secret is provided respectively a secret or
       configmap named as the script if name is provided or as the sgstream with
       `-init-script-<script index>` suffix is created/patched.

      ### RBAC permissions required

      * sgstreams create
      * secrets get, create, patch
      """)
  @Override
  public StreamDto create(StreamDto resource, @Nullable Boolean dryRun) {
    createOrUpdateSecrets(resource);
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = StreamDto.class))})
  @Operation(summary = "Update a sgstream", description = """
      Update a sgstream.

      ### RBAC permissions required

      * sgstreams patch
      * secrets get, create, patch
      """)
  @Override
  public StreamDto update(StreamDto resource, @Nullable Boolean dryRun) {
    createOrUpdateSecrets(resource);
    return super.update(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a sgstream", description = """
      Delete a sgstream.

      ### RBAC permissions required

      * sgstreams delete
      """)
  @Override
  public void delete(StreamDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  private void createOrUpdateSecrets(StreamDto resource) {
    var secretsToCreate = Seq.seq(getSecretsToCreate(resource))
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
    secretsToCreate.stream()
        .filter(t -> t.v2.isEmpty())
        .map(Tuple2::v1)
        .forEach(secretWriter::create);
    secretsToCreate.stream()
        .filter(t -> t.v2.isPresent())
        .map(Tuple2::v1)
        .forEach(secretWriter::update);
  }

  private List<Secret> getSecretsToCreate(StreamDto resource) {
    return Seq.<Secret>of()
        .append(Optional.ofNullable(resource.getSpec())
            .map(StreamSpec::getSource)
            .map(StreamSource::getSgCluster)
            .stream()
            .flatMap(sgCluster -> {
              return Seq.<Secret>of()
                  .append(Optional.of(true)
                      .filter(ignore -> sgCluster.getUsernameValue() != null)
                      .map(ignore -> updateCredentialSecret(
                          resource,
                          DEFAULT_SOURCE_USERNAME_KEY,
                          sgCluster::getUsernameValue,
                          sgCluster::getUsername,
                          sgCluster::setUsername)))
                  .append(Optional.of(true)
                      .filter(ignore -> sgCluster.getPasswordValue() != null)
                      .map(ignore -> updateCredentialSecret(
                          resource,
                          DEFAULT_SOURCE_PASSWORD_KEY,
                          sgCluster::getPasswordValue,
                          sgCluster::getPassword,
                          sgCluster::setPassword)));
            }))
        .append(Optional.ofNullable(resource.getSpec())
            .map(StreamSpec::getSource)
            .map(StreamSource::getPostgres)
            .stream()
            .flatMap(postgres -> {
              return Seq.<Secret>of()
                  .append(Optional.of(true)
                      .filter(ignore -> postgres.getUsernameValue() != null)
                      .map(ignore -> updateCredentialSecret(
                          resource,
                          DEFAULT_SOURCE_USERNAME_KEY,
                          postgres::getUsernameValue,
                          postgres::getUsername,
                          postgres::setUsername)))
                  .append(Optional.of(true)
                      .filter(ignore -> postgres.getPasswordValue() != null)
                      .map(ignore -> updateCredentialSecret(
                          resource,
                          DEFAULT_SOURCE_PASSWORD_KEY,
                          postgres::getPasswordValue,
                          postgres::getPassword,
                          postgres::setPassword)));
            }))
        .append(Optional.ofNullable(resource.getSpec())
            .map(StreamSpec::getTarget)
            .map(StreamTarget::getSgCluster)
            .stream()
            .flatMap(sgCluster -> {
              return Seq.<Secret>of()
                  .append(Optional.of(true)
                      .filter(ignore -> sgCluster.getUsernameValue() != null)
                      .map(ignore -> updateCredentialSecret(
                          resource,
                          DEFAULT_TARGET_USERNAME_KEY,
                          sgCluster::getUsernameValue,
                          sgCluster::getUsername,
                          sgCluster::setUsername)))
                  .append(Optional.of(true)
                      .filter(ignore -> sgCluster.getPasswordValue() != null)
                      .map(ignore -> updateCredentialSecret(
                          resource,
                          DEFAULT_TARGET_PASSWORD_KEY,
                          sgCluster::getPasswordValue,
                          sgCluster::getPassword,
                          sgCluster::setPassword)));
            }))
        .toList();
  }

  private Secret updateCredentialSecret(
      StreamDto resource,
      String defaultKey,
      Supplier<String> credentialValueGetter,
      Supplier<SecretKeySelector> secretKeySelectorGetter,
      Consumer<SecretKeySelector> secretKeySelectorSetter) {
    String secretName = resource.getMetadata().getName() + "-credentials";
    if (credentialValueGetter.get() != null
        && secretKeySelectorGetter.get() == null) {
      SecretKeySelector secretKeyRef = new SecretKeySelector();
      secretKeyRef.setName(secretName);
      secretKeyRef.setKey(defaultKey);
      secretKeySelectorSetter.accept(secretKeyRef);
    }
    return new SecretBuilder()
        .withNewMetadata()
        .withName(secretKeySelectorGetter.get().getName())
        .withNamespace(resource.getMetadata().getNamespace())
        .endMetadata()
        .withData(
            Map.of(
                secretKeySelectorGetter.get().getKey(),
                ResourceUtil.encodeSecret(credentialValueGetter.get())))
        .build();
  }

  @Override
  protected void updateSpec(StackGresStream resourceToUpdate, StackGresStream resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}

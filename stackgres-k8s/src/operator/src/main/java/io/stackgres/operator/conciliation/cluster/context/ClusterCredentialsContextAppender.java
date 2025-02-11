/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterCredentialsContextAppender
    extends ClusterContextAppenderWithSecrets {

  private final ResourceFinder<Secret> secretFinder;

  public ClusterCredentialsContextAppender(
      ResourceFinder<Secret> secretFinder) {
    super(secretFinder);
    this.secretFinder = secretFinder;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Credentials credentials = getCredentials(cluster);
    contextBuilder
        .superuserUsername(credentials.superuserUsername)
        .superuserPassword(credentials.superuserPassword)
        .replicationUsername(credentials.replicationUsername)
        .replicationPassword(credentials.replicationPassword)
        .authenticatorUsername(credentials.authenticatorUsername)
        .authenticatorPassword(credentials.authenticatorPassword)
        .patroniRestApiPassword(credentials.patroniRestApiPassword);
  }

  record Credentials(
      Optional<String> superuserUsername,
      Optional<String> superuserPassword,
      Optional<String> replicationUsername,
      Optional<String> replicationPassword,
      Optional<String> authenticatorUsername,
      Optional<String> authenticatorPassword,
      Optional<String> patroniRestApiPassword) {
  }

  private Credentials getCredentials(
      final StackGresCluster cluster) {
    final Credentials credentials;

    if (Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getSgCluster)
        .isPresent()) {
      credentials = getReplicatedFromUsersForCluster(cluster);
    } else if (Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getUsers)
        .isPresent()) {
      credentials = getReplicatedFromUsersFromConfig(cluster);
    } else {
      credentials = getCredentialsFromConfig(cluster);
    }
    return credentials;
  }

  private Credentials getReplicatedFromUsersForCluster(
      final StackGresCluster cluster) {
    final Credentials replicateFromUsers;
    final String replicateFromCluster = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getSgCluster)
        .orElseThrow();
    final String secretName = PatroniUtil.secretName(replicateFromCluster);
    final Secret replicateFromClusterSecret = secretFinder
        .findByNameAndNamespace(secretName, cluster.getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not find secret " + secretName
            + " for SGCluster " + replicateFromCluster
            + " to replicate from"));

    final var superuserUsername = getSecretKeyOrThrow(replicateFromClusterSecret,
        StackGresPasswordKeys.SUPERUSER_USERNAME_ENV,
        "Superuser username key " + StackGresPasswordKeys.SUPERUSER_USERNAME_ENV
        + " was not found in secret " + secretName);
    final var superuserPassword = getSecretKeyOrThrow(replicateFromClusterSecret,
        StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV,
        "Superuser password key " + StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV
        + " was not found in secret " + secretName);

    final var replicationUsername = getSecretKeyOrThrow(replicateFromClusterSecret,
        StackGresPasswordKeys.REPLICATION_USERNAME_ENV,
        "Replication username key " + StackGresPasswordKeys.REPLICATION_USERNAME_ENV
        + " was not found in secret " + secretName);
    final var replicationPassword = getSecretKeyOrThrow(replicateFromClusterSecret,
        StackGresPasswordKeys.REPLICATION_PASSWORD_ENV,
        "Replication password key " + StackGresPasswordKeys.REPLICATION_PASSWORD_ENV
        + " was not found in secret " + secretName);

    final var authenticatorUsername = getSecretKeyOrThrow(replicateFromClusterSecret,
        StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV,
        "Authenticator username key " + StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV
        + " was not found in secret " + secretName);
    final var authenticatorPassword = getSecretKeyOrThrow(replicateFromClusterSecret,
        StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV,
        "Authenticator password key " + StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV
        + " was not found in secret " + secretName);

    replicateFromUsers = new Credentials(
        superuserUsername,
        superuserPassword,
        replicationUsername,
        replicationPassword,
        authenticatorUsername,
        authenticatorPassword,
        Optional.empty());
    return replicateFromUsers;
  }

  private Credentials getReplicatedFromUsersFromConfig(
      final StackGresCluster cluster) {
    final Credentials replicateFromUsers;
    final var users =
        Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getUsers);

    final var superuserUsername = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterReplicateFromUsers::getSuperuser,
        StackGresClusterReplicateFromUserSecretKeyRef::getUsername,
        secretKeySelector -> "Superuser username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Superuser username secret " + secretKeySelector.getName()
        + " was not found");
    final var superuserPassword = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterReplicateFromUsers::getSuperuser,
        StackGresClusterReplicateFromUserSecretKeyRef::getPassword,
        secretKeySelector -> "Superuser password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Superuser password secret " + secretKeySelector.getName()
        + " was not found");

    final var replicationUsername = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterReplicateFromUsers::getReplication,
        StackGresClusterReplicateFromUserSecretKeyRef::getUsername,
        secretKeySelector -> "Replication username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Replication username secret " + secretKeySelector.getName()
        + " was not found");
    final var replicationPassword = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterReplicateFromUsers::getReplication,
        StackGresClusterReplicateFromUserSecretKeyRef::getPassword,
        secretKeySelector -> "Replication password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Replication password secret " + secretKeySelector.getName()
        + " was not found");

    final var authenticatorUsername = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterReplicateFromUsers::getAuthenticator,
        StackGresClusterReplicateFromUserSecretKeyRef::getUsername,
        secretKeySelector -> "Authenticator username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Authenticator username secret " + secretKeySelector.getName()
        + " was not found");
    final var authenticatorPassword = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterReplicateFromUsers::getAuthenticator,
        StackGresClusterReplicateFromUserSecretKeyRef::getPassword,
        secretKeySelector -> "Authenticator password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Authenticator password secret " + secretKeySelector.getName()
        + " was not found");

    replicateFromUsers = new Credentials(
        superuserUsername,
        superuserPassword,
        replicationUsername,
        replicationPassword,
        authenticatorUsername,
        authenticatorPassword,
      Optional.empty());
    return replicateFromUsers;
  }

  private Credentials getCredentialsFromConfig(
      final StackGresCluster cluster) {
    final var users =
        Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getCredentials)
        .map(StackGresClusterCredentials::getUsers);
    final var patroni =
        Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getCredentials)
        .map(StackGresClusterCredentials::getPatroni);

    final var superuserUsername = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterUsersCredentials::getSuperuser,
        StackGresClusterUserSecretKeyRef::getUsername,
        secretKeySelector -> "Superuser username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Superuser username secret " + secretKeySelector.getName()
        + " was not found");
    final var superuserPassword = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterUsersCredentials::getSuperuser,
        StackGresClusterUserSecretKeyRef::getPassword,
        secretKeySelector -> "Superuser password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Superuser password secret " + secretKeySelector.getName()
        + " was not found");

    final var replicationUsername = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterUsersCredentials::getReplication,
        StackGresClusterUserSecretKeyRef::getUsername,
        secretKeySelector -> "Replication username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Replication username secret " + secretKeySelector.getName()
        + " was not found");
    final var replicationPassword = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterUsersCredentials::getReplication,
        StackGresClusterUserSecretKeyRef::getPassword,
        secretKeySelector -> "Replication password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Replication password secret " + secretKeySelector.getName()
        + " was not found");

    final var authenticatorUsername = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterUsersCredentials::getAuthenticator,
        StackGresClusterUserSecretKeyRef::getUsername,
        secretKeySelector -> "Authenticator username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Authenticator username secret " + secretKeySelector.getName()
        + " was not found");
    final var authenticatorPassword = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), users,
        StackGresClusterUsersCredentials::getAuthenticator,
        StackGresClusterUserSecretKeyRef::getPassword,
        secretKeySelector -> "Authenticator password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Authenticator password secret " + secretKeySelector.getName()
        + " was not found");
    final var patroniRestApiPassword = getSecretAndKeyOrThrow(cluster.getMetadata().getNamespace(), patroni,
        StackGresClusterPatroniCredentials::getRestApiPassword,
        secretKeySelector -> "Patroni REST API password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Patroni REST API password secret " + secretKeySelector.getName()
        + " was not found");

    return new Credentials(
        superuserUsername,
        superuserPassword,
        replicationUsername,
        replicationPassword,
        authenticatorUsername,
        authenticatorPassword,
        patroniRestApiPassword);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromStorage;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.prometheus.PrometheusConfig;
import io.stackgres.common.prometheus.PrometheusConfigSpec;
import io.stackgres.common.prometheus.PrometheusInstallation;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.factory.cluster.PostgresSslSecret;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterRequiredResourcesGenerator.class);

  private final Supplier<VersionInfo> kubernetesVersionSupplier;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresBackup> backupFinder;

  private final ResourceFinder<Secret> secretFinder;

  private final CustomResourceScanner<PrometheusConfig> prometheusScanner;

  private final CustomResourceScanner<StackGresBackup> backupScanner;

  private final OperatorPropertyContext operatorContext;

  private final RequiredResourceDecorator<StackGresClusterContext> decorator;

  @Inject
  public ClusterRequiredResourcesGenerator(
      Supplier<VersionInfo> kubernetesVersionSupplier,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresBackup> backupFinder,
      ResourceFinder<Secret> secretFinder,
      CustomResourceScanner<PrometheusConfig> prometheusScanner,
      CustomResourceScanner<StackGresBackup> backupScanner,
      OperatorPropertyContext operatorContext,
      RequiredResourceDecorator<StackGresClusterContext> decorator) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
    this.clusterFinder = clusterFinder;
    this.backupConfigFinder = backupConfigFinder;
    this.objectStorageFinder = objectStorageFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.poolingConfigFinder = poolingConfigFinder;
    this.profileFinder = profileFinder;
    this.backupFinder = backupFinder;
    this.secretFinder = secretFinder;
    this.prometheusScanner = prometheusScanner;
    this.backupScanner = backupScanner;
    this.operatorContext = operatorContext;
    this.decorator = decorator;
  }

  private static PrometheusInstallation toPrometheusInstallation(PrometheusConfig pc) {
    Map<String, String> matchLabels = Optional.ofNullable(pc.getSpec())
        .map(PrometheusConfigSpec::getPodMonitorSelector)
        .map(LabelSelector::getMatchLabels)
        .map(Map::copyOf)
        .orElse(Map.of());
    PrometheusInstallation pi = new PrometheusInstallation();
    pi.setNamespace(pc.getMetadata().getNamespace());
    pi.setMatchLabels(matchLabels);
    return pi;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresCluster config) {
    final ObjectMeta metadata = config.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    VersionInfo kubernetesVersion = kubernetesVersionSupplier.get();

    final StackGresClusterSpec spec = config.getSpec();
    final StackGresClusterConfiguration clusterConfiguration = spec.getConfiguration();
    final StackGresPostgresConfig pgConfig = postgresConfigFinder
        .findByNameAndNamespace(clusterConfiguration.getPostgresConfig(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + clusterNamespace + "." + clusterName
                + " have a non existent SGPostgresConfig postgresconf"));

    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(spec.getResourceProfile(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + clusterNamespace + "." + clusterName + " have a non existent "
                + StackGresProfile.KIND + " " + spec.getResourceProfile()));
    final Optional<StackGresBackupConfig> backupConfig = Optional
        .ofNullable(clusterConfiguration.getBackupConfig())
        .flatMap(backupConfigName -> backupConfigFinder
            .findByNameAndNamespace(backupConfigName, clusterNamespace));

    final Optional<StackGresObjectStorage> objectStorage = Optional
        .ofNullable(clusterConfiguration.getBackups())
        .map(Collection::stream)
        .flatMap(Stream::findFirst)
        .map(StackGresClusterBackupConfiguration::getObjectStorage)
        .flatMap(objectStorageName -> objectStorageFinder
            .findByNameAndNamespace(objectStorageName, clusterNamespace));

    final Optional<StackGresPoolingConfig> pooling = Optional
        .ofNullable(clusterConfiguration.getConnectionPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(poolingConfigName, clusterNamespace));

    final Set<String> clusterBackupNamespaces = getClusterBackupNamespaces(clusterNamespace);

    final Optional<StackGresBackup> restoreBackup = findRestoreBackup(config, clusterNamespace);

    final Credentials credentials = getCredentials(clusterNamespace, spec);

    final Optional<StackGresCluster> replicateCluster =
        Optional.of(spec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getSgCluster)
        .flatMap(sgCluster -> Optional.of(
            clusterFinder.findByNameAndNamespace(sgCluster, clusterNamespace)
            .orElseThrow(() -> new IllegalArgumentException("Can not find SGCluster "
                + sgCluster + " to replicate from"))));

    final Optional<StackGresObjectStorage> replicateObjectStorageConfig =
        replicateCluster
        .flatMap(replicateFromCluster -> Optional.of(replicateFromCluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getConfiguration)
            .map(StackGresClusterConfiguration::getBackups)
            .stream()
            .flatMap(List::stream)
            .findFirst()
            .map(StackGresClusterBackupConfiguration::getObjectStorage))
        .or(() -> Optional.of(spec)
          .map(StackGresClusterSpec::getReplicateFrom)
          .map(StackGresClusterReplicateFrom::getStorage)
          .map(StackGresClusterReplicateFromStorage::getSgObjectStorage))
        .map(sgObjectStorage -> objectStorageFinder
            .findByNameAndNamespace(sgObjectStorage, clusterNamespace)
            .orElseThrow(() -> new IllegalArgumentException("Can not find SGObjectStorage "
                + sgObjectStorage + " to replicate from")));

    final PostgresSsl postgresSsl = getPostgresSsl(clusterNamespace, config);

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(config)
        .postgresConfig(pgConfig)
        .profile(profile)
        .backupConfig(backupConfig)
        .objectStorageConfig(objectStorage)
        .poolingConfig(pooling)
        .restoreBackup(restoreBackup)
        .prometheus(getPrometheus(config))
        .clusterBackupNamespaces(clusterBackupNamespaces)
        .databaseSecret(secretFinder.findByNameAndNamespace(clusterName, clusterNamespace))
        .replicateCluster(replicateCluster)
        .replicateObjectStorageConfig(replicateObjectStorageConfig)
        .superuserUsername(credentials.superuserUsername)
        .superuserPassword(credentials.superuserPassword)
        .replicationUsername(credentials.replicationUsername)
        .replicationPassword(credentials.replicationPassword)
        .authenticatorUsername(credentials.authenticatorUsername)
        .authenticatorPassword(credentials.authenticatorPassword)
        .patroniRestApiPassword(credentials.patroniRestApiPassword)
        .postgresSslCertificate(postgresSsl.certificate)
        .postgresSslPrivateKey(postgresSsl.privateKey)
        .build();

    return decorator.decorateResources(context);
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
      final String clusterNamespace,
      final StackGresClusterSpec spec) {
    final Credentials credentials;

    if (Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getSgCluster)
        .isPresent()) {
      credentials = getReplicatedFromUsersForCluster(clusterNamespace, spec);
    } else if (Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getUsers)
        .isPresent()) {
      credentials = getReplicatedFromUsersFromConfig(clusterNamespace, spec);
    } else {
      credentials = getCredentialsFromConfig(clusterNamespace, spec);
    }
    return credentials;
  }

  private Credentials getReplicatedFromUsersForCluster(final String clusterNamespace,
      final StackGresClusterSpec spec) {
    final Credentials replicateFromUsers;
    final String replicateFromCluster = Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getSgCluster)
        .orElseThrow();
    final String secretName = PatroniSecret.name(replicateFromCluster);
    final Secret replicateFromClusterSecret = secretFinder
        .findByNameAndNamespace(secretName, clusterNamespace)
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

  private Credentials getReplicatedFromUsersFromConfig(final String clusterNamespace,
      final StackGresClusterSpec spec) {
    final Credentials replicateFromUsers;
    final var users =
        Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getUsers);

    final var superuserUsername = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterReplicateFromUsers::getSuperuser,
        StackGresClusterReplicateFromUserSecretKeyRef::getUsername,
        secretKeySelector -> "Superuser username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Superuser username secret " + secretKeySelector.getName()
        + " was not found");
    final var superuserPassword = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterReplicateFromUsers::getSuperuser,
        StackGresClusterReplicateFromUserSecretKeyRef::getPassword,
        secretKeySelector -> "Superuser password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Superuser password secret " + secretKeySelector.getName()
        + " was not found");

    final var replicationUsername = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterReplicateFromUsers::getReplication,
        StackGresClusterReplicateFromUserSecretKeyRef::getUsername,
        secretKeySelector -> "Replication username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Replication username secret " + secretKeySelector.getName()
        + " was not found");
    final var replicationPassword = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterReplicateFromUsers::getReplication,
        StackGresClusterReplicateFromUserSecretKeyRef::getPassword,
        secretKeySelector -> "Replication password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Replication password secret " + secretKeySelector.getName()
        + " was not found");

    final var authenticatorUsername = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterReplicateFromUsers::getAuthenticator,
        StackGresClusterReplicateFromUserSecretKeyRef::getUsername,
        secretKeySelector -> "Authenticator username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Authenticator username secret " + secretKeySelector.getName()
        + " was not found");
    final var authenticatorPassword = getSecretAndKeyOrThrow(clusterNamespace, users,
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

  private Credentials getCredentialsFromConfig(final String clusterNamespace,
      final StackGresClusterSpec spec) {
    final Credentials configuredUsers;
    final var users =
        Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getCredentials)
        .map(StackGresClusterCredentials::getUsers);
    final var patroni =
        Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getCredentials)
        .map(StackGresClusterCredentials::getPatroni);

    final var superuserUsername = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterUsersCredentials::getSuperuser,
        StackGresClusterUserSecretKeyRef::getUsername,
        secretKeySelector -> "Superuser username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Superuser username secret " + secretKeySelector.getName()
        + " was not found");
    final var superuserPassword = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterUsersCredentials::getSuperuser,
        StackGresClusterUserSecretKeyRef::getPassword,
        secretKeySelector -> "Superuser password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Superuser password secret " + secretKeySelector.getName()
        + " was not found");

    final var replicationUsername = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterUsersCredentials::getReplication,
        StackGresClusterUserSecretKeyRef::getUsername,
        secretKeySelector -> "Replication username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Replication username secret " + secretKeySelector.getName()
        + " was not found");
    final var replicationPassword = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterUsersCredentials::getReplication,
        StackGresClusterUserSecretKeyRef::getPassword,
        secretKeySelector -> "Replication password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Replication password secret " + secretKeySelector.getName()
        + " was not found");

    final var authenticatorUsername = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterUsersCredentials::getAuthenticator,
        StackGresClusterUserSecretKeyRef::getUsername,
        secretKeySelector -> "Authenticator username key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Authenticator username secret " + secretKeySelector.getName()
        + " was not found");
    final var authenticatorPassword = getSecretAndKeyOrThrow(clusterNamespace, users,
        StackGresClusterUsersCredentials::getAuthenticator,
        StackGresClusterUserSecretKeyRef::getPassword,
        secretKeySelector -> "Authenticator password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Authenticator password secret " + secretKeySelector.getName()
        + " was not found");
    final var patroniRestApiPassword = getSecretAndKeyOrThrow(clusterNamespace, patroni,
        StackGresClusterPatroniCredentials::getRestApiPassword,
        secretKeySelector -> "Patroni REST API password key " + secretKeySelector.getKey()
        + " was not found in secret " + secretKeySelector.getName(),
        secretKeySelector -> "Patroni REST API password secret " + secretKeySelector.getName()
        + " was not found");
    configuredUsers = new Credentials(
        superuserUsername,
        superuserPassword,
        replicationUsername,
        replicationPassword,
        authenticatorUsername,
        authenticatorPassword,
        patroniRestApiPassword);
    return configuredUsers;
  }

  record PostgresSsl(
      Optional<String> certificate,
      Optional<String> privateKey) {
  }

  private PostgresSsl getPostgresSsl(
      final String clusterNamespace,
      final StackGresCluster cluster) {
    var ssl = Optional.ofNullable(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl);
    if (ssl.map(StackGresClusterSsl::getEnabled).orElse(false)) {
      if (ssl.map(StackGresClusterSsl::getCertificateSecretKeySelector).isPresent()
          && ssl.map(StackGresClusterSsl::getPrivateKeySecretKeySelector).isPresent()) {
        return new PostgresSsl(
            getSecretAndKeyOrThrow(clusterNamespace, ssl,
                StackGresClusterSsl::getCertificateSecretKeySelector,
                secretKeySelector -> "Certificate key " + secretKeySelector.getKey()
                + " was not found in secret " + secretKeySelector.getName(),
                secretKeySelector -> "Certificate secret " + secretKeySelector.getName()
                + " was not found"),
            getSecretAndKeyOrThrow(clusterNamespace, ssl,
                StackGresClusterSsl::getPrivateKeySecretKeySelector,
                secretKeySelector -> "Private key key " + secretKeySelector.getKey()
                + " was not found in secret " + secretKeySelector.getName(),
                secretKeySelector -> "Private key secret " + secretKeySelector.getName()
                + " was not found"));
      }
      return new PostgresSsl(
          getSecretAndKey(clusterNamespace, ssl,
              s -> new SecretKeySelector(
                  PostgresSslSecret.CERTIFICATE_KEY, PostgresSslSecret.name(cluster))),
          getSecretAndKey(clusterNamespace, ssl,
              s -> new SecretKeySelector(
                  PostgresSslSecret.PRIVATE_KEY_KEY, PostgresSslSecret.name(cluster))));
    }

    return new PostgresSsl(Optional.empty(), Optional.empty());
  }

  private <T, S> Optional<String> getSecretAndKeyOrThrow(final String clusterNamespace,
      final Optional<T> secretSection,
      final Function<T, S> secretKeyRefGetter,
      final Function<S, SecretKeySelector> secretKeySelectorGetter,
      final Function<SecretKeySelector, String> onKeyNotFoundMessageGetter,
      final Function<SecretKeySelector, String> onSecretNotFoundMessageGetter) {
    return secretSection
        .map(secretKeyRefGetter)
        .map(secretKeySelectorGetter)
        .map(secretKeySelector -> secretFinder
            .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
            .flatMap(secret -> getSecretKeyOrThrow(secret, secretKeySelector.getKey(),
                onKeyNotFoundMessageGetter.apply(secretKeySelector)))
            .orElseThrow(() -> new IllegalArgumentException(
                onSecretNotFoundMessageGetter.apply(secretKeySelector))));
  }

  private <T> Optional<String> getSecretAndKeyOrThrow(final String clusterNamespace,
      final Optional<T> credential,
      final Function<T, SecretKeySelector> secretKeySelectorGetter,
      final Function<SecretKeySelector, String> onKeyNotFoundMessageGetter,
      final Function<SecretKeySelector, String> onSecretNotFoundMessageGetter) {
    return credential
        .map(secretKeySelectorGetter)
        .map(secretKeySelector -> secretFinder
            .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
            .flatMap(secret -> getSecretKeyOrThrow(secret, secretKeySelector.getKey(),
                onKeyNotFoundMessageGetter.apply(secretKeySelector)))
            .orElseThrow(() -> new IllegalArgumentException(
                onSecretNotFoundMessageGetter.apply(secretKeySelector))));
  }

  private Optional<String> getSecretKeyOrThrow(
      final Secret secret,
      final String key,
      final String onKeyNotFoundMessage) {
    return Optional.of(
        Optional.of(secret)
        .map(Secret::getData)
        .map(data -> data.get(key))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new IllegalArgumentException(onKeyNotFoundMessage)));
  }

  private <T> Optional<String> getSecretAndKey(final String clusterNamespace,
      final Optional<T> credential,
      final Function<T, SecretKeySelector> secretKeySelectorGetter) {
    return credential
        .map(secretKeySelectorGetter)
        .flatMap(secretKeySelector -> secretFinder
            .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
            .flatMap(secret -> getSecretKey(secret, secretKeySelector.getKey())));
  }

  private Optional<String> getSecretKey(
      final Secret secret,
      final String key) {
    return Optional.of(secret)
        .map(Secret::getData)
        .map(data -> data.get(key))
        .map(ResourceUtil::decodeSecret);
  }

  private Set<String> getClusterBackupNamespaces(final String clusterNamespace) {
    return backupScanner.getResources()
        .stream()
        .map(Optional::of)
        .filter(backup -> backup
            .map(StackGresBackup::getSpec)
            .map(StackGresBackupSpec::getSgCluster)
            .map(StackGresUtil::isRelativeIdNotInSameNamespace)
            .orElse(false))
        .map(backup -> backup
            .map(StackGresBackup::getMetadata)
            .map(ObjectMeta::getNamespace))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(Predicate.not(clusterNamespace::equals))
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet();
  }

  private Optional<StackGresBackup> findRestoreBackup(StackGresCluster config,
      final String clusterNamespace) {
    return Optional
        .ofNullable(config.getSpec().getInitData())
        .map(StackGresClusterInitData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getName)
        .flatMap(backupName -> backupFinder.findByNameAndNamespace(backupName, clusterNamespace));
  }

  public Optional<Prometheus> getPrometheus(StackGresCluster cluster) {
    boolean isAutobindAllowed = operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND);

    boolean isPrometheusAutobindEnabled = Optional.ofNullable(cluster.getSpec()
        .getPrometheusAutobind()).orElse(false);

    if (isAutobindAllowed && isPrometheusAutobindEnabled) {
      LOGGER.trace("Prometheus auto bind enabled, looking for prometheus installations");

      final Optional<List<PrometheusConfig>> prometheusConfigsOpt = prometheusScanner
          .findResources();

      return prometheusConfigsOpt
          .map(prometheusConfigs -> prometheusConfigs.stream()
              .map(ClusterRequiredResourcesGenerator::toPrometheusInstallation)
              .toList())
          .map(installations -> new Prometheus(!installations.isEmpty(), installations));

    } else {
      return Optional.of(new Prometheus(false, null));
    }
  }

}

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
import io.stackgres.common.crd.sgbackup.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromStorage;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.prometheus.Prometheus;
import io.stackgres.common.prometheus.PrometheusInstallation;
import io.stackgres.common.prometheus.PrometheusSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.PrometheusContext;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.cluster.PostgresSslSecret;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterRequiredResourcesGenerator.class);

  private final Supplier<VersionInfo> kubernetesVersionSupplier;

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresBackup> backupFinder;

  private final ResourceFinder<Secret> secretFinder;

  private final CustomResourceScanner<Prometheus> prometheusScanner;

  private final CustomResourceScanner<StackGresBackup> backupScanner;

  private final BackupEnvVarFactory backupEnvVarFactory;

  private final OperatorPropertyContext operatorContext;

  private final ResourceGenerationDiscoverer<StackGresClusterContext> discoverer;

  @Inject
  public ClusterRequiredResourcesGenerator(
      Supplier<VersionInfo> kubernetesVersionSupplier,
      CustomResourceScanner<StackGresConfig> configScanner,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      CustomResourceFinder<StackGresObjectStorage> objectStorageFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresBackup> backupFinder,
      ResourceFinder<Secret> secretFinder,
      CustomResourceScanner<Prometheus> prometheusScanner,
      CustomResourceScanner<StackGresBackup> backupScanner,
      BackupEnvVarFactory backupEnvVarFactory,
      OperatorPropertyContext operatorContext,
      ResourceGenerationDiscoverer<StackGresClusterContext> discoverer) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
    this.configScanner = configScanner;
    this.clusterFinder = clusterFinder;
    this.objectStorageFinder = objectStorageFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.poolingConfigFinder = poolingConfigFinder;
    this.profileFinder = profileFinder;
    this.backupFinder = backupFinder;
    this.secretFinder = secretFinder;
    this.prometheusScanner = prometheusScanner;
    this.backupScanner = backupScanner;
    this.backupEnvVarFactory = backupEnvVarFactory;
    this.operatorContext = operatorContext;
    this.discoverer = discoverer;
  }

  private static PrometheusInstallation toPrometheusInstallation(Prometheus pc) {
    Map<String, String> matchLabels = Optional.ofNullable(pc.getSpec())
        .map(PrometheusSpec::getPodMonitorSelector)
        .map(LabelSelector::getMatchLabels)
        .map(Map::copyOf)
        .orElse(Map.of());
    PrometheusInstallation pi = new PrometheusInstallation();
    pi.setNamespace(pc.getMetadata().getNamespace());
    pi.setMatchLabels(matchLabels);
    return pi;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresCluster cluster) {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    VersionInfo kubernetesVersion = kubernetesVersionSupplier.get();

    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig not found or more than one exists. Aborting reoconciliation!"));
    final StackGresClusterSpec spec = cluster.getSpec();
    final StackGresClusterConfigurations clusterConfiguration = spec.getConfigurations();
    final StackGresPostgresConfig pgConfig = postgresConfigFinder
        .findByNameAndNamespace(clusterConfiguration.getSgPostgresConfig(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + clusterNamespace + "." + clusterName
                + " have a non existent SGPostgresConfig postgresconf"));

    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(spec.getSgInstanceProfile(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + clusterNamespace + "." + clusterName + " have a non existent "
                + StackGresProfile.KIND + " " + spec.getSgInstanceProfile()));

    final Optional<StackGresObjectStorage> backupStorageObject = Optional
        .ofNullable(clusterConfiguration.getBackups())
        .map(Collection::stream)
        .flatMap(Stream::findFirst)
        .map(StackGresClusterBackupConfiguration::getSgObjectStorage)
        .map(backupObjectStorageName -> objectStorageFinder
            .findByNameAndNamespace(backupObjectStorageName, clusterNamespace)
            .orElseThrow(() -> new IllegalArgumentException(
                "SGObjectStorage " + backupObjectStorageName + " not found")));

    final Optional<StackGresPoolingConfig> pooling = Optional
        .ofNullable(clusterConfiguration.getSgPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(poolingConfigName, clusterNamespace));

    final Set<String> clusterBackupNamespaces = getClusterBackupNamespaces(clusterNamespace);

    Optional<Secret> databaseSecret =
        secretFinder.findByNameAndNamespace(clusterName, clusterNamespace);

    final Map<String, Secret> backupSecrets = backupStorageObject
        .map(StackGresObjectStorage::getSpec)
        .stream()
        .flatMap(backupEnvVarFactory::streamStorageSecretReferences)
        .map(secretKeySelector -> secretKeySelector.getName())
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet()
        .stream()
        .map(name -> Tuple.tuple(
            name,
            secretFinder
            .findByNameAndNamespace(name, clusterNamespace)
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + name + " not found"))))
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));

    final Optional<StackGresBackup> restoreBackup = findRestoreBackup(cluster, clusterNamespace);

    final Map<String, Secret> restoreSecrets = restoreBackup
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getSgBackupConfig)
        .map(StackGresBackupConfigSpec::getStorage)
        .stream()
        .flatMap(backupEnvVarFactory::streamStorageSecretReferences)
        .map(secretKeySelector -> secretKeySelector.getName())
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet()
        .stream()
        .map(name -> Tuple.tuple(
            name,
            secretFinder
            .findByNameAndNamespace(name, clusterNamespace)
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + name + " not found"))))
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));

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

    final Optional<StackGresObjectStorage> replicateObjectStorage =
        replicateCluster
        .flatMap(replicateFromCluster -> Optional.of(replicateFromCluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getConfigurations)
            .map(StackGresClusterConfigurations::getBackups)
            .stream()
            .flatMap(List::stream)
            .findFirst()
            .map(StackGresClusterBackupConfiguration::getSgObjectStorage))
        .or(() -> Optional.of(spec)
          .map(StackGresClusterSpec::getReplicateFrom)
          .map(StackGresClusterReplicateFrom::getStorage)
          .map(StackGresClusterReplicateFromStorage::getSgObjectStorage))
        .map(sgObjectStorage -> objectStorageFinder
            .findByNameAndNamespace(sgObjectStorage, clusterNamespace)
            .orElseThrow(() -> new IllegalArgumentException("Can not find SGObjectStorage "
                + sgObjectStorage + " to replicate from")));

    final Map<String, Secret> replicateSecrets = replicateObjectStorage
        .map(StackGresObjectStorage::getSpec)
        .stream()
        .flatMap(backupEnvVarFactory::streamStorageSecretReferences)
        .map(secretKeySelector -> secretKeySelector.getName())
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet()
        .stream()
        .map(name -> Tuple.tuple(
            name,
            secretFinder
            .findByNameAndNamespace(name, clusterNamespace)
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + name + " not found"))))
        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));

    final var userPasswordForBinding = getUserPasswordServiceBindingFromSecret(clusterNamespace,
        spec);

    final PostgresSsl postgresSsl = getPostgresSsl(clusterNamespace, cluster);

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .config(config)
        .source(cluster)
        .postgresConfig(pgConfig)
        .profile(profile)
        .objectStorage(backupStorageObject)
        .poolingConfig(pooling)
        .backupSecrets(backupSecrets)
        .restoreBackup(restoreBackup)
        .restoreSecrets(restoreSecrets)
        .replicateSecrets(replicateSecrets)
        .prometheusContext(getPrometheusContext(cluster))
        .clusterBackupNamespaces(clusterBackupNamespaces)
        .databaseSecret(databaseSecret)
        .replicateCluster(replicateCluster)
        .replicateObjectStorageConfig(replicateObjectStorage)
        .superuserUsername(credentials.superuserUsername)
        .superuserPassword(credentials.superuserPassword)
        .replicationUsername(credentials.replicationUsername)
        .replicationPassword(credentials.replicationPassword)
        .authenticatorUsername(credentials.authenticatorUsername)
        .authenticatorPassword(credentials.authenticatorPassword)
        .patroniRestApiPassword(credentials.patroniRestApiPassword)
        .userPasswordForBinding(userPasswordForBinding)
        .postgresSslCertificate(postgresSsl.certificate)
        .postgresSslPrivateKey(postgresSsl.privateKey)
        .build();

    return discoverer.generateResources(context);
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
    final var users =
        Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getCredentials)
        .map(StackGresClusterCredentials::getUsers);
    final var patroni =
        Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getCredentials)
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

    return new Credentials(
        superuserUsername,
        superuserPassword,
        replicationUsername,
        replicationPassword,
        authenticatorUsername,
        authenticatorPassword,
        patroniRestApiPassword);
  }

  private Optional<String> getUserPasswordServiceBindingFromSecret(final String clusterNamespace,
      final StackGresClusterSpec spec) {
    final var serviceBindingConfig = Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBinding);
    return getSecretAndKeyOrThrow(clusterNamespace,
      serviceBindingConfig,
      StackGresClusterServiceBinding::getPassword,
      secretKeySelector -> "Service Binding password key " + secretKeySelector.getKey()
          + " was not found in secret " + secretKeySelector.getName(),
      secretKeySelector -> "Service Binding password secret " + secretKeySelector.getName()
          + " was not found");
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
        .flatMap(Optional::stream)
        .filter(Predicate.not(clusterNamespace::equals))
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet();
  }

  private Optional<StackGresBackup> findRestoreBackup(StackGresCluster config,
      final String clusterNamespace) {
    return Optional
        .ofNullable(config.getSpec().getInitialData())
        .map(StackGresClusterInitialData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getName)
        .flatMap(backupName -> backupFinder.findByNameAndNamespace(backupName, clusterNamespace));
  }

  public Optional<PrometheusContext> getPrometheusContext(StackGresCluster cluster) {
    boolean isAutobindAllowed = operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND);

    boolean isPrometheusAutobindEnabled = Optional.ofNullable(cluster.getSpec()
        .getPrometheusAutobind()).orElse(false);

    if (isAutobindAllowed && isPrometheusAutobindEnabled) {
      LOGGER.trace("Prometheus auto bind enabled, looking for prometheus installations");

      final Optional<List<Prometheus>> prometheusConfigsOpt = prometheusScanner
          .findResources();

      return prometheusConfigsOpt
          .map(prometheusConfigs -> prometheusConfigs.stream()
              .map(ClusterRequiredResourcesGenerator::toPrometheusInstallation)
              .toList())
          .map(installations -> new PrometheusContext(!installations.isEmpty(), installations));

    } else {
      return Optional.of(new PrometheusContext(false, null));
    }
  }

}

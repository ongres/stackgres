/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterUtil.CERTIFICATE_KEY;
import static io.stackgres.common.StackGresShardedClusterUtil.PRIVATE_KEY_KEY;
import static io.stackgres.common.StackGresShardedClusterUtil.postgresSslSecretName;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupProcess;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupSpec;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterInitialData;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestore;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestoreFromBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphere;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereAuthority;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForDdpUtil;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForShardingSphereUtil;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings(value = "PMD.TooManyStaticImports")
@ApplicationScoped
public class ShardedClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedClusterRequiredResourcesGenerator.class);

  private final Supplier<VersionInfo> kubernetesVersionSupplier;

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  private final DefaultProfileFactory defaultProfileFactory;

  private final DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory;

  private final DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  private final CustomResourceFinder<StackGresShardedBackup> backupFinder;

  private final ResourceFinder<Secret> secretFinder;

  private final ResourceFinder<Endpoints> endpointsFinder;

  private final CustomResourceScanner<StackGresShardedBackup> shardedBackupScanner;

  private final ResourceGenerationDiscoverer<StackGresShardedClusterContext> discoverer;

  @Inject
  public ShardedClusterRequiredResourcesGenerator(
      Supplier<VersionInfo> kubernetesVersionSupplier,
      CustomResourceScanner<StackGresConfig> configScanner,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      DefaultProfileFactory defaultProfileFactory,
      DefaultShardedClusterPostgresConfigFactory defaultPostgresConfigFactory,
      DefaultPoolingConfigFactory defaultPoolingConfigFactory,
      CustomResourceFinder<StackGresShardedBackup> backupFinder,
      ResourceFinder<Secret> secretFinder,
      ResourceFinder<Endpoints> endpointsFinder,
      CustomResourceScanner<StackGresShardedBackup> shardedBackupScanner,
      ResourceGenerationDiscoverer<StackGresShardedClusterContext> discoverer) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
    this.configScanner = configScanner;
    this.profileFinder = profileFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.poolingConfigFinder = poolingConfigFinder;
    this.defaultProfileFactory = defaultProfileFactory;
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
    this.defaultPoolingConfigFactory = defaultPoolingConfigFactory;
    this.backupFinder = backupFinder;
    this.secretFinder = secretFinder;
    this.endpointsFinder = endpointsFinder;
    this.shardedBackupScanner = shardedBackupScanner;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedCluster cluster) {
    final ObjectMeta metadata = cluster.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    final VersionInfo kubernetesVersion = kubernetesVersionSupplier.get();

    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig not found or more than one exists. Aborting reoconciliation!"));

    final Optional<Secret> databaseSecret = secretFinder
        .findByNameAndNamespace(clusterName, clusterNamespace);

    Optional<StackGresProfile> coordinatorProfile = profileFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        clusterNamespace);
    if (!cluster.getSpec().getCoordinator().getSgInstanceProfile().equals(
        defaultProfileFactory.getDefaultResourceName(cluster))
        && coordinatorProfile.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresProfile.KIND
          + " " + cluster.getSpec().getCoordinator().getSgInstanceProfile()
          + " was not found");
    }

    Optional<StackGresPostgresConfig> coordinatorPostgresConfig = postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig(),
        clusterNamespace);
    if (!cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig().equals(
        defaultPostgresConfigFactory.getDefaultResourceName(cluster))
        && coordinatorPostgresConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPostgresConfig.KIND
          + " " + cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig()
          + " was not found");
    }

    Optional<StackGresPoolingConfig> coordinatorPoolingConfig = poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig(),
        clusterNamespace);
    if (!cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig().equals(
        defaultPoolingConfigFactory.getDefaultResourceName(cluster))
        && coordinatorPoolingConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPoolingConfig.KIND
          + " " + cluster.getSpec().getCoordinator().getConfigurationsForCoordinator().getSgPoolingConfig()
          + " was not found");
    }

    Optional<StackGresProfile> shardsProfile = profileFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getSgInstanceProfile(),
        clusterNamespace);
    if (!cluster.getSpec().getShards().getSgInstanceProfile().equals(
        defaultProfileFactory.getDefaultResourceName(cluster))
        && coordinatorProfile.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresProfile.KIND
          + " " + cluster.getSpec().getShards().getSgInstanceProfile()
          + " was not found");
    }

    Optional<StackGresPostgresConfig> shardsPostgresConfig = postgresConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig(),
        clusterNamespace);
    if (!cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig().equals(
        defaultPostgresConfigFactory.getDefaultResourceName(cluster))
        && coordinatorPostgresConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPostgresConfig.KIND
          + " " + cluster.getSpec().getShards().getConfigurations().getSgPostgresConfig()
          + " was not found");
    }

    Optional<StackGresPoolingConfig> shardsPoolingConfig = poolingConfigFinder.findByNameAndNamespace(
        cluster.getSpec().getShards().getConfigurations().getSgPoolingConfig(),
        clusterNamespace);
    if (!cluster.getSpec().getShards().getConfigurations().getSgPoolingConfig().equals(
        defaultPoolingConfigFactory.getDefaultResourceName(cluster))
        && coordinatorPoolingConfig.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresPoolingConfig.KIND
          + " " + cluster.getSpec().getShards().getConfigurations().getSgPoolingConfig()
          + " was not found");
    }

    if (Optional.of(cluster.getSpec())
        .map(StackGresShardedClusterSpec::getInitialData)
        .map(StackGresShardedClusterInitialData::getRestore)
        .map(StackGresShardedClusterRestore::getFromBackup)
        .map(StackGresShardedClusterRestoreFromBackup::getName)
        .isPresent()) {
      String backupName = cluster.getSpec().getInitialData().getRestore()
          .getFromBackup().getName();
      StackGresShardedBackup restoreBackup = backupFinder
          .findByNameAndNamespace(backupName, clusterNamespace)
          .orElseThrow(() -> new IllegalArgumentException(
              StackGresShardedBackup.KIND + " " + backupName + " not found"));

      if (Optional.of(restoreBackup)
          .map(StackGresShardedBackup::getStatus)
          .map(StackGresShardedBackupStatus::getProcess)
          .map(StackGresShardedBackupProcess::getStatus)
          .map(ShardedBackupStatus.COMPLETED.status()::equals)
          .map(completed -> !completed)
          .orElse(true)) {
        throw new IllegalArgumentException("Cannot restore from " + StackGresShardedBackup.KIND + " "
            + backupName + " because it's not Completed");
      }

      int clusters = 1 + cluster.getSpec().getShards().getClusters();
      var sgBackups = Optional.of(restoreBackup)
          .map(StackGresShardedBackup::getStatus)
          .map(StackGresShardedBackupStatus::getSgBackups)
          .orElse(null);
      if (!Optional.ofNullable(sgBackups)
          .map(list -> list.size() == clusters)
          .orElse(false)) {
        throw new IllegalArgumentException(
            "In " + StackGresShardedBackup.KIND + " " + backupName
            + " sgBackups must be an array of size " + clusters
            + " (the coordinator plus the number of shards)"
            + " but was " + Optional.ofNullable(sgBackups)
            .map(List::size)
            .orElse(null));
      }

      String backupMajorVersion = restoreBackup.getStatus()
          .getBackupInformation().getPostgresVersion().split("\\.")[0];
      String givenPgVersion = cluster.getSpec()
          .getPostgres().getVersion();
      String givenMajorVersion = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .getMajorVersion(givenPgVersion);

      if (!backupMajorVersion.equals(givenMajorVersion)) {
        throw new IllegalArgumentException(
            "Cannot restore from " + StackGresShardedBackup.KIND + " " + backupName
            + " because it has been created from a postgres instance"
            + " with version " + backupMajorVersion);
      }
      if (cluster.getStatus() == null) {
        cluster.setStatus(new StackGresShardedClusterStatus());
      }
      cluster.getStatus().setSgBackups(sgBackups);
    }
    StackGresCluster coordinator = getCoordinatorCluster(cluster);

    List<StackGresCluster> shards = getShardsClusters(cluster);

    Optional<Endpoints> coordinatorPrimaryEndpoints = endpointsFinder
        .findByNameAndNamespace(
            PatroniUtil.readWriteName(coordinator), clusterNamespace);
    List<Endpoints> shardsPrimaryEndpoints = shards.stream()
        .map(shard -> endpointsFinder
            .findByNameAndNamespace(
                PatroniUtil.readWriteName(shard), clusterNamespace))
        .flatMap(Optional::stream)
        .toList();

    final Set<String> clusterBackupNamespaces = getClusterBackupNamespaces(clusterNamespace);

    final Credentials credentials = getCredentials(clusterNamespace, cluster.getSpec());

    final var userPasswordForBinding = getUserPasswordServiceBindingFromSecret(clusterNamespace,
        cluster.getSpec());

    final PostgresSsl postgresSsl = getPostgresSsl(clusterNamespace, cluster);

    final List<Tuple2<String, String>> shardingSphereAuthorityUsers =
        Optional.of(cluster.getSpec().getCoordinator().getConfigurationsForCoordinator())
        .map(StackGresShardedClusterCoordinatorConfigurations::getShardingSphere)
        .map(StackGresShardedClusterShardingSphere::getAuthority)
        .map(StackGresShardedClusterShardingSphereAuthority::getUsers)
        .stream()
        .flatMap(List::stream)
        .map(user -> Tuple.tuple(
            Optional.of(secretFinder
                .findByNameAndNamespace(user.getUser().getName(), clusterNamespace)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Secret " + user.getUser().getName() + " not found for ShardingSphere authority")))
            .map(secret -> secret.getData().get(user.getUser().getKey()))
            .map(ResourceUtil::decodeSecret)
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + user.getUser().getName() + " do not contains key "
                    + user.getUser().getKey() + " for ShardingSphere authority")),
            Optional.of(secretFinder
                .findByNameAndNamespace(user.getPassword().getName(), clusterNamespace)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Secret " + user.getPassword().getName() + " not found for ShardingSphere authority")))
            .map(secret -> secret.getData().get(user.getPassword().getKey()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + user.getPassword().getName() + " do not contains key "
                    + user.getPassword().getKey() + " for ShardingSphere authority"))))
        .toList();

    StackGresShardedClusterContext context = ImmutableStackGresShardedClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .config(config)
        .source(cluster)
        .coordinatorProfile(coordinatorProfile)
        .coordinatorPostgresConfig(coordinatorPostgresConfig)
        .coordinatorPoolingConfig(coordinatorPoolingConfig)
        .shardsProfile(shardsProfile)
        .shardsPostgresConfig(shardsPostgresConfig)
        .shardsPoolingConfig(shardsPoolingConfig)
        .coordinator(coordinator)
        .shards(shards)
        .coordinatorPrimaryEndpoints(coordinatorPrimaryEndpoints)
        .shardsPrimaryEndpoints(shardsPrimaryEndpoints)
        .clusterBackupNamespaces(clusterBackupNamespaces)
        .databaseSecret(databaseSecret)
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
        .shardingSphereAuthorityUsers(shardingSphereAuthorityUsers)
        .build();

    return discoverer.generateResources(context);
  }

  private List<StackGresCluster> getShardsClusters(
      StackGresShardedCluster cluster) {

    return IntStream.range(0, cluster.getSpec().getShards().getClusters())
        .mapToObj(index -> getShardsCluster(cluster, index))
        .toList();
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
      final StackGresShardedClusterSpec spec) {
    final Credentials credentials;

    credentials = getCredentialsFromConfig(clusterNamespace, spec);

    return credentials;
  }

  private Credentials getCredentialsFromConfig(final String clusterNamespace,
      final StackGresShardedClusterSpec spec) {
    final Credentials configuredUsers;
    final var users =
        Optional.ofNullable(spec)
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getCredentials)
        .map(StackGresClusterCredentials::getUsers);
    final var patroni =
        Optional.ofNullable(spec)
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getCredentials)
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

  private Optional<String> getUserPasswordServiceBindingFromSecret(final String clusterNamespace,
      final StackGresShardedClusterSpec spec) {
    final var serviceBindingConfig = Optional.ofNullable(spec)
        .map(StackGresShardedClusterSpec::getConfigurations)
        .map(StackGresShardedClusterConfigurations::getBinding);
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
      final StackGresShardedCluster cluster) {
    var ssl = Optional.ofNullable(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getPostgres)
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
                  CERTIFICATE_KEY, postgresSslSecretName(cluster))),
          getSecretAndKey(clusterNamespace, ssl,
              s -> new SecretKeySelector(
                  PRIVATE_KEY_KEY, postgresSslSecretName(cluster))));
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
    return shardedBackupScanner.getResources()
        .stream()
        .map(Optional::of)
        .filter(backup -> backup
            .map(StackGresShardedBackup::getSpec)
            .map(StackGresShardedBackupSpec::getSgShardedCluster)
            .map(StackGresUtil::isRelativeIdNotInSameNamespace)
            .orElse(false))
        .map(backup -> backup
            .map(StackGresShardedBackup::getMetadata)
            .map(ObjectMeta::getNamespace))
        .flatMap(Optional::stream)
        .filter(Predicate.not(clusterNamespace::equals))
        .collect(Collectors.groupingBy(Function.identity()))
        .keySet();
  }

  private StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
    switch (StackGresShardingType.fromString(cluster.getSpec().getType())) {
      case CITUS:
        return StackGresShardedClusterForCitusUtil.getCoordinatorCluster(cluster);
      case DDP:
        return StackGresShardedClusterForDdpUtil.getCoordinatorCluster(cluster);
      case SHARDING_SPHERE:
        return StackGresShardedClusterForShardingSphereUtil.getCoordinatorCluster(cluster);
      default:
        throw new UnsupportedOperationException(
            "Sharding technology " + cluster.getSpec().getType() + " not implemented");
    }
  }

  private StackGresCluster getShardsCluster(StackGresShardedCluster cluster, int index) {
    switch (StackGresShardingType.fromString(cluster.getSpec().getType())) {
      case CITUS:
        return StackGresShardedClusterForCitusUtil.getShardsCluster(cluster, index);
      case DDP:
        return StackGresShardedClusterForDdpUtil.getShardsCluster(cluster, index);
      case SHARDING_SPHERE:
        return StackGresShardedClusterForShardingSphereUtil.getShardsCluster(cluster, index);
      default:
        throw new UnsupportedOperationException(
            "Sharding technology " + cluster.getSpec().getType() + " not implemented");
    }
  }

}

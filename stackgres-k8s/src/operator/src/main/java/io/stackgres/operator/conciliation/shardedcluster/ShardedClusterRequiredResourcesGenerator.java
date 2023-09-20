/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterForCitusUtil.CERTIFICATE_KEY;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.PRIVATE_KEY_KEY;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.getCoordinatorCluster;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.getShardsCluster;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.postgresSslSecretName;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings(value = "PMD.TooManyStaticImports")
@ApplicationScoped
public class ShardedClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedClusterRequiredResourcesGenerator.class);

  private final Supplier<VersionInfo> kubernetesVersionSupplier;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final ResourceFinder<Secret> secretFinder;

  private final ResourceFinder<Endpoints> endpointsFinder;

  private final ResourceGenerationDiscoverer<StackGresShardedClusterContext> discoverer;

  @Inject
  public ShardedClusterRequiredResourcesGenerator(
      Supplier<VersionInfo> kubernetesVersionSupplier,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      ResourceFinder<Secret> secretFinder,
      ResourceFinder<Endpoints> endpointsFinder,
      ResourceGenerationDiscoverer<StackGresShardedClusterContext> discoverer) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
    this.postgresConfigFinder = postgresConfigFinder;
    this.poolingConfigFinder = poolingConfigFinder;
    this.profileFinder = profileFinder;
    this.secretFinder = secretFinder;
    this.endpointsFinder = endpointsFinder;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedCluster config) {
    final ObjectMeta metadata = config.getMetadata();
    final String clusterName = metadata.getName();
    final String clusterNamespace = metadata.getNamespace();

    VersionInfo kubernetesVersion = kubernetesVersionSupplier.get();

    Optional<Secret> databaseSecret = secretFinder
        .findByNameAndNamespace(clusterName, clusterNamespace);

    StackGresPostgresConfig coordinatorConfig = postgresConfigFinder.findByNameAndNamespace(
        config.getSpec().getCoordinator().getConfigurations().getSgPostgresConfig(),
        clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Coordinator of SGShardedCluster "
                + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresPostgresConfig.KIND
                + " " + config.getSpec().getCoordinator().getConfigurations()
                .getSgPostgresConfig()));
    StackGresClusterContext coordinatorContext = getCoordinatorContext(
        clusterName, clusterNamespace, config, kubernetesVersion, databaseSecret);

    List<StackGresClusterContext> shardsContexts = getShardsContexts(
        clusterName, clusterNamespace, config, kubernetesVersion, databaseSecret);

    Optional<Endpoints> coordinatorPrimaryEndpoints = endpointsFinder
        .findByNameAndNamespace(
            PatroniUtil.readWriteName(coordinatorContext.getCluster()), clusterNamespace);
    List<Endpoints> shardsPrimaryEndpoints = shardsContexts.stream()
        .map(shardsContext -> endpointsFinder
            .findByNameAndNamespace(
                PatroniUtil.readWriteName(shardsContext.getCluster()), clusterNamespace))
        .flatMap(Optional::stream)
        .toList();

    final Credentials credentials = getCredentials(clusterNamespace, config.getSpec());

    final var userPasswordForBinding = getUserPasswordServiceBindingFromSecret(clusterNamespace,
        config.getSpec());

    final PostgresSsl postgresSsl = getPostgresSsl(clusterNamespace, config);

    StackGresShardedClusterContext context = ImmutableStackGresShardedClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(config)
        .databaseSecret(databaseSecret)
        .coordinatorConfig(coordinatorConfig)
        .coordinator(coordinatorContext)
        .shards(shardsContexts)
        .coordinatorPrimaryEndpoints(coordinatorPrimaryEndpoints)
        .shardsPrimaryEndpoints(shardsPrimaryEndpoints)
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

  private StackGresClusterContext getCoordinatorContext(
      final String clusterName,
      final String clusterNamespace,
      StackGresShardedCluster config,
      VersionInfo kubernetesVersion,
      Optional<Secret> databaseSecret) {
    final StackGresShardedClusterSpec spec = config.getSpec();
    final StackGresShardedClusterCoordinator coordinator = spec.getCoordinator();
    final StackGresClusterConfigurations configuration = coordinator.getConfigurations();
    final StackGresPostgresConfig pgConfig = postgresConfigFinder
        .findByNameAndNamespace(configuration.getSgPostgresConfig(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Coordinator of SGShardedCluster " + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresPostgresConfig.KIND
                + " " + configuration.getSgPostgresConfig()));

    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(coordinator.getSgInstanceProfile(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Coordinator of SGShardedCluster " + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresProfile.KIND
                + " " + coordinator.getSgInstanceProfile()));

    final Optional<StackGresPoolingConfig> pooling = Optional
        .ofNullable(configuration.getSgPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(poolingConfigName, clusterNamespace));

    StackGresCluster coordinatorCluster = getCoordinatorCluster(config);

    StackGresClusterContext coordinatorContext = ImmutableStackGresClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(coordinatorCluster)
        .postgresConfig(pgConfig)
        .profile(profile)
        .poolingConfig(pooling)
        .databaseSecret(databaseSecret)
        .build();
    return coordinatorContext;
  }

  private List<StackGresClusterContext> getShardsContexts(
      final String clusterName,
      final String clusterNamespace,
      StackGresShardedCluster config,
      VersionInfo kubernetesVersion,
      Optional<Secret> databaseSecret) {
    final StackGresShardedClusterSpec spec = config.getSpec();
    final StackGresShardedClusterShards shards = spec.getShards();
    final StackGresClusterConfigurations configuration = shards.getConfigurations();
    final StackGresPostgresConfig pgConfig = postgresConfigFinder
        .findByNameAndNamespace(configuration.getSgPostgresConfig(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Shards of SGShardedCluster " + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresPostgresConfig.KIND
                + " " + configuration.getSgPostgresConfig()));

    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(shards.getSgInstanceProfile(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "Shards of SGShardedCluster " + clusterNamespace + "." + clusterName
                + " have a non existent " + StackGresProfile.KIND
                + " " + shards.getSgInstanceProfile()));

    final Optional<StackGresPoolingConfig> pooling = Optional
        .ofNullable(configuration.getSgPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(poolingConfigName, clusterNamespace));

    return IntStream.range(0, config.getSpec().getShards().getClusters())
        .mapToObj(index -> getShardContext(
            config, kubernetesVersion, databaseSecret,
            pgConfig, profile, pooling, index))
        .toList();
  }

  private StackGresClusterContext getShardContext(
      StackGresShardedCluster config,
      VersionInfo kubernetesVersion,
      Optional<Secret> databaseSecret,
      StackGresPostgresConfig pgConfig,
      StackGresProfile profile,
      Optional<StackGresPoolingConfig> pooling,
      int index) {
    StackGresCluster shardsCluster = getShardsCluster(config, index);

    StackGresClusterContext shardsContext = ImmutableStackGresClusterContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(shardsCluster)
        .postgresConfig(pgConfig)
        .profile(profile)
        .poolingConfig(pooling)
        .databaseSecret(databaseSecret)
        .build();
    return shardsContext;
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

}

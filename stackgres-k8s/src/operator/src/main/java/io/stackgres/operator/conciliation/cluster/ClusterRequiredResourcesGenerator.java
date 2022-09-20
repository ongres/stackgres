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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromInstance;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromUsers;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
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
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterRequiredResourcesGenerator.class);

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

    final Optional<String> superuserUsername;
    final Optional<String> superuserPassword;
    final Optional<String> replicationUsername;
    final Optional<String> replicationPassword;
    final Optional<String> authenticatorUsername;
    final Optional<String> authenticatorPassword;

    if (Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getSgCluster)
        .isPresent()) {
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

      superuserUsername = Optional.of(
          Optional.of(replicateFromClusterSecret)
          .map(Secret::getData)
          .map(data -> data.get(StackGresPasswordKeys.SUPERUSER_USERNAME_ENV))
          .map(ResourceUtil::decodeSecret)
          .orElseThrow(() -> new IllegalArgumentException(
              "Superuser username key " + StackGresPasswordKeys.SUPERUSER_USERNAME_ENV
              + " was not found in secret " + secretName)));
      superuserPassword = Optional.of(
          Optional.of(replicateFromClusterSecret)
          .map(Secret::getData)
          .map(data -> data.get(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV))
          .map(ResourceUtil::decodeSecret)
          .orElseThrow(() -> new IllegalArgumentException(
              "Superuser password key " + StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV
              + " was not found in secret " + secretName)));

      replicationUsername = Optional.of(
          Optional.of(replicateFromClusterSecret)
          .map(Secret::getData)
          .map(data -> data.get(StackGresPasswordKeys.REPLICATION_USERNAME_ENV))
          .map(ResourceUtil::decodeSecret)
          .orElseThrow(() -> new IllegalArgumentException(
              "Replication username key " + StackGresPasswordKeys.REPLICATION_USERNAME_ENV
              + " was not found in secret " + secretName)));
      replicationPassword = Optional.of(
          Optional.of(replicateFromClusterSecret)
          .map(Secret::getData)
          .map(data -> data.get(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV))
          .map(ResourceUtil::decodeSecret)
          .orElseThrow(() -> new IllegalArgumentException(
              "Replication password key " + StackGresPasswordKeys.REPLICATION_PASSWORD_ENV
              + " was not found in secret " + secretName)));

      authenticatorUsername = Optional.of(
          Optional.of(replicateFromClusterSecret)
          .map(Secret::getData)
          .map(data -> data.get(StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV))
          .map(ResourceUtil::decodeSecret)
          .orElseThrow(() -> new IllegalArgumentException(
              "Authenticator username key " + StackGresPasswordKeys.AUTHENTICATOR_USERNAME_ENV
              + " was not found in secret " + secretName)));
      authenticatorPassword = Optional.of(
          Optional.of(replicateFromClusterSecret)
          .map(Secret::getData)
          .map(data -> data.get(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV))
          .map(ResourceUtil::decodeSecret)
          .orElseThrow(() -> new IllegalArgumentException(
              "Authenticator password key " + StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV
              + " was not found in secret " + secretName)));
    } else if (Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getInstance)
        .map(StackGresClusterReplicateFromInstance::getExternal)
        .isPresent()) {
      final var replicateFromUsers =
          Optional.ofNullable(spec)
          .map(StackGresClusterSpec::getReplicateFrom)
          .map(StackGresClusterReplicateFrom::getUsers);

      superuserUsername = replicateFromUsers
          .map(StackGresClusterReplicateFromUsers::getSuperuser)
          .map(StackGresClusterReplicateFromUserSecretKeyRef::getUsername)
          .map(secretKeySelector -> secretFinder
              .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
              .map(secret -> Optional.ofNullable(secret.getData())
                  .map(data -> data.get(secretKeySelector.getKey()))
                  .map(ResourceUtil::decodeSecret)
                  .orElseThrow(() -> new IllegalArgumentException(
                      "Superuser username key " + secretKeySelector.getKey()
                      + " was not found in secret " + secretKeySelector.getName())))
              .orElseThrow(() -> new IllegalArgumentException(
                  "Superuser username secret " + secretKeySelector.getName()
                  + " was not found")));
      superuserPassword = replicateFromUsers
          .map(StackGresClusterReplicateFromUsers::getSuperuser)
          .map(StackGresClusterReplicateFromUserSecretKeyRef::getPassword)
          .map(secretKeySelector -> secretFinder
              .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
              .map(secret -> Optional.ofNullable(secret.getData())
                  .map(data -> data.get(secretKeySelector.getKey()))
                  .map(ResourceUtil::decodeSecret)
                  .orElseThrow(() -> new IllegalArgumentException(
                      "Superuser password key " + secretKeySelector.getKey()
                      + " was not found in secret " + secretKeySelector.getName())))
              .orElseThrow(() -> new IllegalArgumentException(
                  "Superuser password secret " + secretKeySelector.getName()
                  + " was not found")));

      replicationUsername = replicateFromUsers
          .map(StackGresClusterReplicateFromUsers::getReplication)
          .map(StackGresClusterReplicateFromUserSecretKeyRef::getUsername)
          .map(secretKeySelector -> secretFinder
              .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
              .map(secret -> Optional.ofNullable(secret.getData())
                  .map(data -> data.get(secretKeySelector.getKey()))
                  .map(ResourceUtil::decodeSecret)
                  .orElseThrow(() -> new IllegalArgumentException(
                      "Replication username key " + secretKeySelector.getKey()
                      + " was not found in secret " + secretKeySelector.getName())))
              .orElseThrow(() -> new IllegalArgumentException(
                  "Replication username secret " + secretKeySelector.getName()
                  + " was not found")));
      replicationPassword = replicateFromUsers
          .map(StackGresClusterReplicateFromUsers::getReplication)
          .map(StackGresClusterReplicateFromUserSecretKeyRef::getPassword)
          .map(secretKeySelector -> secretFinder
              .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
              .map(secret -> Optional.ofNullable(secret.getData())
                  .map(data -> data.get(secretKeySelector.getKey()))
                  .map(ResourceUtil::decodeSecret)
                  .orElseThrow(() -> new IllegalArgumentException(
                      "Replication password key " + secretKeySelector.getKey()
                      + " was not found in secret " + secretKeySelector.getName())))
              .orElseThrow(() -> new IllegalArgumentException(
                  "Replication password secret " + secretKeySelector.getName()
                  + " was not found")));

      authenticatorUsername = replicateFromUsers
          .map(StackGresClusterReplicateFromUsers::getAuthenticator)
          .map(StackGresClusterReplicateFromUserSecretKeyRef::getUsername)
          .map(secretKeySelector -> secretFinder
              .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
              .map(secret -> Optional.ofNullable(secret.getData())
                  .map(data -> data.get(secretKeySelector.getKey()))
                  .map(ResourceUtil::decodeSecret)
                  .orElseThrow(() -> new IllegalArgumentException(
                      "Authenticator username key " + secretKeySelector.getKey()
                      + " was not found in secret " + secretKeySelector.getName())))
              .orElseThrow(() -> new IllegalArgumentException(
                  "Authenticator username secret " + secretKeySelector.getName()
                  + " was not found")));
      authenticatorPassword = replicateFromUsers
          .map(StackGresClusterReplicateFromUsers::getAuthenticator)
          .map(StackGresClusterReplicateFromUserSecretKeyRef::getPassword)
          .map(secretKeySelector -> secretFinder
              .findByNameAndNamespace(secretKeySelector.getName(), clusterNamespace)
              .map(secret -> Optional.ofNullable(secret.getData())
                  .map(data -> data.get(secretKeySelector.getKey()))
                  .map(ResourceUtil::decodeSecret)
                  .orElseThrow(() -> new IllegalArgumentException(
                      "Authenticator password key " + secretKeySelector.getKey()
                      + " was not found in secret " + secretKeySelector.getName())))
              .orElseThrow(() -> new IllegalArgumentException(
                  "Authenticator password secret " + secretKeySelector.getName()
                  + " was not found")));
    } else {
      superuserUsername = Optional.empty();
      superuserPassword = Optional.empty();
      replicationUsername = Optional.empty();
      replicationPassword = Optional.empty();
      authenticatorUsername = Optional.empty();
      authenticatorPassword = Optional.empty();
    }

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
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
        .superuserUsername(superuserUsername)
        .superuserPassword(superuserPassword)
        .replicationUsername(replicationUsername)
        .replicationPassword(replicationPassword)
        .authenticatorUsername(authenticatorUsername)
        .authenticatorPassword(authenticatorPassword)
        .build();

    return decorator.decorateResources(context);
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

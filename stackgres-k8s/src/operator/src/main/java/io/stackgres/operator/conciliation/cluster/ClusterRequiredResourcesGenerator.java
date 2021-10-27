/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.patroni.StackGresRandomPasswordKeys;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.customresource.prometheus.PrometheusConfig;
import io.stackgres.operator.customresource.prometheus.PrometheusConfigSpec;
import io.stackgres.operator.customresource.prometheus.PrometheusInstallation;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterRequiredResourcesGenerator.class);

  private final CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceScanner<StackGresBackup> backupScanner;

  private final ResourceFinder<Secret> secretFinder;

  private final CustomResourceScanner<PrometheusConfig> prometheusScanner;

  private final OperatorPropertyContext operatorContext;

  private final RequiredResourceDecorator<StackGresClusterContext> decorator;

  @Inject
  public ClusterRequiredResourcesGenerator(
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceScanner<StackGresBackup> backupScanner,
      ResourceFinder<Secret> secretFinder,
      CustomResourceScanner<PrometheusConfig> prometheusScanner,
      OperatorPropertyContext operatorContext,
      RequiredResourceDecorator<StackGresClusterContext> decorator) {
    this.backupConfigFinder = backupConfigFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.poolingConfigFinder = poolingConfigFinder;
    this.profileFinder = profileFinder;
    this.backupScanner = backupScanner;
    this.secretFinder = secretFinder;
    this.prometheusScanner = prometheusScanner;
    this.operatorContext = operatorContext;
    this.decorator = decorator;
  }

  private static PrometheusInstallation toPrometheusInstallation(PrometheusConfig pc) {
    Map<String, String> matchLabels = Optional.ofNullable(pc.getSpec())
        .map(PrometheusConfigSpec::getServiceMonitorSelector)
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
    final StackGresPostgresConfig clusterPgConfig = postgresConfigFinder
        .findByNameAndNamespace(clusterConfiguration.getPostgresConfig(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + clusterNamespace + "/" + clusterName
                + " have a non existent SGPostgresConfig postgresconf"));

    final StackGresProfile clusterProfile = profileFinder
        .findByNameAndNamespace(spec.getResourceProfile(), clusterNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + clusterNamespace + "/" + clusterName + " have a non existent "
                + StackGresProfile.KIND + " " + spec.getResourceProfile()));
    final Optional<StackGresBackupConfig> backupConfig = Optional
        .ofNullable(clusterConfiguration.getBackupConfig())
        .flatMap(backupConfigName -> backupConfigFinder
            .findByNameAndNamespace(backupConfigName, clusterNamespace));

    final Optional<StackGresPoolingConfig> clusterPooling = Optional
        .ofNullable(clusterConfiguration.getConnectionPoolingConfig())
        .flatMap(poolingConfigName -> poolingConfigFinder
            .findByNameAndNamespace(poolingConfigName, clusterNamespace));

    // The logic here is return an empty if there is no StackGresClusterRestore
    // if the uid match return that backup and if the backup is not found return a dummy object,
    // the dummy object is for the cases when the restore is done but the backup is deleted.
    // This way the template don't change after a reconciliation cycle requiring a restart.
    final Optional<StackGresBackup> restoreBackup = Optional
        .ofNullable(config.getSpec().getInitData())
        .map(StackGresClusterInitData::getRestore)
        .flatMap(restore -> backupScanner.getResources().stream()
            .filter(backup -> backup.getMetadata().getUid()
                .equals(restore.getFromBackup().getUid()))
            .findFirst()
            .or(() -> Optional.of(new StackGresBackup())));

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .source(config)
        .postgresConfig(clusterPgConfig)
        .stackGresProfile(clusterProfile)
        .backupConfig(backupConfig)
        .poolingConfig(clusterPooling)
        .restoreBackup(restoreBackup)
        .prometheus(getPrometheus(config))
        .internalScripts(Seq.of(getPostgresExporterInitScript())
            .append(Seq.of(
                getBabelfishUserScript(config),
                getBabelfishDatabaseScript(),
                getBabelfishInitDatabaseScript())
                .filter(script -> StackGresUtil.getPostgresFlavorComponent(config).getName()
                    .equals(StackGresComponent.BABELFISH.getName()))))
        .databaseCredentials(secretFinder.findByNameAndNamespace(clusterName, clusterNamespace))
        .build();

    return decorator.decorateResources(context);
  }

  private StackGresClusterScriptEntry getPostgresExporterInitScript() {
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("prometheus-postgres-exporter-init");
    script.setDatabase("postgres");
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(ClusterRequiredResourcesGenerator.class.getResource(
            "/prometheus-postgres-exporter/init.sql"),
            StandardCharsets.UTF_8)
        .read()).get());
    return script;
  }

  private StackGresClusterScriptEntry getBabelfishUserScript(StackGresCluster cluster) {
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("babelfish-user");
    script.setDatabase("postgres");
    script.setScriptFrom(new StackGresClusterScriptFrom());
    script.getScriptFrom().setSecretKeyRef(new SecretKeySelector(
        StackGresRandomPasswordKeys.BABELFISH_CREATE_USER_SQL_KEY, PatroniSecret.name(cluster)));
    return script;
  }

  private StackGresClusterScriptEntry getBabelfishDatabaseScript() {
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("babelfish-database");
    script.setDatabase("postgres");
    script.setScript("CREATE DATABASE babelfish OWNER babelfish");
    return script;
  }

  private StackGresClusterScriptEntry getBabelfishInitDatabaseScript() {
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("babelfish-init");
    script.setDatabase("babelfish");
    script.setScript(Unchecked.supplier(() -> Resources
        .asCharSource(ClusterRequiredResourcesGenerator.class.getResource(
            "/babelfish/init.sql"),
            StandardCharsets.UTF_8)
        .read()).get());
    return script;
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
              .collect(Collectors.toUnmodifiableList()))
          .map(installations -> new Prometheus(!installations.isEmpty(), installations));

    } else {
      return Optional.of(new Prometheus(false, null));
    }
  }

}

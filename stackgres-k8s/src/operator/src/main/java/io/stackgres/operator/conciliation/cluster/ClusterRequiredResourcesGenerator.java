/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.cluster.DecoratorDiscoverer;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.customresource.prometheus.PrometheusConfig;
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

  private final ResourceGenerationDiscoverer<StackGresClusterContext> generators;

  private final CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  private final CustomResourceScanner<StackGresBackup> backupScanner;

  private final LabelFactory<StackGresCluster> labelFactory;

  private final CustomResourceScanner<StackGresDbOps> dbOpsScanner;

  private final DecoratorDiscoverer<StackGresCluster> decoratorDiscoverer;

  private final CustomResourceScanner<PrometheusConfig> prometheusScanner;

  private final OperatorPropertyContext operatorContext;

  @Inject
  public ClusterRequiredResourcesGenerator(
      ResourceGenerationDiscoverer<StackGresClusterContext> generators,
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceScanner<StackGresBackup> backupScanner,
      LabelFactory<StackGresCluster> labelFactory,
      CustomResourceScanner<StackGresDbOps> dbOpsScanner,
      DecoratorDiscoverer<StackGresCluster> decoratorDiscoverer,
      CustomResourceScanner<PrometheusConfig> prometheusScanner,
      OperatorPropertyContext operatorContext) {
    this.generators = generators;
    this.backupConfigFinder = backupConfigFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.poolingConfigFinder = poolingConfigFinder;
    this.profileFinder = profileFinder;
    this.backupScanner = backupScanner;
    this.labelFactory = labelFactory;
    this.dbOpsScanner = dbOpsScanner;
    this.decoratorDiscoverer = decoratorDiscoverer;
    this.prometheusScanner = prometheusScanner;
    this.operatorContext = operatorContext;
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

    Optional<StackGresClusterRestore> restoreConfig = Optional
        .ofNullable(config.getSpec().getInitData())
        .map(StackGresClusterInitData::getRestore);

    final Optional<StackGresBackup> restoreBackup;
    if (restoreConfig.isEmpty()) {
      restoreBackup = Optional.empty();
    } else {
      restoreBackup = restoreConfig.map(restore -> {
        final List<StackGresBackup> backups = backupScanner.getResources();
        return backups.stream()
            .filter(backup -> backup.getMetadata().getUid().equals(restore.getFromBackup().getUid()))
            .peek(backup -> {
              Preconditions.checkNotNull(backup.getStatus(),
                  "Backup is " + BackupPhase.RUNNING.label());
              Preconditions.checkNotNull(backup.getStatus().getProcess(),
                  "Backup is " + BackupPhase.RUNNING.label());
              Preconditions.checkArgument(backup.getStatus().getProcess().getStatus()
                      .equals(BackupPhase.COMPLETED.label()),
                  "Backup is " + backup.getStatus().getProcess().getStatus());
            }).findFirst().orElseThrow(() -> new IllegalArgumentException(
                "SGCluster " + clusterNamespace + "/" + clusterName
                    + " have an invalid restore backup Uid"));
      });
    }

    List<StackGresBackup> backups = getBackups(config);

    StackGresClusterContext context = ImmutableStackGresClusterContext.builder()
        .source(config)
        .postgresConfig(clusterPgConfig)
        .stackGresProfile(clusterProfile)
        .backupConfig(backupConfig)
        .poolingConfig(clusterPooling)
        .backups(backups)
        .restoreBackup(restoreBackup)
        .addAllDbOps(getDbOps(config))
        .prometheus(getPrometheus(config))
        .internalScripts(Seq.of(getPostgresExporterInitScript()))
        .patroniClusterLabels(labelFactory.patroniClusterLabels(config))
        .clusterScope(labelFactory.clusterScope(config))
        .build();

    final List<HasMetadata> resources = generators.getResourceGenerators(context)
        .stream().flatMap(generator -> generator.generateResource(context))
        .collect(Collectors.toUnmodifiableList());

    List<Decorator<StackGresCluster>> decorators = decoratorDiscoverer.discoverDecorator(config);

    decorators.forEach(decorator -> decorator.decorate(config, List.of(), resources));
    return resources;
  }

  private List<StackGresDbOps> getDbOps(StackGresCluster cluster) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    return dbOpsScanner.getResources(namespace)
        .stream()
        .filter(dbOps -> dbOps.getSpec().getSgCluster().equals(name))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<StackGresBackup> getBackups(StackGresCluster cluster) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    return backupScanner.getResources(namespace)
        .stream()
        .filter(backup -> backup.getSpec().getSgCluster().equals(name))
        .collect(Collectors.toUnmodifiableList());
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

  public Optional<Prometheus> getPrometheus(StackGresCluster cluster) {
    boolean isAutobindAllowed = operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND);

    boolean isPrometheusAutobindEnabled = Optional.ofNullable(cluster.getSpec()
        .getPrometheusAutobind()).orElse(false);

    if (isAutobindAllowed && isPrometheusAutobindEnabled) {
      LOGGER.trace("Prometheus auto bind enabled, looking for prometheus installations");

      List<PrometheusInstallation> prometheusInstallations = prometheusScanner.findResources()
          .map(pcs -> pcs.stream()
              .filter(pc -> pc.getSpec().getServiceMonitorSelector().getMatchLabels() != null)
              .filter(pc -> !pc.getSpec().getServiceMonitorSelector().getMatchLabels().isEmpty())
              .map(pc -> {

                PrometheusInstallation pi = new PrometheusInstallation();
                pi.setNamespace(pc.getMetadata().getNamespace());

                ImmutableMap<String, String> matchLabels = ImmutableMap
                    .copyOf(pc.getSpec().getServiceMonitorSelector().getMatchLabels());

                pi.setMatchLabels(matchLabels);
                return pi;

              })
              .collect(Collectors.toList()))
          .orElse(new ArrayList<>());

      if (!prometheusInstallations.isEmpty()) {
        return Optional.of(new Prometheus(true, prometheusInstallations));
      } else {
        return Optional.of(new Prometheus(false, null));
      }
    } else {
      return Optional.of(new Prometheus(false, null));
    }
  }

}

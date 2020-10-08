/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.storages.AwsCredentials;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.AzureBlobStorageCredentials;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.crd.storages.GoogleCloudSecretKeySelector;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.cluster.factory.Cluster;
import io.stackgres.operator.cluster.factory.ClusterStatefulSet;
import io.stackgres.operator.common.ImmutableStackGresUserClusterContext;
import io.stackgres.operator.common.ImmutableStackGresUserGeneratorContext;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.SidecarEntry;
import io.stackgres.operator.common.StackGresBackupContext;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresRestoreContext;
import io.stackgres.operator.common.StackGresUserClusterContext;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.customresource.prometheus.PrometheusConfig;
import io.stackgres.operator.customresource.prometheus.PrometheusInstallation;
import io.stackgres.operator.resource.ClusterResourceHandlerSelector;
import io.stackgres.operator.resource.ClusterSidecarFinder;
import io.stackgres.operator.sidecars.fluentbit.FluentBit;
import io.stackgres.operator.sidecars.pgexporter.PostgresExporter;
import io.stackgres.operator.sidecars.pgutils.PostgresUtil;
import io.stackgres.operator.sidecars.pooling.PgPooling;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliator;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterReconciliationCycle
    extends StackGresReconciliationCycle<StackGresClusterContext, StackGresCluster,
      ClusterResourceHandlerSelector> {

  private final ClusterSidecarFinder sidecarFinder;
  private final Cluster cluster;
  private final ClusterStatusManager statusManager;
  private final EventController eventController;
  private final OperatorPropertyContext operatorContext;
  private final CustomResourceFinder<StackGresProfile> profileFinder;
  private final CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;
  private final CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;
  private final CustomResourceScanner<StackGresBackup> backupScanner;
  private final ResourceFinder<Secret> secretFinder;
  private final CustomResourceScanner<PrometheusConfig> prometheusScanner;

  private final LabelFactory<StackGresCluster> labelFactory;

  /**
   * Create a {@code ClusterReconciliationCycle} instance.
   */
  @Inject
  public ClusterReconciliationCycle(
      KubernetesClientFactory kubClientFactory,
      ClusterSidecarFinder sidecarFinder, Cluster cluster,
      ClusterResourceHandlerSelector handlerSelector,
      ClusterStatusManager statusManager, EventController eventController,
      ObjectMapperProvider objectMapperProvider,
      OperatorPropertyContext operatorContext,
      LabelFactory<StackGresCluster> labelFactory,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      CustomResourceFinder<StackGresProfile> profileFinder,
      CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder,
      CustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      CustomResourceScanner<StackGresBackup> backupScanner, ResourceFinder<Secret> secretFinder,
      CustomResourceScanner<PrometheusConfig> prometheusScanner) {
    super("Cluster", kubClientFactory::create, StackGresClusterContext::getCluster,
        handlerSelector, objectMapperProvider.objectMapper(), clusterScanner);
    this.sidecarFinder = sidecarFinder;
    this.cluster = cluster;
    this.statusManager = statusManager;
    this.eventController = eventController;
    this.operatorContext = operatorContext;
    this.labelFactory = labelFactory;
    this.profileFinder = profileFinder;
    this.postgresConfigFinder = postgresConfigFinder;
    this.backupConfigFinder = backupConfigFinder;
    this.backupScanner = backupScanner;
    this.secretFinder = secretFinder;
    this.prometheusScanner = prometheusScanner;
  }

  public ClusterReconciliationCycle() {
    super(null, null, c -> null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.sidecarFinder = null;
    this.cluster = null;
    this.statusManager = null;
    this.eventController = null;
    this.operatorContext = null;
    this.labelFactory = null;
    this.profileFinder = null;
    this.postgresConfigFinder = null;
    this.backupConfigFinder = null;
    this.backupScanner = null;
    this.secretFinder = null;
    this.prometheusScanner = null;
  }

  @Override
  protected void onError(Exception ex) {
    try (KubernetesClient client = clientSupplier.get()) {
      eventController.sendEvent(ClusterEventReason.CLUSTER_CONFIG_ERROR,
          "StackGres Cluster reconciliation cycle failed: "
              + ex.getMessage(), client);
    }
  }

  @Override
  protected void onConfigError(StackGresClusterContext context, HasMetadata configResource,
      Exception ex) {
    try (KubernetesClient client = clientSupplier.get()) {
      statusManager.updateCondition(
          ClusterStatusCondition.CLUSTER_CONFIG_ERROR.getCondition(), context, client);
      eventController.sendEvent(ClusterEventReason.CLUSTER_CONFIG_ERROR,
          "StackGres Cluster " + configResource.getMetadata().getNamespace() + "."
              + configResource.getMetadata().getName() + " reconciliation failed: "
              + ex.getMessage(), configResource, client);
    }
  }

  @Override
  protected ImmutableList<HasMetadata> getRequiredResources(StackGresClusterContext context) {
    return ResourceGenerator.<StackGresGeneratorContext>with(
        ImmutableStackGresUserGeneratorContext.builder()
            .clusterContext(context)
            .build())
        .of(HasMetadata.class)
        .append(cluster)
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected AbstractReconciliator<StackGresClusterContext, StackGresCluster,
        ClusterResourceHandlerSelector> createReconciliator(
      KubernetesClient client, StackGresClusterContext context) {
    return ClusterReconciliator.builder()
        .withEventController(eventController)
        .withHandlerSelector(handlerSelector)
        .withStatusManager(statusManager)
        .withClient(client)
        .withObjectMapper(objectMapper)
        .withClusterContext(context)
        .build();
  }

  @Override
  protected StackGresClusterContext getContextWithExistingResourcesOnly(
      StackGresClusterContext context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly) {
    return ImmutableStackGresUserClusterContext.copyOf((StackGresUserClusterContext) context)
        .withExistingResources(existingResourcesOnly);
  }

  @Override
  protected StackGresClusterContext getContextWithExistingAndRequiredResources(
      StackGresClusterContext context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources) {
    return ImmutableStackGresUserClusterContext.copyOf((StackGresUserClusterContext) context)
        .withRequiredResources(requiredResources)
        .withExistingResources(existingResources);
  }

  @Override
  protected StackGresClusterContext mapResourceToContext(StackGresCluster resource) {
    return this.getClusterConfig(resource);
  }

  private StackGresClusterContext getClusterConfig(StackGresCluster cluster) {
    return ImmutableStackGresUserClusterContext.builder()
        .operatorContext(operatorContext)
        .cluster(cluster)
        .profile(getProfile(cluster))
        .postgresConfig(getPostgresConfig(cluster))
        .backupContext(getBackupContext(cluster))
        .sidecars(getClusterSidecars(cluster).stream()
            .map(sidecarFinder::getSidecarTransformer)
            .map(Unchecked.function(sidecar -> getSidecarEntry(cluster, sidecar)))
            .collect(ImmutableList.toImmutableList()))
        .backups(getBackups(cluster))
        .labels(labelFactory.clusterLabels(cluster))
        .clusterNamespace(labelFactory.clusterNamespace(cluster))
        .clusterName(labelFactory.clusterName(cluster))
        .clusterKey(labelFactory.getLabelMapper().clusterKey())
        .scheduledBackupKey(labelFactory.getLabelMapper().scheduledBackupKey())
        .backupKey(labelFactory.getLabelMapper().backupKey())
        .ownerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(cluster)))
        .restoreContext(getRestoreContext(cluster))
        .prometheus(getPrometheus(cluster))
        .internalScripts(ImmutableList.of(getPostgresExporterInitScript()))
        .build();
  }

  private StackGresClusterScriptEntry getPostgresExporterInitScript() {
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("prometheus-postgres-exporter-init");
    script.setDatabase("postgres");
    script.setScript(Unchecked.supplier(() -> Resources
          .asCharSource(ClusterStatefulSet.class.getResource(
              "/prometheus-postgres-exporter/init.sql"),
              StandardCharsets.UTF_8)
          .read()).get());
    return script;
  }

  private Optional<StackGresProfile> getProfile(StackGresCluster cluster) {
    final String namespace = cluster.getMetadata().getNamespace();
    return Optional.ofNullable(cluster.getSpec().getResourceProfile())
        .flatMap(profileName -> profileFinder.findByNameAndNamespace(profileName, namespace));
  }

  private Optional<StackGresPostgresConfig> getPostgresConfig(StackGresCluster cluster) {
    final String namespace = cluster.getMetadata().getNamespace();
    return Optional.ofNullable(cluster.getSpec().getConfiguration().getPostgresConfig())
        .flatMap(postgresConfigName -> postgresConfigFinder.findByNameAndNamespace(
            postgresConfigName, namespace));
  }

  private List<String> getClusterSidecars(StackGresCluster cluster) {
    List<String> sidecarsToDisable = new ArrayList<>();

    StackGresClusterPod pod = cluster.getSpec().getPod();
    if (Boolean.TRUE.equals(pod.getDisableConnectionPooling())) {
      sidecarsToDisable.add(PgPooling.class.getAnnotation(Sidecar.class).value());
    }

    if (Boolean.TRUE.equals(pod.getDisableMetricsExporter())) {
      sidecarsToDisable
          .add(PostgresExporter.class.getAnnotation(Sidecar.class).value());
    }

    if (Boolean.TRUE.equals(pod.getDisablePostgresUtil())) {
      sidecarsToDisable.add(PostgresUtil.class.getAnnotation(Sidecar.class).value());
    }

    if (Optional.ofNullable(cluster.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getDistributedLogs)
        .map(distributedLogs -> false)
        .orElse(true)) {
      sidecarsToDisable.add(FluentBit.class.getAnnotation(Sidecar.class).value());
    }

    List<String> allSidecars = sidecarFinder.getAllSidecars();

    return ImmutableList
        .copyOf(allSidecars.stream()
            .filter(s -> !sidecarsToDisable.contains(s))
            .collect(Collectors.toList()));
  }

  private <T> SidecarEntry<T> getSidecarEntry(
      StackGresCluster cluster,
      StackGresClusterSidecarResourceFactory<T> sidecar) throws Exception {
    Optional<T> sidecarConfig = sidecar.getConfig(cluster);
    return new SidecarEntry<>(sidecar, sidecarConfig);
  }

  private Optional<StackGresBackupContext> getBackupContext(StackGresCluster cluster) {
    final String namespace = cluster.getMetadata().getNamespace();
    return Optional.ofNullable(cluster.getSpec().getConfiguration().getBackupConfig())
        .flatMap(backupConfig -> backupConfigFinder.findByNameAndNamespace(backupConfig, namespace))
        .map(backupConf -> StackGresBackupContext.builder()
            .withBackupConfig(backupConf)
            .withSecrets(getBackupSecrets(namespace, backupConf.getSpec()))
            .build());
  }

  private ImmutableList<StackGresBackup> getBackups(StackGresCluster cluster) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    return backupScanner.getResources(namespace)
            .stream()
            .filter(backup -> backup.getSpec().getSgCluster().equals(name))
            .collect(ImmutableList.toImmutableList());
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

  private Optional<StackGresRestoreContext> getRestoreContext(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec().getInitData())
        .map(StackGresClusterInitData::getRestore)
        .flatMap(restore -> backupScanner.getResources()
            .stream()
            .filter(backup -> backup.getMetadata().getUid().equals(restore.getBackupUid()))
            .findAny()
            .map(backup -> {
              Preconditions.checkNotNull(backup.getStatus(),
                  "Backup is " + BackupPhase.RUNNING.label());
              Preconditions.checkNotNull(backup.getStatus().getProcess(),
                  "Backup is " + BackupPhase.RUNNING.label());
              Preconditions.checkArgument(backup.getStatus().getProcess().getStatus()
                  .equals(BackupPhase.COMPLETED.label()),
                  "Backup is " + backup.getStatus().getProcess().getStatus());
              return backup;
            })
            .map(backup -> StackGresRestoreContext.builder()
                .withRestore(restore)
                .withBackup(backup)
                .withSecrets(getBackupSecrets(backup.getMetadata().getNamespace(),
                    backup.getStatus().getBackupConfig()))
                .build()));
  }

  private ImmutableMap<String, Map<String, String>> getBackupSecrets(
      final String namespace,
      StackGresBackupConfigSpec backupConfSpec) {
    return Seq.of(
        Optional.ofNullable(backupConfSpec.getStorage().getS3())
            .map(AwsS3Storage::getAwsCredentials)
            .map(AwsCredentials::getSecretKeySelectors)
            .map(AwsSecretKeySelector::getAccessKeyId),
        Optional.ofNullable(backupConfSpec.getStorage().getS3())
            .map(AwsS3Storage::getAwsCredentials)
            .map(AwsCredentials::getSecretKeySelectors)
            .map(AwsSecretKeySelector::getSecretAccessKey),
        Optional.ofNullable(backupConfSpec.getStorage().getS3Compatible())
            .map(AwsS3CompatibleStorage::getAwsCredentials)
            .map(AwsCredentials::getSecretKeySelectors)
            .map(AwsSecretKeySelector::getAccessKeyId),
        Optional.ofNullable(backupConfSpec.getStorage().getS3Compatible())
            .map(AwsS3CompatibleStorage::getAwsCredentials)
            .map(AwsCredentials::getSecretKeySelectors)
            .map(AwsSecretKeySelector::getSecretAccessKey),
        Optional.ofNullable(backupConfSpec.getStorage().getGcs())
            .map(GoogleCloudStorage::getCredentials)
            .map(GoogleCloudCredentials::getSecretKeySelectors)
            .map(GoogleCloudSecretKeySelector::getServiceAccountJsonKey),
        Optional.ofNullable(backupConfSpec.getStorage().getAzureBlob())
            .map(AzureBlobStorage::getAzureCredentials)
            .map(AzureBlobStorageCredentials::getSecretKeySelectors)
            .map(AzureBlobSecretKeySelector::getAccount),
        Optional.ofNullable(backupConfSpec.getStorage().getAzureBlob())
            .map(AzureBlobStorage::getAzureCredentials)
            .map(AzureBlobStorageCredentials::getSecretKeySelectors)
            .map(AzureBlobSecretKeySelector::getAccessKey))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(secretKeySelector -> Tuple.tuple(secretKeySelector,
            getSecret(namespace, secretKeySelector)))
        .grouped(t -> t.v1.getName())
        .collect(ImmutableMap.toImmutableMap(
            t -> t.v1, t -> t.v2
                .collect(ImmutableMap.toImmutableMap(
                    tt -> tt.v1.getKey(), tt -> tt.v2))));
  }

  private String getSecret(String namespace, SecretKeySelector secretKeySelector) {
    return Optional.of(secretFinder.findByNameAndNamespace(secretKeySelector.getName(), namespace)
        .orElseThrow(() -> new IllegalStateException(
            "Secret " + namespace + "." + secretKeySelector.getName()
            + " not found")))
        .map(Secret::getData)
        .map(data -> data.get(secretKeySelector.getKey()))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new IllegalStateException(
            "Key " + secretKeySelector.getKey()
            + " not found in secret " + namespace + "."
            + secretKeySelector.getName()));
  }

}

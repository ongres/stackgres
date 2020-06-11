/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

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
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackupDoneable;
import io.stackgres.common.crd.sgbackup.StackGresBackupList;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDoneable;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.ClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.crd.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
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
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.cluster.factory.Cluster;
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
import io.stackgres.operator.configuration.OperatorContext;
import io.stackgres.operator.customresource.prometheus.PrometheusConfig;
import io.stackgres.operator.customresource.prometheus.PrometheusInstallation;
import io.stackgres.operator.resource.ClusterResourceHandlerSelector;
import io.stackgres.operator.resource.ClusterSidecarFinder;
import io.stackgres.operator.sidecars.fluentbit.FluentBit;
import io.stackgres.operator.sidecars.pgexporter.PostgresExporter;
import io.stackgres.operator.sidecars.pgutils.PostgresUtil;
import io.stackgres.operator.sidecars.pooling.PgPooling;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliationCycle;
import io.stackgres.operatorframework.reconciliation.AbstractReconciliator;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterReconciliationCycle
    extends AbstractReconciliationCycle<StackGresClusterContext, StackGresCluster,
      ClusterResourceHandlerSelector> {

  private final ClusterSidecarFinder sidecarFinder;
  private final Cluster cluster;
  private final ClusterStatusManager statusManager;
  private final EventController eventController;
  private final CustomResourceScanner<PrometheusConfig> prometheusScanner;
  private final OperatorContext operatorContext;

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
      CustomResourceScanner<PrometheusConfig> prometheusScanner,
      OperatorContext operatorContext,
      LabelFactory<StackGresCluster> labelFactory) {
    super("Cluster", kubClientFactory::create, StackGresClusterContext::getCluster,
        handlerSelector, objectMapperProvider.objectMapper());
    this.sidecarFinder = sidecarFinder;
    this.cluster = cluster;
    this.statusManager = statusManager;
    this.eventController = eventController;
    this.prometheusScanner = prometheusScanner;
    this.operatorContext = operatorContext;
    this.labelFactory = labelFactory;
  }

  public ClusterReconciliationCycle() {
    super(null, null, c -> null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.sidecarFinder = null;
    this.cluster = null;
    this.statusManager = null;
    this.eventController = null;
    this.prometheusScanner = null;
    this.operatorContext = null;
    this.labelFactory = null;
  }

  @Override
  protected void onError(Exception ex) {
    eventController.sendEvent(EventReason.CLUSTER_CONFIG_ERROR,
        "StackGres Cluster reconciliation cycle failed: "
            + ex.getMessage());
  }

  @Override
  protected void onConfigError(StackGresClusterContext context, HasMetadata configResource,
                               Exception ex) {
    statusManager.sendCondition(ClusterStatusCondition.CLUSTER_CONFIG_ERROR, context);
    eventController.sendEvent(EventReason.CLUSTER_CONFIG_ERROR,
        "StackGres Cluster " + configResource.getMetadata().getNamespace() + "."
            + configResource.getMetadata().getName() + " reconciliation failed: "
            + ex.getMessage(), configResource);
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
  protected ImmutableList<StackGresClusterContext> getExistingConfigs(
      KubernetesClient client) {
    return ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME)
        .map(crd -> client
            .customResources(crd,
                StackGresCluster.class,
                StackGresClusterList.class,
                StackGresClusterDoneable.class)
            .inAnyNamespace()
            .list()
            .getItems()
            .stream()
            .map(cluster -> getClusterConfig(cluster, client))
            .collect(ImmutableList.toImmutableList()))
        .orElseThrow(() -> new IllegalStateException("StackGres is not correctly installed:"
            + " CRD " + StackGresClusterDefinition.NAME + " not found."));
  }

  private StackGresClusterContext getClusterConfig(StackGresCluster cluster,
      KubernetesClient client) {
    return ImmutableStackGresUserClusterContext.builder()
        .operatorContext(operatorContext)
        .cluster(cluster)
        .profile(getProfile(cluster, client))
        .postgresConfig(getPostgresConfig(cluster, client))
        .backupContext(getBackupContext(cluster, client))
        .sidecars(getClusterSidecars(cluster).stream()
            .map(sidecarFinder::getSidecarTransformer)
            .map(Unchecked.function(sidecar -> getSidecarEntry(cluster, client, sidecar)))
            .collect(ImmutableList.toImmutableList()))
        .backups(getBackups(cluster, client))
        .labels(labelFactory.clusterLabels(cluster))
        .clusterNamespace(labelFactory.clusterNamespace(cluster))
        .clusterName(labelFactory.clusterName(cluster))
        .clusterKey(labelFactory.getLabelMapper().clusterKey())
        .backupKey(labelFactory.getLabelMapper().backupKey())
        .ownerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(cluster)))
        .prometheus(getPrometheus(cluster, client))
        .restoreContext(getRestoreContext(cluster, client))
        .build();
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
      StackGresCluster cluster, KubernetesClient client,
      StackGresClusterSidecarResourceFactory<T> sidecar) throws Exception {
    Optional<T> sidecarConfig = sidecar.getConfig(cluster, client);
    return new SidecarEntry<T>(sidecar, sidecarConfig);
  }

  private Optional<StackGresPostgresConfig> getPostgresConfig(StackGresCluster cluster,
      KubernetesClient client) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String pgConfig = cluster.getSpec().getConfiguration().getPostgresConfig();
    if (pgConfig != null) {
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, StackGresPostgresConfigDefinition.NAME);
      if (crd.isPresent()) {
        return Optional.ofNullable(client
            .customResources(crd.get(),
                StackGresPostgresConfig.class,
                StackGresPostgresConfigList.class,
                StackGresPostgresConfigDoneable.class)
            .inNamespace(namespace)
            .withName(pgConfig)
            .get());
      }
    }
    return Optional.empty();
  }

  private Optional<StackGresBackupContext> getBackupContext(StackGresCluster cluster,
      KubernetesClient client) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String backupConfig = cluster.getSpec().getConfiguration().getBackupConfig();
    if (backupConfig != null) {
      return ResourceUtil.getCustomResource(client, StackGresBackupConfigDefinition.NAME)
          .map(crd -> client
              .customResources(crd,
                  StackGresBackupConfig.class,
                  StackGresBackupConfigList.class,
                  StackGresBackupConfigDoneable.class)
              .inNamespace(namespace)
              .withName(backupConfig)
              .get())
          .map(backupConf -> StackGresBackupContext.builder()
              .withBackupConfig(backupConf)
              .withSecrets(getBackupSecrets(client, namespace, backupConf.getSpec()))
              .build());
    }
    return Optional.empty();
  }

  private Optional<StackGresProfile> getProfile(StackGresCluster cluster,
      KubernetesClient client) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String profileName = cluster.getSpec().getResourceProfile();
    if (profileName != null) {
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME);
      if (crd.isPresent()) {
        return Optional.ofNullable(client
            .customResources(crd.get(),
                StackGresProfile.class,
                StackGresProfileList.class,
                StackGresProfileDoneable.class)
            .inNamespace(namespace)
            .withName(profileName)
            .get());
      }
    }
    return Optional.empty();
  }

  private ImmutableList<StackGresBackup> getBackups(StackGresCluster cluster,
      KubernetesClient client) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    return ResourceUtil.getCustomResource(client, StackGresBackupDefinition.NAME)
        .map(crd -> client
            .customResources(crd,
                StackGresBackup.class,
                StackGresBackupList.class,
                StackGresBackupDoneable.class)
            .inNamespace(namespace)
            .list()
            .getItems()
            .stream()
            .filter(backup -> backup.getSpec().getSgCluster().equals(name))
            .collect(ImmutableList.toImmutableList()))
        .orElse(ImmutableList.of());
  }

  public Optional<Prometheus> getPrometheus(StackGresCluster cluster,
      KubernetesClient client) {
    boolean isAutobindAllowed = Boolean
        .parseBoolean(operatorContext.getProperty(OperatorProperty.PROMETHEUS_AUTOBIND)
            .orElse("false"));

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

  private Optional<StackGresRestoreContext> getRestoreContext(StackGresCluster cluster,
      KubernetesClient client) {
    Optional<ClusterRestore> restoreOpt = Optional
        .ofNullable(cluster.getSpec().getInitData())
        .map(StackGresClusterInitData::getRestore);

    if (restoreOpt.isPresent()) {
      ClusterRestore restore = restoreOpt.get();
      return ResourceUtil.getCustomResource(client, StackGresBackupDefinition.NAME)
          .flatMap(crd -> client
              .customResources(crd,
                  StackGresBackup.class,
                  StackGresBackupList.class,
                  StackGresBackupDoneable.class)
              .inAnyNamespace()
              .list()
              .getItems()
              .stream()
              .filter(backup -> backup.getMetadata().getUid().equals(restore.getBackupUid()))
              .findAny())
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
              .withSecrets(getBackupSecrets(client, backup.getMetadata().getNamespace(),
                  backup.getStatus().getBackupConfig()))
              .build());
    }
    return Optional.empty();
  }

  private ImmutableMap<String, Map<String, String>> getBackupSecrets(
      KubernetesClient client,
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
            Optional.of(Optional.ofNullable(client.secrets()
                .inNamespace(namespace)
                .withName(secretKeySelector.getName())
                .get())
                .orElseThrow(() -> new IllegalStateException(
                    "Secret " + namespace + "." + secretKeySelector.getName()
                        + " not found")))
                .map(secret -> secret
                    .getData()
                    .get(secretKeySelector.getKey()))
                .map(ResourceUtil::dencodeSecret)
                .orElseThrow(() -> new IllegalStateException(
                    "Key " + secretKeySelector.getKey()
                        + " not found in secret " + namespace + "."
                        + secretKeySelector.getName()))))
        .grouped(t -> t.v1.getName())
        .collect(ImmutableMap.toImmutableMap(
            t -> t.v1, t -> t.v2
                .collect(ImmutableMap.toImmutableMap(
                    tt -> tt.v1.getKey(), tt -> tt.v2))));
  }

}

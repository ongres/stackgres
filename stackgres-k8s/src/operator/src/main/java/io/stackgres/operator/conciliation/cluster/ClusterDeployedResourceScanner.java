/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.external.autoscaling.VerticalPodAutoscaler;
import io.stackgres.common.crd.external.autoscaling.VerticalPodAutoscalerList;
import io.stackgres.common.crd.external.keda.ScaledObject;
import io.stackgres.common.crd.external.keda.ScaledObjectList;
import io.stackgres.common.crd.external.keda.TriggerAuthentication;
import io.stackgres.common.crd.external.keda.TriggerAuthenticationList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterAutoscaling;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptList;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterDeployedResourceScanner
    extends AbstractDeployedResourcesScanner<StackGresCluster>
    implements ReconciliationOperations {

  private final KubernetesClient client;
  private final LabelFactoryForCluster labelFactory;

  @Inject
  public ClusterDeployedResourceScanner(
      DeployedResourcesCache deployedResourcesCache,
      KubernetesClient client,
      LabelFactoryForCluster labelFactory) {
    super(deployedResourcesCache);
    this.client = client;
    this.labelFactory = labelFactory;
  }

  public ClusterDeployedResourceScanner() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.labelFactory = null;
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresCluster config) {
    return labelFactory.genericLabels(config);
  }

  @Override
  protected Map<String, String> getCrossNamespaceLabels(StackGresCluster config) {
    return labelFactory.clusterCrossNamespaceLabels(config);
  }

  @Override
  protected KubernetesClient getClient() {
    return client;
  }

  @Override
  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInNamepspaceResourceOperations(
                  StackGresCluster cluster) {
    return Seq.seq(IN_NAMESPACE_RESOURCE_OPERATIONS.entrySet())
        .append(Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getAutoscaling)
            .map(StackGresClusterAutoscaling::isHorizontalPodAutoscalingEnabled)
            .stream()
            .flatMap(ignored -> KEDA_RESOURCE_OPERATIONS.entrySet().stream()))
        .append(Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getAutoscaling)
            .map(StackGresClusterAutoscaling::isVerticalPodAutoscalingEnabled)
            .stream()
            .flatMap(ignored -> VERTICAL_POD_AUTOSCALER_RESOURCE_OPERATIONS.entrySet().stream()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInAnyNamespaceResourceOperations(
                  StackGresCluster cluster) {
    return super.getInAnyNamespaceResourceOperations(cluster);
  }

  static final Map<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      IN_NAMESPACE_RESOURCE_OPERATIONS =
      Map.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>ofEntries(
          Map.entry(Secret.class, KubernetesClient::secrets),
          Map.entry(ConfigMap.class, KubernetesClient::configMaps),
          Map.entry(ServiceAccount.class, KubernetesClient::serviceAccounts),
          Map.entry(Role.class, client -> client.rbac().roles()),
          Map.entry(RoleBinding.class, client -> client.rbac().roleBindings()),
          Map.entry(Endpoints.class, KubernetesClient::endpoints),
          Map.entry(Service.class, KubernetesClient::services),
          Map.entry(Pod.class, client -> client.pods()),
          Map.entry(Job.class, client -> client.batch().v1().jobs()),
          Map.entry(CronJob.class, client -> client.batch().v1().cronjobs()),
          Map.entry(StatefulSet.class, client -> client.apps().statefulSets()),
          Map.entry(StackGresScript.class, client -> client
              .resources(StackGresScript.class, StackGresScriptList.class)),
          Map.entry(StackGresProfile.class, client -> client
              .resources(StackGresProfile.class, StackGresProfileList.class)),
          Map.entry(StackGresPostgresConfig.class, client -> client
              .resources(StackGresPostgresConfig.class, StackGresPostgresConfigList.class)),
          Map.entry(StackGresPoolingConfig.class, client -> client
              .resources(StackGresPoolingConfig.class, StackGresPoolingConfigList.class))
          );

  static final Map<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      VERTICAL_POD_AUTOSCALER_RESOURCE_OPERATIONS =
      Map.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>ofEntries(
          Map.entry(VerticalPodAutoscaler.class, client -> client
              .resources(VerticalPodAutoscaler.class, VerticalPodAutoscalerList.class))
          );

  static final Map<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      KEDA_RESOURCE_OPERATIONS =
      Map.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>ofEntries(
          Map.entry(ScaledObject.class, client -> client
              .resources(ScaledObject.class, ScaledObjectList.class)),
          Map.entry(TriggerAuthentication.class, client -> client
              .resources(TriggerAuthentication.class, TriggerAuthenticationList.class))
          );

}

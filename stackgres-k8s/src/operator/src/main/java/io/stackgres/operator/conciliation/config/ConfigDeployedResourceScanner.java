/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleList;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.external.prometheus.PodMonitor;
import io.stackgres.common.crd.external.prometheus.PodMonitorList;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorPrometheusOperator;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.ReconciliationOperations;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigDeployedResourceScanner
    extends AbstractDeployedResourcesScanner<StackGresConfig>
    implements ReconciliationOperations {

  private final Optional<String> operatorNamespace = OperatorProperty.OPERATOR_NAMESPACE.get();

  private final KubernetesClient client;
  private final LabelFactoryForConfig labelFactory;
  private final boolean prometheusAutobind;

  @Inject
  public ConfigDeployedResourceScanner(
      DeployedResourcesCache deployedResourcesCache,
      KubernetesClient client,
      LabelFactoryForConfig labelFactory,
      OperatorPropertyContext operatorContext) {
    super(deployedResourcesCache);
    this.client = client;
    this.labelFactory = labelFactory;
    this.prometheusAutobind = operatorContext.getBoolean(OperatorProperty.PROMETHEUS_AUTOBIND);
  }

  public ConfigDeployedResourceScanner() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.client = null;
    this.labelFactory = null;
    this.prometheusAutobind = false;
  }

  @Override
  protected Map<String, String> getGenericLabels(StackGresConfig config) {
    return labelFactory.genericLabels(config);
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
                  StackGresConfig config) {
    var inNamespaceResourceOperations = new HashMap<>(IN_NAMESPACE_RESOURCE_OPERATIONS);
    if (prometheusAutobind && Optional.of(config)
        .map(StackGresConfig::getSpec)
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getPrometheusOperator)
        .map(StackGresConfigCollectorPrometheusOperator::getMonitors)
        .filter(monitors -> monitors.size() > 0)
        .map(ignored -> true)
        .or(() -> Optional.of(config)
            .map(StackGresConfig::getSpec)
            .map(StackGresConfigSpec::getCollector)
            .map(StackGresConfigCollector::getPrometheusOperator)
            .map(StackGresConfigCollectorPrometheusOperator::getAllowDiscovery))
        .orElse(false)) {
      inNamespaceResourceOperations.putAll(PROMETHEUS_RESOURCE_OPERATIONS);
    }
    return inNamespaceResourceOperations;
  }

  @Override
  protected Map<Class<? extends HasMetadata>,
      Function<KubernetesClient, MixedOperation<? extends HasMetadata,
          ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>> getInAnyNamespaceResourceOperations(
                  StackGresConfig config) {
    var inAnyNamespaceResourceOperations = new HashMap<>(
        super.getInAnyNamespaceResourceOperations(config));
    if (!Optional.of(config)
        .map(StackGresConfig::getSpec)
        .map(StackGresConfigSpec::getDisableClusterRole).orElse(false)
        && !Optional.of(config)
        .map(StackGresConfig::getSpec)
        .map(StackGresConfigSpec::getSgConfigNamespace)
        .equals(operatorNamespace)) {
      inAnyNamespaceResourceOperations.putAll(CLUSTER_ROLE_RESOURCE_OPERATIONS);
    }
    if (prometheusAutobind && Optional.of(config)
        .map(StackGresConfig::getSpec)
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getPrometheusOperator)
        .map(StackGresConfigCollectorPrometheusOperator::getMonitors)
        .filter(monitors -> monitors.size() > 0)
        .map(ignored -> true)
        .or(() -> Optional.of(config)
            .map(StackGresConfig::getSpec)
            .map(StackGresConfigSpec::getCollector)
            .map(StackGresConfigCollector::getPrometheusOperator)
            .map(StackGresConfigCollectorPrometheusOperator::getAllowDiscovery))
        .orElse(false)) {
      inAnyNamespaceResourceOperations.putAll(PROMETHEUS_RESOURCE_OPERATIONS);
    }
    return inAnyNamespaceResourceOperations;
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
          Map.entry(Service.class, KubernetesClient::services),
          Map.entry(Role.class, client -> client.rbac().roles()),
          Map.entry(RoleBinding.class, client -> client.rbac().roleBindings()),
          Map.entry(Job.class, client -> client.batch().v1().jobs()),
          Map.entry(Deployment.class, client -> client.apps().deployments()));

  static final Map<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      CLUSTER_ROLE_RESOURCE_OPERATIONS =
      Map.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>ofEntries(
          Map.entry(ClusterRole.class, client -> client
              .resources(ClusterRole.class, ClusterRoleList.class))
          );

  static final Map<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      PROMETHEUS_RESOURCE_OPERATIONS =
      Map.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>ofEntries(
          Map.entry(PodMonitor.class, client -> client
              .resources(PodMonitor.class, PodMonitorList.class))
          );

}

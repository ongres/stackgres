/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigGrafana;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.config.OperatorSecret;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleAdminSecret;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleDeployment;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleGrafanaIntegrationJob;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleSecret;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresConfig> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ConfigRequiredResourcesGenerator.class);

  private final Optional<String> sgConfigNamespace = OperatorProperty.SGCONFIG_NAMESPACE.get();
  private final Optional<String> operatorNamespace = OperatorProperty.OPERATOR_NAMESPACE.get();

  private final Supplier<VersionInfo> kubernetesVersionSupplier;

  private final ResourceGenerationDiscoverer<StackGresConfigContext> discoverer;

  private final ResourceFinder<ServiceAccount> serviceAccountFinder;

  private final ResourceFinder<Secret> secretFinder;

  private final ResourceFinder<Job> jobFinder;

  private final ResourceScanner<ClusterRoleBinding> clusterRoleBindingScanner;

  private final ConfigGrafanaIntegrationChecker grafanaIntegrationChecker;

  @Inject
  public ConfigRequiredResourcesGenerator(
      Supplier<VersionInfo> kubernetesVersionSupplier,
      ResourceGenerationDiscoverer<StackGresConfigContext> discoverer,
      ResourceFinder<ServiceAccount> serviceAccountFinder,
      ResourceFinder<Secret> secretFinder,
      ResourceFinder<Job> jobFinder,
      ResourceScanner<ClusterRoleBinding> clusterRoleBindingScanner,
      ConfigGrafanaIntegrationChecker grafanaIntegrationChecker) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
    this.discoverer = discoverer;
    this.serviceAccountFinder = serviceAccountFinder;
    this.secretFinder = secretFinder;
    this.jobFinder = jobFinder;
    this.clusterRoleBindingScanner = clusterRoleBindingScanner;
    this.grafanaIntegrationChecker = grafanaIntegrationChecker;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresConfig config) {
    VersionInfo kubernetesVersion = kubernetesVersionSupplier.get();

    String namespace = config.getMetadata().getNamespace();
    Optional<Secret> operatorSecret = secretFinder.findByNameAndNamespace(
        OperatorSecret.name(config), namespace);
    Optional<Secret> webConsoleSecret = secretFinder.findByNameAndNamespace(
        WebConsoleSecret.name(config), namespace);
    Optional<Secret> webConsoleAdminSecret = secretFinder.findByNameAndNamespace(
        WebConsoleAdminSecret.sourceName(config), namespace);
    Optional<ServiceAccount> webConsoleServiceAccount = serviceAccountFinder.findByNameAndNamespace(
        WebConsoleDeployment.name(config), namespace);
    boolean isGrafanaEmbedded = grafanaIntegrationChecker.isGrafanaEmbedded(config);
    boolean isGrafanaIntegrated = grafanaIntegrationChecker.isGrafanaIntegrated(config);
    boolean isGrafanaIntegrationJobFailed =
        jobFinder.findByNameAndNamespace(WebConsoleGrafanaIntegrationJob.name(config), namespace)
        .map(Job::getStatus)
        .map(JobStatus::getFailed)
        .map(failed -> failed > 0)
        .orElse(false);
    Optional<Map<String, String>> grafanaCredentials = Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getGrafana)
        .filter(grafana -> grafana.getSecretNamespace() != null
            && grafana.getSecretName() != null
            && grafana.getSecretUserKey() != null
            && grafana.getSecretPasswordKey() != null)
        .map(grafana -> secretFinder.findByNameAndNamespace(
            grafana.getSecretName(), grafana.getSecretNamespace())
            .orElseThrow(() -> new IllegalArgumentException(
                "Can not find secret "
                    + grafana.getSecretNamespace() + "." + grafana.getSecretName()
                    + " for grafana credentials")))
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret);
    Optional<String> grafanaUser = grafanaCredentials
        .map(credentials -> credentials.get(
            config.getSpec().getGrafana().getSecretUserKey()))
        .or(() -> Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getGrafana)
            .map(StackGresConfigGrafana::getUser));
    Optional<String> grafanaPassword = grafanaCredentials
        .map(credentials -> credentials.get(
            config.getSpec().getGrafana().getSecretPasswordKey()))
        .or(() -> Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getGrafana)
            .map(StackGresConfigGrafana::getPassword));
    final Optional<String> webConsoleClusterRoleName;
    if (!Optional.ofNullable(config.getSpec().getDisableClusterRole()).orElse(false)
        && !sgConfigNamespace.or(() -> operatorNamespace).equals(operatorNamespace)
        && !Optional.ofNullable(config.getSpec())
            .map(StackGresConfigSpec::getDeploy)
            .map(StackGresConfigDeploy::getRestapi)
            .orElse(true)) {
      var webConsoleClusterRoleBindings = clusterRoleBindingScanner.getResources()
          .stream()
          .filter(clusterRoleBinding -> Objects
              .equals(
                  clusterRoleBinding.getRoleRef().getApiGroup(),
                  HasMetadata.getGroup(ClusterRole.class))
              && Objects.equals(
                  clusterRoleBinding.getRoleRef().getKind(),
                  HasMetadata.getKind(ClusterRole.class))
              && clusterRoleBinding.getSubjects().stream()
              .anyMatch(subject -> Objects.equals(subject.getKind(), HasMetadata.getKind(ServiceAccount.class))
                  && Objects.equals(subject.getNamespace(), operatorNamespace.get())
                  && Objects.equals(subject.getName(), WebConsoleDeployment.name(config))))
          .toList();
      if (webConsoleClusterRoleBindings.size() > 1) {
        throw new IllegalArgumentException("Found more than 1 cluster role binding for Web Console service account: "
            + webConsoleClusterRoleBindings.stream()
            .map(HasMetadata::getMetadata)
            .map(ObjectMeta::getName)
            .collect(Collectors.joining(" ")));
      }
      if (webConsoleClusterRoleBindings.size() < 1) {
        throw new IllegalArgumentException("No cluster role binding for Web Console was found.");
      }
      webConsoleClusterRoleName = webConsoleClusterRoleBindings.stream()
          .map(ClusterRoleBinding::getRoleRef)
          .map(RoleRef::getName)
          .findFirst();
    } else {
      webConsoleClusterRoleName = Optional.empty();
    }

    StackGresConfigContext context = ImmutableStackGresConfigContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(config)
        .operatorSecret(operatorSecret)
        .webConsoleSecret(webConsoleSecret)
        .webConsoleAdminSecret(webConsoleAdminSecret)
        .webConsoleServiceAccount(webConsoleServiceAccount)
        .webConsoleClusterRoleName(webConsoleClusterRoleName)
        .isGrafanaEmbedded(isGrafanaEmbedded)
        .isGrafanaIntegrated(isGrafanaIntegrated)
        .isGrafanaIntegrationJobFailed(isGrafanaIntegrationJobFailed)
        .grafanaUser(grafanaUser)
        .grafanaPassword(grafanaPassword)
        .build();

    return discoverer.generateResources(context);
  }

}

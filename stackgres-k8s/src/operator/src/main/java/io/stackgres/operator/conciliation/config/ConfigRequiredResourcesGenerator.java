/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigGrafana;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.config.OperatorSecret;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleAdminSecret;
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

  private final Supplier<VersionInfo> kubernetesVersionSupplier;

  private final ResourceGenerationDiscoverer<StackGresConfigContext> discoverer;

  private final ResourceFinder<Secret> secretFinder;

  private final ResourceFinder<Job> jobFinder;

  private final ConfigGrafanaIntegrationChecker grafanaIntegrationChecker;

  @Inject
  public ConfigRequiredResourcesGenerator(
      Supplier<VersionInfo> kubernetesVersionSupplier,
      ResourceGenerationDiscoverer<StackGresConfigContext> discoverer,
      ResourceFinder<Secret> secretFinder,
      ResourceFinder<Job> jobFinder,
      ConfigGrafanaIntegrationChecker grafanaIntegrationChecker) {
    this.kubernetesVersionSupplier = kubernetesVersionSupplier;
    this.discoverer = discoverer;
    this.secretFinder = secretFinder;
    this.jobFinder = jobFinder;
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
        WebConsoleAdminSecret.name(config), namespace);
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

    StackGresConfigContext context = ImmutableStackGresConfigContext.builder()
        .kubernetesVersion(kubernetesVersion)
        .source(config)
        .operatorSecret(operatorSecret)
        .webConsoleSecret(webConsoleSecret)
        .webConsoleAdminSecret(webConsoleAdminSecret)
        .isGrafanaIntegrated(isGrafanaIntegrated)
        .isGrafanaIntegrationJobFailed(isGrafanaIntegrationJobFailed)
        .grafanaUser(grafanaUser)
        .grafanaPassword(grafanaPassword)
        .build();

    return discoverer.generateResources(context);
  }

}

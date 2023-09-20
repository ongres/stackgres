/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigInstaller {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigInstaller.class);

  String operatorName = OperatorProperty.OPERATOR_NAME.getString();

  String operatorNamespace = OperatorProperty.OPERATOR_NAMESPACE.getString();

  Optional<String> sgconfig = OperatorProperty.SGCONFIG.get();

  boolean removeOldOperatorBundleResources =
      OperatorProperty.REMOVE_OLD_OPERATOR_BUNDLE_RESOURCES.getBoolean();

  private final YAMLMapper yamlMapper;
  private final CrdLoader crdLoader;
  private final ResourceFinder<CustomResourceDefinition> crdResourceFinder;
  private final ResourceWriter<CustomResourceDefinition> crdResourceWriter;
  private final CustomResourceFinder<StackGresConfig> configFinder;
  private final CustomResourceScheduler<StackGresConfig> configWriter;
  private final KubernetesClient client;

  @Inject
  public ConfigInstaller(
      YamlMapperProvider yamlMapperProvider,
      ResourceFinder<CustomResourceDefinition> crdResourceFinder,
      ResourceWriter<CustomResourceDefinition> crdResourceWriter,
      CustomResourceFinder<StackGresConfig> configFinder,
      CustomResourceScheduler<StackGresConfig> configWriter,
      KubernetesClient client) {
    this.yamlMapper = yamlMapperProvider.get();
    this.crdLoader = new CrdLoader(yamlMapper);
    this.crdResourceFinder = crdResourceFinder;
    this.crdResourceWriter = crdResourceWriter;
    this.configFinder = configFinder;
    this.configWriter = configWriter;
    this.client = client;
  }

  public void installOrUpdateConfig() {
    LOGGER.info("Installing SGConfig");
    var configCrd = crdLoader.getCrd(HasMetadata.getKind(StackGresConfig.class));
    String configCrdName = configCrd.getMetadata().getName();
    var configCrdFound = crdResourceFinder.findByName(configCrdName);
    if (configCrdFound.isEmpty()) {
      if (OperatorProperty.INSTALL_CRDS.getBoolean()) {
        LOGGER.info("CRD {} is not present, installing it", configCrdName);
        crdResourceWriter.create(configCrd);
      } else {
        throw new RuntimeException("CRD " + configCrdName + " is not present, aborting!");
      }
    }
    var configFound = configFinder.findByNameAndNamespace(operatorName, operatorNamespace);
    final StackGresConfig config = configFound
        .map(Unchecked.function(configValue -> yamlMapper.treeToValue(
            yamlMapper.valueToTree(configValue),
            StackGresConfig.class)))
        .orElseGet(() -> new StackGresConfigBuilder()
            .withNewMetadata()
            .withNamespace(operatorNamespace)
            .withName(operatorName)
            .endMetadata()
            .withNewSpec()
            .endSpec()
            .build());
    if (sgconfig.isPresent()) {
      try {
        JsonNode configTree = yamlMapper.valueToTree(config);
        JsonNode updatedConfigTree = yamlMapper.readerForUpdating(configTree)
            .readTree(sgconfig.get());
        StackGresConfig updatedConfig = yamlMapper
            .treeToValue(updatedConfigTree, StackGresConfig.class);
        config.setSpec(updatedConfig.getSpec());
      } catch (JsonProcessingException ex) {
        throw new RuntimeException(ex);
      }
    }
    if (config.getStatus() == null) {
      config.setStatus(new StackGresConfigStatus());
    }
    config.getStatus().setVersion(StackGresProperty.OPERATOR_VERSION.getString());

    //TODO: Remove this as soon as version 1.5 get out of support!
    if (removeOldOperatorBundleResources
        && !Optional.of(config.getStatus())
        .map(StackGresConfigStatus::getOldOperatorBundleResourcesRemoved)
        .orElse(false)) {
      LOGGER.info("Cleanup old operator bundle resources");

      //TODO: Remove this as soon as version 1.4 get out of support!
      removeOldOperatorBundleResourcesForv1_4_3();

      removeOldOperatorBundleResourcesForv1_5_0();

      config.getStatus().setOldOperatorBundleResourcesRemoved(true);
    }

    if (configFound.isEmpty()
        || !Objects.equals(configFound.get(), config)) {
      if (configFound.isEmpty()) {
        LOGGER.info("Creating SGConfig");
        configWriter.create(config);
      } else {
        LOGGER.info("Updating SGConfig");
        configWriter.update(config, foundConfig -> foundConfig.setSpec(config.getSpec()));
      }
      LOGGER.info("Updating SGConfig status");
      configWriter.updateStatus(config, foundConfig -> foundConfig.setStatus(config.getStatus()));
    } else {
      LOGGER.info("SGConfig already installed");
    }
  }

  private void removeOldOperatorBundleResourcesForv1_4_3() {
    performIgnoringNotFound(() -> configWriter.delete(new StackGresConfigBuilder()
        .withNewMetadata()
        .withName("stackgres")
        .withNamespace(operatorNamespace)
        .endMetadata()
        .withNewSpec()
        .endSpec()
        .build()));
    performIgnoringNotFound(() -> client.genericKubernetesResources("stackgres.io/v1", "SGConfig")
        .inNamespace(operatorNamespace)
        .withName("stackgres")
        .patch(
            PatchContext.of(PatchType.JSON),
            "[{\"op\":\"replace\",\"path\":\"/metadata/finalizers\",\"value\":null}]"));
    performIgnoringNotFound(() -> client.rbac().clusterRoles()
        .withName("stackgres").delete());
    performIgnoringNotFound(() -> client.rbac().clusterRoleBindings()
        .withName("stackgres").delete());
    performIgnoringNotFound(() -> client.admissionRegistration().v1()
        .validatingWebhookConfigurations()
        .withName("stackgres").delete());
    performIgnoringNotFound(() -> client.admissionRegistration().v1()
        .mutatingWebhookConfigurations()
        .withName("stackgres").delete());
    performIgnoringNotFound(() -> client.configMaps()
        .withName("stackgres").delete());
    performIgnoringNotFound(() -> client.configMaps()
        .withName("stackgres-grafana-dashboard").delete());
    performIgnoringNotFound(() -> client.secrets()
        .withName("stackgres-certs").delete());
    performIgnoringNotFound(() -> client.secrets()
        .withName("stackgres-service-certs").delete());
    performIgnoringNotFound(() -> client.secrets()
        .withName("stackgres-web-certs").delete());
    performIgnoringNotFound(() -> client.certificates().v1().certificateSigningRequests()
        .withName("stackgres").delete());
    client.apps().deployments()
        .withLabels(Map.of("olm.owner", "stackgres.v1.4.3")).list()
        .getItems()
        .stream()
        .forEach(resource -> client.apps().deployments()
            .resource(resource).delete());
  }

  private void removeOldOperatorBundleResourcesForv1_5_0() {
    performIgnoringNotFound(() -> client.serviceAccounts()
        .withName("stackgres-restapi").delete());
    performIgnoringNotFound(() -> client.rbac().clusterRoles()
        .withName("stackgres-restapi").delete());
    performIgnoringNotFound(() -> client.rbac().clusterRoleBindings()
        .withName("stackgres-restapi").delete());
    performIgnoringNotFound(() -> client.rbac().clusterRoleBindings()
        .withName("stackgres-restapi-admin").delete());
    performIgnoringNotFound(() -> client.admissionRegistration().v1()
        .validatingWebhookConfigurations()
        .withName("stackgres-operator").delete());
    performIgnoringNotFound(() -> client.admissionRegistration().v1()
        .mutatingWebhookConfigurations()
        .withName("stackgres-operator").delete());
    performIgnoringNotFound(() -> client.configMaps()
        .withName("stackgres-operator").delete());
    performIgnoringNotFound(() -> client.configMaps()
        .withName("stackgres-operator-grafana-dashboard").delete());
    performIgnoringNotFound(() -> client.configMaps()
        .withName("stackgres-restapi-nginx").delete());
    performIgnoringNotFound(() -> client.secrets()
        .withName("stackgres-operator-certs").delete());
    performIgnoringNotFound(() -> client.secrets()
        .withName("stackgres-operator-service-certs").delete());
    performIgnoringNotFound(() -> client.secrets()
        .withName("stackgres-operator-web-certs").delete());
    performIgnoringNotFound(() -> client.secrets()
        .withName("stackgres-restapi").delete());
    performIgnoringNotFound(() -> client.certificates().v1().certificateSigningRequests()
        .withName("stackgres-operator").delete());
    client.apps().deployments()
        .withLabels(Map.of("olm.owner", "stackgres.v1.5.0")).list()
        .getItems()
        .stream()
        .forEach(resource -> client.apps().deployments()
            .resource(resource).delete());
    client.apps().deployments()
        .withLabels(Map.of("app", "stackgres-restapi", "group", "stackgres.io")).list()
        .getItems()
        .stream()
        .forEach(resource -> client.apps().deployments()
            .resource(resource).delete());
    client.batch().v1().jobs()
        .withLabels(Map.of("app", "stackgres-operator-init")).list()
        .getItems()
        .stream()
        .forEach(resource -> client.batch().v1().jobs()
            .resource(resource).delete());
    performIgnoringNotFound(() -> client.genericKubernetesResources("stackgres.io/v1", "SGConfig")
        .inNamespace(operatorNamespace)
        .withName(operatorName)
        .patch(
            PatchContext.of(PatchType.JSON),
            "[{\"op\":\"replace\",\"path\":\"/metadata/finalizers\",\"value\":null}]"));
  }

  private void performIgnoringNotFound(Runnable runnable) {
    try {
      runnable.run();
    } catch (KubernetesClientException ex) {
      if (ex.getCode() == 404) {
        return;
      }
      throw ex;
    }
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatus;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleDeployment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigInstaller {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigInstaller.class);

  final String operatorName = OperatorProperty.OPERATOR_NAME.getString();
  final String operatorNamespace = OperatorProperty.OPERATOR_NAMESPACE.getString();
  final String sgConfigNamespace = OperatorProperty.SGCONFIG_NAMESPACE.get()
      .orElseGet(OperatorProperty.OPERATOR_NAMESPACE::getString);
  final boolean clusterRoleDisabled = OperatorProperty.CLUSTER_ROLE_DISABLED.getBoolean();
  final Optional<String> sgconfig = OperatorProperty.SGCONFIG.get();

  private final YAMLMapper yamlMapper;
  private final CrdLoader crdLoader;
  private final ResourceFinder<CustomResourceDefinition> crdResourceFinder;
  private final ResourceWriter<CustomResourceDefinition> crdResourceWriter;
  private final CustomResourceFinder<StackGresConfig> configFinder;
  private final CustomResourceScheduler<StackGresConfig> configWriter;
  private final ResourceScanner<ClusterRoleBinding> clusterRoleBindingScanner;
  private final ResourceFinder<ClusterRoleBinding> clusterRoleBindingFinder;
  private final ResourceWriter<ClusterRoleBinding> clusterRoleBindingWriter;
  private final LabelFactoryForConfig labelFactory;

  @Inject
  public ConfigInstaller(
      YamlMapperProvider yamlMapperProvider,
      ResourceFinder<CustomResourceDefinition> crdResourceFinder,
      ResourceWriter<CustomResourceDefinition> crdResourceWriter,
      CustomResourceFinder<StackGresConfig> configFinder,
      CustomResourceScheduler<StackGresConfig> configWriter,
      ResourceScanner<ClusterRoleBinding> clusterRoleBindingScanner,
      ResourceFinder<ClusterRoleBinding> clusterRoleBindingFinder,
      ResourceWriter<ClusterRoleBinding> clusterRoleBindingWriter,
      LabelFactoryForConfig labelFactory) {
    this.yamlMapper = yamlMapperProvider.get();
    this.crdLoader = new CrdLoader(yamlMapper);
    this.crdResourceFinder = crdResourceFinder;
    this.crdResourceWriter = crdResourceWriter;
    this.configFinder = configFinder;
    this.configWriter = configWriter;
    this.clusterRoleBindingScanner = clusterRoleBindingScanner;
    this.clusterRoleBindingFinder = clusterRoleBindingFinder;
    this.clusterRoleBindingWriter = clusterRoleBindingWriter;
    this.labelFactory = labelFactory;
  }

  public void installOrUpdateConfig() {
    LOGGER.info("Installing SGConfig");
    var configCrd = crdLoader.getCrd(HasMetadata.getKind(StackGresConfig.class));
    String configCrdName = configCrd.getMetadata().getName();
    var configCrdFound = clusterRoleDisabled || crdResourceFinder.findByName(configCrdName).isPresent();
    if (!configCrdFound) {
      if (OperatorProperty.INSTALL_CRDS.getBoolean()) {
        LOGGER.info("CRD {} is not present, installing it", configCrdName);
        crdResourceWriter.create(configCrd);
      } else {
        throw new RuntimeException("CRD " + configCrdName + " is not present, aborting!");
      }
    }
    var configFound = configFinder.findByNameAndNamespace(operatorName, sgConfigNamespace);
    final StackGresConfig config = configFound
        .map(Unchecked.function(configValue -> yamlMapper.treeToValue(
            yamlMapper.valueToTree(configValue),
            StackGresConfig.class)))
        .orElseGet(() -> new StackGresConfigBuilder()
            .withNewMetadata()
            .withNamespace(sgConfigNamespace)
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

    if (!Optional.ofNullable(config.getSpec().getDisableClusterRole()).orElse(false)
        && !sgConfigNamespace.equals(operatorNamespace)
        && Optional.ofNullable(config.getSpec())
            .map(StackGresConfigSpec::getDeploy)
            .map(StackGresConfigDeploy::getRestapi)
            .orElse(true)) {
      LOGGER.info("Creating ClusterRoleBinding for REST API since SGConfig in different namespace than the operator");
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
                  && Objects.equals(subject.getNamespace(), operatorNamespace)
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
      String webConsoleClusterRoleName = webConsoleClusterRoleBindings.stream()
          .map(ClusterRoleBinding::getRoleRef)
          .map(RoleRef::getName)
          .findFirst()
          .get();
      ClusterRoleBinding webConsoleClusterRole = new ClusterRoleBindingBuilder()
          .withNewMetadata()
          .withName(WebConsoleDeployment.namespacedClusterRoleBindingName(config))
          .withLabels(labelFactory.genericLabels(config))
          .endMetadata()
          .withNewRoleRef()
          .withApiGroup(HasMetadata.getGroup(ClusterRole.class))
          .withKind(HasMetadata.getKind(ClusterRole.class))
          .withName(webConsoleClusterRoleName)
          .endRoleRef()
          .addNewSubject()
          .withKind(HasMetadata.getKind(ServiceAccount.class))
          .withNamespace(sgConfigNamespace)
          .withName(WebConsoleDeployment.name(config))
          .endSubject()
          .build();
      if (clusterRoleBindingFinder.findByName(webConsoleClusterRole.getMetadata().getName()).isEmpty()) {
        clusterRoleBindingWriter.create(webConsoleClusterRole);
        LOGGER.info("ClusterRoleBinding {} targeting ClusterRole {} created",
            webConsoleClusterRole.getMetadata().getName(),
            webConsoleClusterRoleName);
      } else {
        clusterRoleBindingWriter.update(webConsoleClusterRole);
        LOGGER.info("ClusterRoleBinding {} targeting ClusterRole {} updated",
            webConsoleClusterRole.getMetadata().getName(),
            webConsoleClusterRoleName);
      }
    }
  }

}

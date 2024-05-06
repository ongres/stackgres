/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
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
  final boolean clusterRoleDisabled = OperatorProperty.CLUSTER_ROLE_DISABLED.getBoolean();
  final Optional<String> sgconfig = OperatorProperty.SGCONFIG.get();

  private final YAMLMapper yamlMapper;
  private final CrdLoader crdLoader;
  private final ResourceFinder<CustomResourceDefinition> crdResourceFinder;
  private final ResourceWriter<CustomResourceDefinition> crdResourceWriter;
  private final CustomResourceFinder<StackGresConfig> configFinder;
  private final CustomResourceScheduler<StackGresConfig> configWriter;

  @Inject
  public ConfigInstaller(
      YamlMapperProvider yamlMapperProvider,
      ResourceFinder<CustomResourceDefinition> crdResourceFinder,
      ResourceWriter<CustomResourceDefinition> crdResourceWriter,
      CustomResourceFinder<StackGresConfig> configFinder,
      CustomResourceScheduler<StackGresConfig> configWriter) {
    this.yamlMapper = yamlMapperProvider.get();
    this.crdLoader = new CrdLoader(yamlMapper);
    this.crdResourceFinder = crdResourceFinder;
    this.crdResourceWriter = crdResourceWriter;
    this.configFinder = configFinder;
    this.configWriter = configWriter;
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

}

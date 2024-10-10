/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.docir.DocirConfigUtil;
import io.stackgres.common.docir.DocirMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.app.OperatorInstallationInfoHolder;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class OperatorDocirMetadataManager extends DocirMetadataManager {

  @Inject
  public OperatorDocirMetadataManager(
      ObjectMapper objectMapper,
      WebClientFactory webClientFactory,
      CustomResourceFinder<StackGresConfig> configFinder,
      Instance<OperatorInstallationInfoHolder> installationInfoHolder) {
    super(
        objectMapper,
        webClientFactory,
        () -> getRepositoryUri(configFinder),
        () -> Map.ofEntries(installationInfoHolder.get().getUserAgentHeaderEntry()),
        false);
  }

  private static URI getRepositoryUri(
      CustomResourceFinder<StackGresConfig> configFinder) {
    return DocirConfigUtil.getRepositoryUri(
        configFinder.findByNameAndNamespace(
            OperatorProperty.OPERATOR_NAME.getString(),
            OperatorProperty.OPERATOR_NAMESPACE.getString())
            .map(StackGresConfig::getSpec)
            .map(StackGresConfigSpec::getRepository)
            .orElse(null));
  }

}

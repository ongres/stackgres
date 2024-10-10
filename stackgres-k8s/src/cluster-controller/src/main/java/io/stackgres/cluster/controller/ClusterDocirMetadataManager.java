/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.cluster.app.ClusterInstallationInfoHolder;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.docir.DocirConfigUtil;
import io.stackgres.common.docir.DocirMetadataManager;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ClusterDocirMetadataManager extends DocirMetadataManager {

  @Inject
  public ClusterDocirMetadataManager(
      ObjectMapper objectMapper,
      ClusterControllerPropertyContext propertyContext,
      WebClientFactory webClientFactory,
      Instance<ClusterInstallationInfoHolder> installationInfoHolder) {
    super(
        objectMapper,
        webClientFactory,
        () -> propertyContext.get(ClusterControllerProperty.CLUSTER_CONTROLLER_DOCIR_REPOSITORY_URL)
            .filter(url -> !url.isBlank())
            .map(URI::create)
            .orElseGet(() -> URI.create(DocirConfigUtil.DEFAULT_REPOSITORY_URL)),
        () -> Map.ofEntries(installationInfoHolder.get().getUserAgentHeaderEntry()),
        false);
  }

}

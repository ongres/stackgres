/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.app.WebApiInstallationInfoHolder;
import io.stackgres.apiweb.configuration.WebApiProperty;
import io.stackgres.apiweb.configuration.WebApiPropertyContext;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.docir.DocirConfigUtil;
import io.stackgres.common.docir.DocirMetadataManager;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class RestApiDocirMetadataManager extends DocirMetadataManager {

  @Inject
  public RestApiDocirMetadataManager(
      ObjectMapper objectMapper,
      WebApiPropertyContext propertyContext,
      WebClientFactory webClientFactory,
      Instance<WebApiInstallationInfoHolder> installationInfoHolder) {
    super(
        objectMapper,
        webClientFactory,
        () -> propertyContext.get(WebApiProperty.DOCIR_REPOSITORY_URL)
            .filter(url -> !url.isBlank())
            .map(URI::create)
            .orElseGet(() -> URI.create(DocirConfigUtil.DEFAULT_REPOSITORY_URL)),
        () -> Map.ofEntries(installationInfoHolder.get().getUserAgentHeaderEntry()),
        false);
  }

}

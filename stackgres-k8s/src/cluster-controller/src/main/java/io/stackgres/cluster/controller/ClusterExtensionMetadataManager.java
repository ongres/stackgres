/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.net.URI;

import com.google.common.collect.ImmutableList;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionMetadataManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterExtensionMetadataManager extends ExtensionMetadataManager {

  @Inject
  public ClusterExtensionMetadataManager(ClusterControllerPropertyContext propertyContext,
      WebClientFactory webClientFactory) {
    super(
        webClientFactory,
        Seq.of(propertyContext.getStringArray(
            ClusterControllerProperty.CLUSTER_CONTROLLER_EXTENSIONS_REPOSITORY_URLS))
            .map(URI::create)
            .collect(ImmutableList.toImmutableList()));
  }

}

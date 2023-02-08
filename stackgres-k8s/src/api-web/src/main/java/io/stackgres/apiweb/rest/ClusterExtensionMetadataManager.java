/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.apiweb.config.WebApiPropertyContext;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionMetadataManager;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterExtensionMetadataManager extends ExtensionMetadataManager {

  @Inject
  public ClusterExtensionMetadataManager(WebApiPropertyContext propertyContext,
      WebClientFactory webClientFactory) {
    super(
        webClientFactory,
        Seq.of(propertyContext.getStringArray(
            WebApiProperty.EXTENSIONS_REPOSITORY_URLS))
            .map(URI::create)
            .collect(ImmutableList.toImmutableList()));
  }

}

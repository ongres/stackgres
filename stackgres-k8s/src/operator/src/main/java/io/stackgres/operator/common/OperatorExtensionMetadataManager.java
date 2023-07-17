/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.net.URI;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class OperatorExtensionMetadataManager extends ExtensionMetadataManager {

  @Inject
  public OperatorExtensionMetadataManager(OperatorPropertyContext propertyContext,
      WebClientFactory webClientFactory) {
    super(
        webClientFactory,
        Seq.of(propertyContext.getStringArray(
            OperatorProperty.EXTENSIONS_REPOSITORY_URLS))
            .map(URI::create)
            .collect(ImmutableList.toImmutableList()));
  }

}

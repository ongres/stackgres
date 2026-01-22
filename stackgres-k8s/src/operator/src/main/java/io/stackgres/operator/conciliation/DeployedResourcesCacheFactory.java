/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class DeployedResourcesCacheFactory {

  @Produces
  public DeployedResourcesCache get(
      OperatorPropertyContext propertyContext,
      ObjectMapper objectMapper) {
    boolean useSsaSnapshot = propertyContext.getBoolean(
        OperatorProperty.RECONCILIATION_USE_SSA_SNAPSHOT);
    if (useSsaSnapshot) {
      return new DeployedResourcesSsaCache(propertyContext, objectMapper);
    }
    return new DeployedResourcesFullCache(propertyContext, objectMapper);
  }

}

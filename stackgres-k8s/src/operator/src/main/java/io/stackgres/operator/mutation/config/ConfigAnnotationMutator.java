/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.config;

import java.util.Map;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.common.ConfigReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigAnnotationMutator
    extends AbstractAnnotationMutator<StackGresConfig, ConfigReview>
    implements ConfigMutator {

  // On version removed change this code to use the oldest one
  private static final long VERSION_1_4 = StackGresVersion.V_1_4.getVersionAsNumber();

  @Override
  public Map<String, String> getAnnotationsToOverwrite(StackGresConfig resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (VERSION_1_4 > version) {
      return Map.of(StackGresContext.VERSION_KEY, StackGresVersion.V_1_4.getVersion());
    }
    return Map.of();
  }

}

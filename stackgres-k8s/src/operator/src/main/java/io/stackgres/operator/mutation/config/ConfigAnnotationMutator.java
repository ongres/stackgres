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

  private static final long LATEST = StackGresVersion.LATEST.getVersionAsNumber();

  @Override
  public Map<String, String> getAnnotationsToOverwrite(StackGresConfig resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (LATEST > version) {
      return Map.of(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    }
    return Map.of();
  }

}

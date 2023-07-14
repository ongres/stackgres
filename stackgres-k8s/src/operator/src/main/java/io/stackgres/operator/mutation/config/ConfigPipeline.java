/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.config;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.common.ConfigReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigPipeline extends AbstractMutationPipeline<StackGresConfig, ConfigReview> {

  @Inject
  public ConfigPipeline(
      @Any Instance<ConfigMutator> mutators) {
    super(mutators);
  }

}

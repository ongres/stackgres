/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.ContextPipeline;
import io.stackgres.operator.conciliation.config.StackGresConfigContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class ConfigContextPipeline
    extends ContextPipeline<StackGresConfig, Builder> {

  public ConfigContextPipeline(Instance<ContextAppender<StackGresConfig, Builder>> contextAppenders) {
    super(contextAppenders);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ScriptPipeline
    extends AbstractMutationPipeline<StackGresScript, StackGresScriptReview> {

  @Inject
  public ScriptPipeline(
      @Any Instance<ScriptMutator> mutators) {
    super(mutators);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class ScriptPipeline
    extends AbstractMutationPipeline<StackGresScript, StackGresScriptReview> {

  @Inject
  public ScriptPipeline(
      @Any Instance<ScriptMutator> mutators) {
    super(mutators);
  }

}

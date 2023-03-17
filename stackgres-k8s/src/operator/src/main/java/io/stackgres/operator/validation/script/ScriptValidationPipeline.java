/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.validation.AbstractValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

@ApplicationScoped
public class ScriptValidationPipeline extends AbstractValidationPipeline<StackGresScriptReview> {

  @Inject
  public ScriptValidationPipeline(
      @Any Instance<Validator<StackGresScriptReview>> validatorInstances) {
    super(validatorInstances);
  }

}

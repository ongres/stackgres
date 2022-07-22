/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import static io.stackgres.common.ManagedSqlUtil.DEFAULT_SCRIPT_NAME_SUFFIX;
import static io.stackgres.operatorframework.resource.ResourceUtil.getServiceAccountFromUsername;
import static io.stackgres.operatorframework.resource.ResourceUtil.isServiceAccountUsername;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class DefaultForbiddenValidator implements ScriptValidator {

  final String operatorServiceAccountName;

  @Inject
  public DefaultForbiddenValidator(OperatorPropertyContext operatorPropertyContext) {
    this.operatorServiceAccountName =
        operatorPropertyContext.getString(OperatorProperty.OPERATOR_NAME);
  }

  @Override
  public void validate(StackGresScriptReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE, UPDATE: {
        String scriptName = Optional.ofNullable(review.getRequest().getName())
            .or(() -> Optional.ofNullable(review.getRequest().getObject())
                .map(StackGresScript::getMetadata)
                .map(ObjectMeta::getName))
            .orElse(null);
        String username = review.getRequest().getUserInfo().getUsername();
        if (scriptName != null
            && scriptName.endsWith(DEFAULT_SCRIPT_NAME_SUFFIX)
            && (
                username == null
                || !isServiceAccountUsername(username)
                || !Objects.equals(
                    getServiceAccountFromUsername(username),
                    operatorServiceAccountName)
                )
            ) {
          var errorType = ErrorType.FORBIDDEN_CR_UPDATE;
          if (review.getRequest().getOperation() == Operation.CREATE) {
            errorType = ErrorType.FORBIDDEN_CR_CREATION;
          }
          fail(ErrorType.getErrorTypeUri(errorType),
              "Creation or update of default scripts is forbidden."
              + " Only SGScript which name do not end with \"" + DEFAULT_SCRIPT_NAME_SUFFIX + "\""
              + " may be created or updated");
        }
        break;
      }
      default:
    }

  }

}

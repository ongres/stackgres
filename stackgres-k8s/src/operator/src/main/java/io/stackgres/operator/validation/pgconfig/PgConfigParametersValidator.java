/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Map;

import javax.inject.Singleton;

import com.ongres.pgconfig.validator.GucValidator;
import com.ongres.pgconfig.validator.PgParameter;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.PG_CONFIG_PARAMETER)
public class PgConfigParametersValidator implements PgConfigValidator {

  @Override
  public void validate(PgConfigReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      final StackGresPostgresConfig conf = review.getRequest().getObject();
      final Map<String, String> postgresqlConf = conf.getSpec().getPostgresqlConf();
      final String postgresVersion = conf.getSpec().getPostgresVersion().split("\\.")[0];
      final GucValidator val = GucValidator.forVersion(postgresVersion);
      StatusDetailsBuilder detailsBuilder = new StatusDetailsBuilder();
      postgresqlConf.entrySet().stream()
          .forEach(e -> {
            PgParameter parameter = val.parameter(e.getKey(), e.getValue());
            if (!parameter.isValid()) {
              detailsBuilder.addNewCause(parameter.getName(),
                  parameter.getError().orElseThrow(), parameter.getHint().orElse(null));
            }
          });

      StatusDetails statusDetails = detailsBuilder.build();
      if (!statusDetails.getCauses().isEmpty()) {
        Status status = new StatusBuilder()
            .withCode(400)
            .withMessage("Postgres configuration \"" + conf.getMetadata().getName()
                + "\" has invalid parameters.")
            .withKind(conf.getKind())
            .withReason(ErrorType.getErrorTypeUri(ErrorType.PG_CONFIG_PARAMETER))
            .withDetails(statusDetails)
            .build();
        throw new ValidationFailed(status);
      }
    }
  }
}

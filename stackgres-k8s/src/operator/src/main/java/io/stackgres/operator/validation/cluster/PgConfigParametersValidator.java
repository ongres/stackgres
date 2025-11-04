/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Map;
import java.util.Optional;

import com.ongres.pgconfig.validator.GucValidator;
import com.ongres.pgconfig.validator.PgParameter;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.PG_CONFIG_PARAMETER)
public class PgConfigParametersValidator implements ClusterValidator {

  final String postgresConfField;

  public PgConfigParametersValidator() {
    postgresConfField = getFieldPath(
        StackGresPostgresConfig.class, "spec",
        StackGresPostgresConfigSpec.class, "postgresqlConf") + ".";
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      if (Optional.ofNullable(review.getRequest().getObject().getSpec().getConfigurations().getPostgres())
          .map(StackGresPostgresConfigSpec::getPostgresqlConf)
          .isEmpty()) {
        return;
      }
      final StackGresPostgresConfigSpec conf =
          review.getRequest().getObject().getSpec().getConfigurations().getPostgres();
      final Map<String, String> postgresqlConf = conf.getPostgresqlConf();
      final String postgresVersion =
          review.getRequest().getObject().getSpec().getPostgres().getVersion().split("\\.")[0];
      // TODO: Update when dependency update is available
      if (postgresVersion.equals("18")) {
        return;
      }
      final GucValidator val = GucValidator.forVersion(postgresVersion);
      StatusDetailsBuilder detailsBuilder = new StatusDetailsBuilder();
      postgresqlConf.entrySet().stream()
          .forEach(e -> {
            PgParameter parameter = val.parameter(e.getKey(), e.getValue());
            if (!parameter.isValid()) {
              detailsBuilder.addNewCause(postgresConfField + parameter.getName(),
                  parameter.getError().orElseThrow(), parameter.getHint().orElse(null));
            }
          });

      StatusDetails statusDetails = detailsBuilder.build();
      if (!statusDetails.getCauses().isEmpty()) {
        Status status = new StatusBuilder()
            .withCode(400)
            .withMessage("Postgres configuration has invalid parameters.")
            .withKind(review.getRequest().getObject().getKind())
            .withReason(ErrorType.getErrorTypeUri(ErrorType.PG_CONFIG_PARAMETER))
            .withDetails(statusDetails)
            .build();
        throw new ValidationFailed(status);
      }
    }
  }
}

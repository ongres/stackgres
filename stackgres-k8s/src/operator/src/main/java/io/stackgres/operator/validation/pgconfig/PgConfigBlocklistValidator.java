/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Set;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.PG_CONFIG_BLOCKLIST)
public class PgConfigBlocklistValidator implements PgConfigValidator {

  private static final Set<String> BLOCKLIST = PostgresBlocklist.getBlocklistParameters();

  @Override
  public void validate(PgConfigReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      final StackGresPostgresConfig conf = review.getRequest().getObject();

      String[] blocklistedProperties = conf.getSpec().getPostgresqlConf()
          .keySet().stream().filter(BLOCKLIST::contains).toArray(String[]::new);

      if (blocklistedProperties.length > 0) {
        fail(conf.getKind(), ErrorType.getErrorTypeUri(ErrorType.PG_CONFIG_BLOCKLIST),
            "Invalid postgres configuration, properties: "
                + String.join(", ", blocklistedProperties) + " cannot be settled");
      }
    }
  }
}

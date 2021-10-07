/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresBlocklist;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.PG_CONFIG_BLOCKLIST)
public class BlocklistValidator implements PgConfigValidator {

  private static final Set<String> BLOCKLIST = PostgresBlocklist.getBlocklistParameters();

  @Override
  public void validate(PgConfigReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      Map<String, String> confProperties = review.getRequest()
          .getObject().getSpec().getPostgresqlConf();

      String[] blocklistedProperties = confProperties.keySet().stream()
          .filter(BLOCKLIST::contains).toArray(String[]::new);
      int blocklistCount = blocklistedProperties.length;

      if (blocklistCount > 0) {
        throw new ValidationFailed("Invalid postgres configuration, properties: "
            + String.join(", ", blocklistedProperties) + " cannot be settled");
      }

    }
  }
}

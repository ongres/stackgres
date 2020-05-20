/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.PG_CONFIG_BLACKLIST)
public class BlacklistValidator implements PgConfigValidator {

  private static final Set<String> BLACKLIST = new HashSet<>(Arrays.asList(BLACKLIST_PROPERTIES));

  @Override
  public void validate(PgConfigReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      Map<String, String> confProperties = review.getRequest()
          .getObject().getSpec().getPostgresqlConf();

      String[] blacklistedProperties = confProperties.keySet().stream()
          .filter(BLACKLIST::contains).toArray(String[]::new);
      int blacklistCount = blacklistedProperties.length;

      if (blacklistCount > 0) {
        throw new ValidationFailed("Invalid postgres configuration, properties: "
            + String.join(", ", blacklistedProperties) + " cannot be settled");
      }

    }
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerBlocklist;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.POOL_CONFIG_BLOCKLIST)
public class PgBouncerBlocklistValidator implements PoolingValidator {

  private static final Set<String> BLOCKLIST = PgBouncerBlocklist.getBlocklistParameters();

  @Override
  public void validate(PoolingReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE || operation == Operation.UPDATE) {
      var databases = review.getRequest()
          .getObject().getSpec().getPgBouncer().getPgbouncerIni().getDatabases();

      var users = review.getRequest()
          .getObject().getSpec().getPgBouncer().getPgbouncerIni().getUsers();

      Set<String> blocklistedProperties = new HashSet<>();

      if (databases != null) {
        Set<String> collect = databases.entrySet().stream()
            .flatMap(t -> t.getValue().keySet().stream())
            .filter(BLOCKLIST::contains)
            .collect(Collectors.toSet());
        blocklistedProperties.addAll(collect);
      }

      if (users != null) {
        Set<String> collect = users.entrySet().stream()
            .flatMap(t -> t.getValue().keySet().stream())
            .filter(BLOCKLIST::contains)
            .collect(Collectors.toSet());
        blocklistedProperties.addAll(collect);
      }

      if (!blocklistedProperties.isEmpty()) {
        throw new ValidationFailed("Invalid PgBouncer configuration, properties: ["
            + String.join(", ", blocklistedProperties) + "] cannot be set");
      }

    }
  }
}

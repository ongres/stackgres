/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.ongres.pgconfig.validator.GucValidator;
import com.ongres.pgconfig.validator.PgParameter;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public class PgConfigNormalizeValuesMutator implements PgConfigMutator {

  @Override
  public StackGresPostgresConfig mutate(StackGresPostgresConfigReview review, StackGresPostgresConfig resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    var spec = resource.getSpec();
    Optional<String> version = Optional.of(spec)
        .map(StackGresPostgresConfigSpec::getPostgresVersion);

    if (version.isPresent()) {
      Optional.of(spec)
          .map(StackGresPostgresConfigSpec::getPostgresqlConf)
          .ifPresent(postgresqlConf -> {
            var normalizedPrams = normalizeParams(version.orElseThrow(), postgresqlConf);
            spec.setPostgresqlConf(normalizedPrams);
          });
    }

    return resource;
  }

  private Map<String, String> normalizeParams(
      String postgresVersion,
      Map<String, String> params) {
    if (params.size() == 0) {
      return Map.of();
    }
    final GucValidator val = GucValidator.forVersion(postgresVersion.split("\\.")[0]);
    Map<String, String> updatedParams = new HashMap<>(params);
    params.forEach((name, setting) -> {
      PgParameter parameter = val.parameter(name, setting);
      if (!Objects.equals(setting, parameter.getSetting())) {
        updatedParams.put(parameter.getName(), parameter.getSetting());
      }
      if (!Objects.equals(name, parameter.getName())) {
        updatedParams.put(parameter.getName(), parameter.getSetting());
        updatedParams.remove(name);
      }
      if (!parameter.isValid()) {
        String errorMsg = parameter.getError().orElseThrow();
        String startMsg = "unrecognized configuration parameter";
        if (errorMsg.startsWith(startMsg)) {
          updatedParams.remove(name);
        }
      }
    });
    return updatedParams;
  }
}

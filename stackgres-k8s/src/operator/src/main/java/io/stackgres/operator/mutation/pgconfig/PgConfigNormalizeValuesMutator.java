/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.ongres.pgconfig.validator.GucValidator;
import com.ongres.pgconfig.validator.PgParameter;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class PgConfigNormalizeValuesMutator implements PgConfigMutator {

  @Override
  public @NotNull List<JsonPatchOperation> mutate(PgConfigReview review) {
    Optional<StackGresPostgresConfigSpec> spec = Optional.of(review)
        .map(PgConfigReview::getRequest)
        .map(AdmissionRequest::getObject)
        .map(StackGresPostgresConfig::getSpec);
    Optional<String> version = spec.map(StackGresPostgresConfigSpec::getPostgresVersion);
    Optional<Map<String, String>> conf = spec.map(StackGresPostgresConfigSpec::getPostgresqlConf);

    if (version.isPresent() && conf.isPresent()) {
      String postgresVersion = version.orElseThrow();
      Map<String, String> postgresqlConf = conf.orElseThrow();
      return normalizeParams(postgresVersion, postgresqlConf);
    }

    return List.of();
  }

  private @NotNull List<JsonPatchOperation> normalizeParams(String postgresVersion,
      Map<String, String> params) {
    if (params.size() == 0) {
      return List.of();
    }
    final GucValidator val = GucValidator.forVersion(postgresVersion.split("\\.")[0]);
    List<JsonPatchOperation> patch = new ArrayList<>();
    params.forEach((name, setting) -> {
      PgParameter parameter = val.parameter(name, setting);
      if (!Objects.equals(setting, parameter.getSetting())) {
        patch.add(applyReplaceValue(PG_CONFIG_POINTER.append(parameter.getName()),
            TextNode.valueOf(parameter.getSetting())));
      }
      if (!Objects.equals(name, parameter.getName())) {
        patch.add(applyMoveValue(PG_CONFIG_POINTER.append(name),
            PG_CONFIG_POINTER.append(parameter.getName())));
      }
      if (!parameter.isValid()) {
        String errorMsg = parameter.getError().orElseThrow();
        String startMsg = "unrecognized configuration parameter";
        if (errorMsg.startsWith(startMsg)) {
          patch.add(applyRemoveValue(PG_CONFIG_POINTER.append(name)));
        }
      }
    });
    return List.copyOf(patch);
  }
}

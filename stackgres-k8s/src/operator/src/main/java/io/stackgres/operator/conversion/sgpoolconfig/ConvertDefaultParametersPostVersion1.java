/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgpoolconfig;

import static io.stackgres.operator.conversion.ConversionUtil.VERSION_1;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import io.stackgres.operator.conversion.Conversion;
import io.stackgres.operator.conversion.Converter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Conversion(StackGresPoolingConfig.KIND)
public class ConvertDefaultParametersPostVersion1 implements Converter {

  @Override
  public ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node) {
    if (desiredVersion >= VERSION_1 && originalVersion < VERSION_1) {
      Optional.ofNullable(node.get("status"))
          .map(status -> status.get("pgBouncer"))
          .map(ObjectNode.class::cast)
          .ifPresent(pgBouncer -> {
            ObjectNode defaultParameters = node.objectNode();
            PgBouncerDefaultValues.getDefaultValues()
                .forEach(defaultParameters::put);
            pgBouncer.set("defaultParameters", defaultParameters);
          });
    } else if (desiredVersion < VERSION_1 && originalVersion >= VERSION_1) {
      Optional.ofNullable(node.get("status"))
          .map(status -> status.get("pgBouncer"))
          .map(ObjectNode.class::cast)
          .ifPresent(pgBouncer -> {
            ObjectNode parameters = Optional.ofNullable(node.get("spec"))
                .map(spec -> spec.get("pgBouncer"))
                .map(spec -> spec.get("pgbouncer.ini"))
                .map(spec -> spec.has("pgbouncer") ? spec.get("pgbouncer") : spec)
                .map(ObjectNode.class::cast)
                .orElse(node.objectNode());
            ArrayNode defaultParameters = node.arrayNode();
            PgBouncerDefaultValues.getDefaultValues()
                .entrySet()
                .stream()
                .filter(defaultParameter -> !parameters.has(defaultParameter.getKey())
                    || parameters.get(defaultParameter.getKey())
                        .asText().equals(defaultParameter.getValue()))
                .forEach(defaultParameter -> defaultParameters.add(defaultParameter.getKey()));
            pgBouncer.set("defaultParameters", defaultParameters);
          });
    }
    return node;
  }

}

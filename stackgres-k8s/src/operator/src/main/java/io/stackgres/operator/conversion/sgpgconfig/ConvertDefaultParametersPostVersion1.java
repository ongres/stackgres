/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgpgconfig;

import static io.stackgres.operator.conversion.ConversionUtil.VERSION_1;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.operator.conversion.Conversion;
import io.stackgres.operator.conversion.Converter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Conversion(StackGresPostgresConfig.KIND)
public class ConvertDefaultParametersPostVersion1 implements Converter {

  @Override
  public ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node) {
    if (desiredVersion >= VERSION_1 && originalVersion < VERSION_1) {
      Optional.ofNullable(node.get("status"))
          .map(ObjectNode.class::cast)
          .ifPresent(status -> {
            String pgVersion = Optional.ofNullable(node.get("spec"))
                .map(spec -> spec.get("postgresVersion"))
                .map(JsonNode::textValue)
                .orElseThrow();

            ObjectNode defaultParameters = node.objectNode();
            PostgresDefaultValues.getDefaultValues(pgVersion)
                .forEach(defaultParameters::put);
            status.set("defaultParameters", defaultParameters);
          });
    } else if (desiredVersion < VERSION_1 && originalVersion >= VERSION_1) {
      Optional.ofNullable(node.get("status"))
          .map(ObjectNode.class::cast)
          .ifPresent(status -> {
            String pgVersion = Optional.ofNullable(node.get("spec"))
                .map(spec -> spec.get("postgresVersion"))
                .map(JsonNode::textValue)
                .orElseThrow();

            ObjectNode parameters = Optional.ofNullable(node.get("spec"))
                .map(spec -> spec.get("postgresql.conf"))
                .map(ObjectNode.class::cast)
                .orElse(node.objectNode());
            ArrayNode defaultParameters = node.arrayNode();
            PostgresDefaultValues.getDefaultValues(pgVersion)
                .entrySet()
                .stream()
                .filter(defaultParameter -> !parameters.has(defaultParameter.getKey())
                    || parameters.get(defaultParameter.getKey())
                        .asText().equals(defaultParameter.getValue()))
                .forEach(defaultParameter -> defaultParameters.add(defaultParameter.getKey()));
            status.set("defaultParameters", defaultParameters);
          });
    }
    return node;
  }

}

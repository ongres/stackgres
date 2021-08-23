/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgcluster;

import static io.stackgres.operator.conversion.ConversionUtil.VERSION_1;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conversion.Conversion;
import io.stackgres.operator.conversion.Converter;

@ApplicationScoped
@Conversion(StackGresCluster.KIND)
public class ConvertUpgradePostgresVersionPostVersion1 implements Converter {

  @Inject
  ObjectMapper mapper;

  @Override
  public ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node) {
    if (desiredVersion >= VERSION_1 && originalVersion < VERSION_1) {
      Optional.ofNullable(node.get("spec"))
          .map(ObjectNode.class::cast)
          .ifPresent(spec -> {
            String pgVersion = spec.get("postgresVersion").asText();
            if (pgVersion.equals("latest") || pgVersion.equals("12")) {
              spec.set("postgresVersion", mapper.convertValue(
                  "12.6", JsonNode.class));
            } else if (pgVersion.equals("11")) {
              spec.set("postgresVersion", mapper.convertValue(
                  "11.11", JsonNode.class));
            }
          });
    }
    return node;
  }

}

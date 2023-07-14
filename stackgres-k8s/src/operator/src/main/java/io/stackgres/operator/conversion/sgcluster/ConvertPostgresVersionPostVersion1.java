/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgcluster;

import static io.stackgres.operator.conversion.ConversionUtil.VERSION_1;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conversion.Conversion;
import io.stackgres.operator.conversion.Converter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Conversion(StackGresCluster.KIND)
public class ConvertPostgresVersionPostVersion1 implements Converter {

  @Override
  public ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node) {
    if (desiredVersion >= VERSION_1 && originalVersion < VERSION_1) {
      Optional.ofNullable(node.get("spec"))
          .map(ObjectNode.class::cast)
          .ifPresent(spec -> {
            String postgresVersion = spec.get("postgresVersion").asText();
            ObjectNode postgres = node.objectNode();
            if (postgresVersion.equals("latest") || postgresVersion.equals("12")) {
              postgres.put("version", "12.6");
            } else if (postgresVersion.equals("11")) {
              postgres.put("version", "11.11");
            } else {
              postgres.put("version", postgresVersion);
            }
            spec.set("postgres", postgres);
            spec.remove("postgresVersion");
          });
    } else if (desiredVersion < VERSION_1 && originalVersion >= VERSION_1) {
      Optional.ofNullable(node.get("spec"))
          .map(ObjectNode.class::cast)
          .ifPresent(spec -> {
            String postgresVersion = spec.get("postgres").get("version").asText();
            spec.put("postgresVersion", postgresVersion);
            spec.remove("postgres");
          });
    }
    return node;
  }

}

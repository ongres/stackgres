/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion.sgpoolconfig;

import static io.stackgres.operator.conversion.ConversionUtil.VERSION_1;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.conversion.Conversion;
import io.stackgres.operator.conversion.Converter;

@ApplicationScoped
@Conversion(StackGresPoolingConfig.KIND)
public class PgBouncerSpecMigrationPostVersion1 implements Converter {

  @Override
  public ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node) {

    if (desiredVersion >= VERSION_1 && originalVersion < VERSION_1) {
      Optional.ofNullable(node.get("spec"))
          .map(spec -> spec.get("pgBouncer"))
          .map(pgBouncer -> pgBouncer.get("pgbouncer.ini"))
          .ifPresent(pgBouncerIni -> {
            ObjectNode pgBouncer = (ObjectNode) node.get("spec").get("pgBouncer");
            pgBouncer.remove("pgbouncer.ini");
            ObjectNode newPgBouncerIni = node.objectNode();
            newPgBouncerIni.set("pgbouncer", pgBouncerIni);
            pgBouncer.set("pgbouncer.ini", newPgBouncerIni);
          });
    } else if (desiredVersion < VERSION_1 && originalVersion >= VERSION_1) {
      Optional.ofNullable(node.get("spec"))
          .map(spec -> spec.get("pgBouncer"))
          .map(pgBouncer -> pgBouncer.get("pgbouncer.ini"))
          .map(ObjectNode.class::cast)
          .ifPresent(pgBouncerIni -> {
            ObjectNode innerPgBouncer = Optional.ofNullable(pgBouncerIni.get("pgbouncer"))
                .map(ObjectNode.class::cast)
                .orElseGet(node::objectNode);
            Converter.removeFieldIfExists(pgBouncerIni, "pgbouncer");
            Converter.removeFieldIfExists(pgBouncerIni, "users");
            Converter.removeFieldIfExists(pgBouncerIni, "databases");
            ObjectNode pgBouncer = (ObjectNode) node.get("spec").get("pgBouncer");
            Converter.removeFieldIfExists(pgBouncer, "pgbouncer.ini");
            pgBouncer.set("pgbouncer.ini", innerPgBouncer);

          });
    }
    return node;
  }
}

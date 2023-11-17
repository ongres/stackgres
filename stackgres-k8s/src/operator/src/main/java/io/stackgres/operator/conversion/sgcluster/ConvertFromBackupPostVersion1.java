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
public class ConvertFromBackupPostVersion1 implements Converter {

  @Override
  public ObjectNode convert(long originalVersion, long desiredVersion, ObjectNode node) {
    if (desiredVersion >= VERSION_1 && originalVersion < VERSION_1) {
      Optional.ofNullable(node.get("spec"))
          .map(spec -> spec.get("initialData"))
          .map(initialData -> initialData.get("restore"))
          .map(ObjectNode.class::cast)
          .ifPresent(restore -> {
            String backupUid = restore.get("fromBackup").asText();
            ObjectNode fromBackup = node.objectNode();
            fromBackup.put("uid", backupUid);
            restore.set("fromBackup", fromBackup);
          });
    } else if (desiredVersion < VERSION_1 && originalVersion >= VERSION_1) {
      Optional.ofNullable(node.get("spec"))
          .map(spec -> spec.get("initialData"))
          .map(initialData -> initialData.get("restore"))
          .map(ObjectNode.class::cast)
          .ifPresent(restore -> {
            String backupUid = restore.get("fromBackup").get("uid").asText();
            restore.put("fromBackup", backupUid);
          });
    }
    return node;
  }

}

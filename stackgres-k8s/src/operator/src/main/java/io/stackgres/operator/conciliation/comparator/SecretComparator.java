/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.zjsonpatch.JsonDiff;
import io.stackgres.operatorframework.resource.ResourceUtil;

public class SecretComparator extends DefaultComparator {

  private static Map<String, String> getData(Secret secret) {
    Map<String, String> data = new HashMap<>();
    if (secret.getStringData() != null) {
      final Map<String, String> secretStringData = new HashMap<>(secret.getStringData());
      secretStringData.replaceAll((key, value) -> ResourceUtil.encodeSecret(value));
      data.putAll(secretStringData);
    }
    final Map<String, String> secretData = secret.getData();
    if (secretData != null) {
      data.putAll(secretData);
    }
    return data;
  }

  @Override
  public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {
    if (!(required instanceof Secret && deployed instanceof Secret)) {
      throw new IllegalArgumentException();
    }
    Secret s1 = (Secret) required;
    Secret s2 = (Secret) deployed;
    JsonNode metadataDiff = JsonDiff.asJson(PATCH_MAPPER.valueToTree(required.getMetadata()),
        PATCH_MAPPER.valueToTree(deployed.getMetadata()));

    if (metadataDiff.size() > 0) {
      return false;
    }
    return isDataEqual(s1, s2);
  }

  private boolean isDataEqual(Secret required, Secret deployed) {
    var r1Data = getData(required);
    var r2Data = getData(deployed);
    return r1Data.equals(r2Data);
  }

}

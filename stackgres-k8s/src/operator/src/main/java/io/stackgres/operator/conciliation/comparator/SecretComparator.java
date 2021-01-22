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
import io.fabric8.kubernetes.client.internal.PatchUtils;
import io.fabric8.zjsonpatch.JsonDiff;
import io.stackgres.operatorframework.resource.ResourceUtil;

public class SecretComparator implements ResourceComparator {

  @Override
  public boolean isResourceContentEqual(HasMetadata r1, HasMetadata r2) {
    if (!(r1 instanceof Secret && r2 instanceof Secret)) {
      throw new IllegalArgumentException();
    }
    Secret s1 = (Secret) r1;
    Secret s2 = (Secret) r2;
    JsonNode metadataDiff = JsonDiff.asJson(PatchUtils.patchMapper().valueToTree(r1.getMetadata()),
        PatchUtils.patchMapper().valueToTree(r2.getMetadata()));

    if (metadataDiff.size() > 0) {
      return false;
    }
    return isDataEqual(s1, s2);
  }

  private boolean isDataEqual(Secret r1, Secret r2) {
    var r1Data = getData(r1);
    var r2Data = getData(r2);
    return r1Data.equals(r2Data);
  }

  private Map<String, String> getData(Secret secret) {
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

}

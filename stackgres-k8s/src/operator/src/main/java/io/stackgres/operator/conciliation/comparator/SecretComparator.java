/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.operatorframework.resource.ResourceUtil;

public class SecretComparator extends StackGresAbstractComparator {

  private static final IgnorePatch[] IGNORE_PATCH_PATTERNS = {
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATCH_PATTERNS;
  }

  @Override
  public ArrayNode getRawJsonDiff(HasMetadata required, HasMetadata deployed) {
    return super.getRawJsonDiff(encodeSecret(required), encodeSecret(deployed));
  }

  private Secret encodeSecret(HasMetadata resource) {
    if (!(resource instanceof Secret)) {
      throw new IllegalArgumentException("Resource "
          + resource.getKind() + ":" + resource.getMetadata().getName() + " is not a Secret");
    }
    Secret secret = (Secret) resource;
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
    return new SecretBuilder(secret)
        .withStringData(null)
        .withData(data)
        .build();
  }

}

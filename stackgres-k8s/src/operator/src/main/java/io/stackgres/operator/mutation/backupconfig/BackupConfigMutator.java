/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface BackupConfigMutator extends JsonPatchMutator<BackupConfigReview> {

  JsonPointer SPEC_POINTER = JsonPointer.of("spec");

  static String getJsonMappingField(String field, Class<?> clazz) throws NoSuchFieldException {
    return clazz.getDeclaredField(field)
        .getAnnotation(JsonProperty.class)
        .value();
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import com.github.fge.jackson.jsonpointer.JsonPointer;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operatorframework.JsonPatchMutator;

public interface BackupMutator extends JsonPatchMutator<BackupConfigReview> {

  JsonPointer SG_BACKUP_CONFIG_POINTER = JsonPointer.of("spec");
}

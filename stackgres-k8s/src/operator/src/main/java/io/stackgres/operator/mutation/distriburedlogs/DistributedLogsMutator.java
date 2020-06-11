/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distriburedlogs;

import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface DistributedLogsMutator extends JsonPatchMutator<StackGresDistributedLogsReview> {
}

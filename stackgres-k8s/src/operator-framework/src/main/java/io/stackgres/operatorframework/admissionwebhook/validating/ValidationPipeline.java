/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.validating;

import org.jetbrains.annotations.NotNull;

public interface ValidationPipeline<T> {

  void validate(@NotNull T review) throws ValidationFailed;

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework;

import io.stackgres.operatorframework.ValidationFailed;

public interface ValidationPipeline<T> {

  void validate(T review) throws ValidationFailed;

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import io.stackgres.common.OperatorProperty;
import io.stackgres.operatorframework.resource.EventReason;

public interface OperatorEventReason extends EventReason {

  @Override
  default String component() {
    return OperatorProperty.OPERATOR_NAME.getString();
  }

}

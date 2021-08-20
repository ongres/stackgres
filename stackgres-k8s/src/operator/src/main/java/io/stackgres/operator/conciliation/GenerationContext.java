/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.stackgres.operator.common.StackGresVersion;

public interface GenerationContext<T> {

  T getSource();

  StackGresVersion getVersion();

}

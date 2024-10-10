/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgconfig.StackGresConfig;

public interface ConfigContext {
  
  StackGresContext getContext();

  StackGresConfig getConfig();

}

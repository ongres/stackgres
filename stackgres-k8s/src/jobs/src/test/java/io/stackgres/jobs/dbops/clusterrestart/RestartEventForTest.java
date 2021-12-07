/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import org.immutables.value.Value;

@Value.Immutable
public interface RestartEventForTest extends RestartEvent {

  @Value.Default
  @Override
  default String getMessage() {
    return getEventType().toString();
  }

}

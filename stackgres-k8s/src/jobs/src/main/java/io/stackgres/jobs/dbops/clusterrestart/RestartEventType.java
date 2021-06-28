/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

public enum RestartEventType {

  POD_CREATED,
  POD_RESTART,
  SWITCHOVER,
  POSTGRES_RESTART;
}

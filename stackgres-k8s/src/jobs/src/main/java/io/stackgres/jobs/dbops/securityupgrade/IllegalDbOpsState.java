/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

public class IllegalDbOpsState extends RuntimeException {

  public IllegalDbOpsState(String message) {
    super(message);
  }
}

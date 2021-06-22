/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

public class IllegalDbOpsState extends RuntimeException {

  static final long serialVersionUID = 0L;

  public IllegalDbOpsState(String message) {
    super(message);
  }
}

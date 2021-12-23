/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

public class InvalidClusterException extends RuntimeException {

  static final long serialVersionUID = 0L;

  public InvalidClusterException(String message) {
    super(message);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

public class FailoverException extends RuntimeException {

  public FailoverException(String message) {
    super(message);
  }
}

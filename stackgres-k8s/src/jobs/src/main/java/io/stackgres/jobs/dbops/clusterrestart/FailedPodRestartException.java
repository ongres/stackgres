/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

public class FailedPodRestartException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public FailedPodRestartException(String message) {
    super(message);
  }

  public FailedPodRestartException(Throwable e) {
    super(e.getMessage(), e);
  }

}

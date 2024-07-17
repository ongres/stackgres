/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.lock;

public class RetryLockException extends RuntimeException {

  public RetryLockException() {
    super();
  }

  public RetryLockException(Throwable cause) {
    super(cause);
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.security.SecureRandom;

public interface RetryUtil {

  static int calculateExponentialBackoffDelay(int initial, int maximum, int variation, int retry) {
    return (int) Math.min(maximum, initial * Math.pow(Math.E, retry))
        + (variation - new SecureRandom().nextInt(2 * variation));
  }

}

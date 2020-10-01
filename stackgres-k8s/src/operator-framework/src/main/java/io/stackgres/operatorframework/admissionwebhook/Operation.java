/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook;

import java.util.Locale;

public enum Operation {

  CREATE, UPDATE, DELETE, CONNECT;

  @Override
  public String toString() {
    return name().toLowerCase(Locale.US);
  }

}

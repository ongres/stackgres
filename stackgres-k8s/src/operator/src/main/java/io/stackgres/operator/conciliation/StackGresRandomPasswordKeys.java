/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

public interface StackGresRandomPasswordKeys {

  String SUPERUSER_PASSWORD_KEY = "superuser-password";
  String REPLICATION_PASSWORD_KEY = "replication-password";
  String AUTHENTICATOR_PASSWORD_KEY = "authenticator-password";
  String RESTAPI_PASSWORD_KEY = "restapi-password";
}

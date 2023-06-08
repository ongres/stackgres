/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

public interface DistributedLogsPatroniEnvironmentVariablesFactory {
  String V09_PATRONI_ENV_VAR_FACTORY = "V09_PATRONI_ENV_VAR_FACTORY";
  String V09_COMMON_ENV_VAR_FACTORY = "V09_COMMON_ENV_VAR_FACTORY";
  String LATEST_PATRONI_ENV_VAR_FACTORY = "LATEST_PATRONI_ENV_VAR_FACTORY";
}

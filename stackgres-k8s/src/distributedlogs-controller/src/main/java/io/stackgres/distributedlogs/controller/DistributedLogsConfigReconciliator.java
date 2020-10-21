/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.io.IOException;

public interface DistributedLogsConfigReconciliator {

  String getFluentdConfigHash();

  void reloadFluentdConfiguration() throws IOException;

}

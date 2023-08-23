/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

public interface OperatorWatchersHandler {
  void startWatchers();

  void stopWatchers();
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import io.stackgres.operator.conciliation.AbstractReconciliator;

public interface OperatorLockHolder {

  boolean isLeader();

  void register(AbstractReconciliator<?, ?> reconciliator);

  void start();

  void startReconciliation();

  void stop();

  void forceUnlockOthers();

}

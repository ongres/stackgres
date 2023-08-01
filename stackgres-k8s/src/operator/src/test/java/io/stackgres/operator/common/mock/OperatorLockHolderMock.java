/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.mock;

import io.quarkus.test.Mock;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.OperatorLockHolder;

@Mock
public class OperatorLockHolderMock implements OperatorLockHolder {

  @Override
  public boolean isLeader() {
    return true;
  }

  @Override
  public void register(AbstractReconciliator<?> reconciliator) {
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

}

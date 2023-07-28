/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.mock;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.Mock;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.OperatorLockReconciliator;

@Mock
public class OperatorLockReconciliatorMock extends OperatorLockReconciliator {

  @Inject
  public OperatorLockReconciliatorMock() {
    super(null, null, null);
  }

  @Override
  public boolean isLeader() {
    return true;
  }

  @Override
  public void register(AbstractReconciliator<?> reconciliator) {
  }

  @Override
  protected void onStart(@Observes StartupEvent ev) {
  }

  @Override
  protected void onStop(@Observes ShutdownEvent ev) throws Exception {
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OperatorLockReconciliator {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      OperatorLockReconciliator.class.getPackage().getName());

  private final CustomResourceScanner<StackGresConfig> scanner;
  private final CustomResourceScheduler<StackGresConfig> scheduler;
  private final OperatorPropertyContext context;
  private final ScheduledExecutorService executorService;

  private final AtomicBoolean leader = new AtomicBoolean(false);
  private final List<AbstractReconciliator<?>> reconciliators = new ArrayList<>();

  protected OperatorLockReconciliator(
      CustomResourceScanner<StackGresConfig> scanner,
      CustomResourceScheduler<StackGresConfig> scheduler,
      OperatorPropertyContext context) {
    this.scanner = scanner;
    this.context = context;
    this.scheduler = scheduler;
    this.executorService = Executors.newSingleThreadScheduledExecutor(
        r -> new Thread(r, "OperatorLockReconciliationLoop"));
  }

  public boolean isLeader() {
    return leader.get();
  }

  public void register(AbstractReconciliator<?> reconciliator) {
    this.reconciliators.add(reconciliator);
    if (leader.get()) {
      this.reconciliators.forEach(AbstractReconciliator::reconcileAll);
    }
  }

  protected void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Starting Operator Lock Reconciliation");
    executorService.scheduleWithFixedDelay(this::reconciliationCycle,
        context.getInt(OperatorProperty.LOCK_POLL_INTERVAL),
        context.getInt(OperatorProperty.LOCK_POLL_INTERVAL),
        TimeUnit.SECONDS);
  }

  protected void onStop(@Observes ShutdownEvent ev) throws Exception {
    LOGGER.info("Stopping Operator Lock Reconciliation");
    executorService.shutdown();
    executorService.awaitTermination(30, TimeUnit.SECONDS);
  }

  private void reconciliationCycle() {
    try {
      List<StackGresConfig> configs = scanner.getResources();
      if (configs.size() == 0) {
        throw new IllegalArgumentException("No SGConfig found. Please create an SGConfig for the"
            + " operator to function properly");
      }
      if (configs.size() > 1) {
        throw new IllegalArgumentException("More than one SGConfig found. Please remove extra"
            + " SGConfig for the operator to function properly");
      }
      StackGresConfig config = configs.get(0);

      final String serviceAccount = context.getString(OperatorProperty.OPERATOR_SERVICE_ACCOUNT);
      final String podName = context.getString(OperatorProperty.OPERATOR_POD_NAME);
      final int lockTimeout = context.getInt(OperatorProperty.LOCK_TIMEOUT);
      scheduler.update(config, foundConfig -> {
        if (!StackGresUtil.isLockedByMe(config, podName)) {
          if (leader.get()) {
            LOGGER.warn("Lock on SGConfig was lost");
            leader.set(false);
          }
          if (StackGresUtil.isLocked(config, lockTimeout)) {
            LOGGER.warn("Waiting for the lock on SGConfig to be released");
            return;
          }
        }
        StackGresUtil.setLock(config,
            serviceAccount,
            podName,
            System.currentTimeMillis() / 1000);
        if (!leader.get()) {
          LOGGER.info("Lock on SGConfig was set. I am the leader!");
          leader.set(true);
          this.reconciliators.forEach(AbstractReconciliator::reconcileAll);
        }
      });
    } catch (Exception ex) {
      LOGGER.error("Reconciliation of operator lock failed", ex);
    }
  }

}

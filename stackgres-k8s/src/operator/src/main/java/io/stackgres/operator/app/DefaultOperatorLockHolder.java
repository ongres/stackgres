/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.runtime.Quarkus;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultOperatorLockHolder implements OperatorLockHolder {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      DefaultOperatorLockHolder.class.getPackage().getName());

  private final CustomResourceScanner<StackGresConfig> scanner;
  private final CustomResourceScheduler<StackGresConfig> scheduler;
  private final OperatorPropertyContext context;
  private final ScheduledExecutorService executorService;

  private final AtomicBoolean leader = new AtomicBoolean(false);
  private final AtomicBoolean doReconciliation = new AtomicBoolean(false);
  private final List<AbstractReconciliator<?, ?>> reconciliators = new ArrayList<>();

  protected DefaultOperatorLockHolder(
      CustomResourceScanner<StackGresConfig> scanner,
      CustomResourceScheduler<StackGresConfig> scheduler,
      OperatorPropertyContext context) {
    this.scanner = scanner;
    this.context = context;
    this.scheduler = scheduler;
    this.executorService = Executors.newSingleThreadScheduledExecutor(
        r -> new Thread(r, "OperatorLockHolder"));
  }

  @Override
  public boolean isLeader() {
    return leader.get();
  }

  @Override
  public void register(AbstractReconciliator<?, ?> reconciliator) {
    this.reconciliators.add(reconciliator);
    if (leader.get()) {
      if (doReconciliation.get()) {
        this.reconciliators.forEach(AbstractReconciliator::reconcileAll);
      }
    }
  }

  @Override
  public void startReconciliation() {
    doReconciliation.set(true);
    this.reconciliators.forEach(AbstractReconciliator::reconcileAll);
  }

  @Override
  public void start() {
    LOGGER.info("Starting Operator Lock Reconciliation");
    executorService.scheduleWithFixedDelay(this::tryHoldLock,
        0,
        context.getInt(OperatorProperty.LOCK_POLL_INTERVAL),
        TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    if (!executorService.isShutdown()) {
      LOGGER.info("Stopping Operator Lock Reconciliation");
      executorService.shutdown();
    }
    try {
      executorService.awaitTermination(
          context.getInt(OperatorProperty.LOCK_POLL_INTERVAL),
          TimeUnit.SECONDS);
    } catch (Exception ex) {
      LOGGER.error("An error occurred during shutdown of operator lock reconciliator", ex);
    }
    releaseLock();
  }

  @Override
  public void forceUnlockOthers() {
    if (!isLeader()) {
      List<StackGresConfig> configs = scanner.getResources();
      if (configs.size() == 0) {
        throw new IllegalArgumentException("No SGConfig found. Please create an SGConfig for the"
            + " operator to function properly");
      }
      if (configs.size() > 1) {
        throw new IllegalArgumentException("More than one SGConfig found. Please remove extra"
            + " SGConfig for the operator to function properly");
      }
      StackGresConfig config = configs.getFirst();

      final String podName = context.getString(OperatorProperty.OPERATOR_POD_NAME);
      scheduler.update(config, foundConfig -> {
        if (!StackGresUtil.isLockedBy(foundConfig, podName)) {
          if (StackGresUtil.isLocked(foundConfig)) {
            StackGresUtil.resetLock(foundConfig);
            LOGGER.info("Lock on SGConfig was released forcibly");
          } else {
            LOGGER.info("Lock on SGConfig was already release");
          }
        } else {
          LOGGER.info("Lock on SGConfig is already locked by me");
        }
      });
    }
  }

  private void tryHoldLock() {
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
      StackGresConfig config = configs.getFirst();

      final String serviceAccount = context.getString(OperatorProperty.OPERATOR_SERVICE_ACCOUNT);
      final String podName = context.getString(OperatorProperty.OPERATOR_POD_NAME);
      final int lockDuration = context.getInt(OperatorProperty.LOCK_DURATION);
      scheduler.update(config, foundConfig -> {
        if (!StackGresUtil.isLockedBy(foundConfig, podName)) {
          if (leader.get()) {
            LOGGER.warn("Lock on SGConfig was lost");
            leader.set(false);
            if (context.getBoolean(OperatorProperty.FORCE_UNLOCK_OPERATOR)) {
              LOGGER.warn("Lock on SGConfig was lost while forcing unlock operator, exiting");
              Quarkus.asyncExit(1);
            }
          }
          if (StackGresUtil.isLocked(foundConfig)) {
            LOGGER.warn("Waiting for the lock on SGConfig to be released");
            return;
          }
        }
        StackGresUtil.setLock(
            foundConfig,
            serviceAccount,
            podName,
            lockDuration);
        if (!leader.get()) {
          LOGGER.info("Lock on SGConfig was set. I am the leader!");
          leader.set(true);
          if (doReconciliation.get()) {
            this.reconciliators.forEach(AbstractReconciliator::reconcileAll);
          }
        }
      });
    } catch (Exception ex) {
      LOGGER.error("Reconciliation of operator lock failed", ex);
    }
  }

  private void releaseLock() {
    List<StackGresConfig> configs = scanner.getResources();
    if (configs.size() == 0) {
      throw new IllegalArgumentException("No SGConfig found. Please create an SGConfig for the"
          + " operator to function properly");
    }
    if (configs.size() > 1) {
      throw new IllegalArgumentException("More than one SGConfig found. Please remove extra"
          + " SGConfig for the operator to function properly");
    }
    StackGresConfig config = configs.getFirst();
    final String podName = context.getString(OperatorProperty.OPERATOR_POD_NAME);
    scheduler.update(config, foundConfig -> {
      if (StackGresUtil.isLockedBy(foundConfig, podName)) {
        StackGresUtil.resetLock(foundConfig);
        LOGGER.info("Lock on SGConfig was released");
        if (leader.get()) {
          leader.set(false);
        }
      } else {
        LOGGER.info("Lock on SGConfig was already unlocked");
        if (leader.get()) {
          leader.set(false);
        }
      }
    });
  }

}

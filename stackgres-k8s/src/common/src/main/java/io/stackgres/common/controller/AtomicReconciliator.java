/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.controller;

/**
 * Reconciliation interface for simple reconciliation loops.
 * It assumes that the reconciliation doesn't need any kind of context per reconciliation cycle,
 * instead the implementation should be able to find any additional information that it needs
 * from its configuration.
 * This is the case for reconciliations loops inside components like pods local controller
 */
public interface AtomicReconciliator {

  /**
   * Method to be called per reconciliation cycle.
   */
  void reconcile();
}

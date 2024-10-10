/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

/**
 * Restrict a factory bound with {@link OperatorVersionBinder} to the clusters that retrieve
 * their images from the StackGres images registry ({@code spec.configurations.registry.enabled})
 * or to the ones that use the images bundled with the operator.
 */
public enum RegistryBinding {
  ANY,
  ENABLED,
  DISABLED;
}

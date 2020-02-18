/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;

import org.immutables.value.Value.Immutable;

@Immutable
public interface StackGresGeneratorContext {

  StackGresClusterContext getClusterContext();

  ImmutableList<HasMetadata> getExistingResources();

}

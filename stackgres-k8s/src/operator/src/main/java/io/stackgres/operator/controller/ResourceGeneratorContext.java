/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterConfig;

import org.immutables.value.Value.Immutable;

@Immutable
public interface ResourceGeneratorContext {

  StackGresClusterConfig getClusterConfig();

  ImmutableList<HasMetadata> getExistingResources();

}

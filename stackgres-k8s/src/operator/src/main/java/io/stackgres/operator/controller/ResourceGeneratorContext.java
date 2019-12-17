/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;

import org.immutables.value.Value.Immutable;

@Immutable
public interface ResourceGeneratorContext<T> {

  T getConfig();

  ImmutableList<HasMetadata> getExistingResources();

}

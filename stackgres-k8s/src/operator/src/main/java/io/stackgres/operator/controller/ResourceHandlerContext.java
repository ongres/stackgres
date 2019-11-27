/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterConfig;

import org.jooq.lambda.tuple.Tuple2;

public interface ResourceHandlerContext {

  StackGresClusterConfig getClusterConfig();

  ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getExistingResources();

  ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getRequiredResources();

}

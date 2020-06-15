/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.jooq.lambda.tuple.Tuple2;

public interface ResourceHandlerContext {

  ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getExistingResources();

  ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getRequiredResources();

  ImmutableMap<String, String> getLabels();

}

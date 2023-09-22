/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface EnvVarContext<R extends HasMetadata> {

  R getResource();

  Map<String, String> getEnvironmentVariables();

}

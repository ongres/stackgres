/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import org.jetbrains.annotations.NotNull;

public interface LabelFactoryForConfig
    extends LabelFactory<StackGresConfig> {

  Map<String, String> restapiLabels(@NotNull StackGresConfig resource);

  Map<String, String> grafanaIntegrationLabels(@NotNull StackGresConfig resource);

  Map<String, String> collectorLabels(@NotNull StackGresConfig resource);

  Map<String, String> configCrossNamespaceLabels(@NotNull StackGresConfig resource);

  @Override
  LabelMapperForConfig labelMapper();

}

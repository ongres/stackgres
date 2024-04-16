/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import javax.annotation.Nonnull;

import io.stackgres.common.crd.sgconfig.StackGresConfig;

public interface LabelFactoryForConfig
    extends LabelFactory<StackGresConfig> {

  Map<String, String> restapiLabels(@Nonnull StackGresConfig resource);

  Map<String, String> grafanaIntegrationLabels(@Nonnull StackGresConfig resource);

  @Override
  LabelMapperForConfig labelMapper();

}

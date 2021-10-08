/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import org.jetbrains.annotations.NotNull;

public interface PostgresConfigurationFactory
    extends DefaultCustomResourceFactory<StackGresPostgresConfig> {

  @NotNull String getPostgresVersion();
}

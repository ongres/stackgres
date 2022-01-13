/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import org.immutables.value.Value;

@Value.Immutable
public interface ExtensionRequest {
  String getPostgresVersion();

  StackGresComponent getStackGresComponent();

  StackGresClusterExtension getExtension();

  StackGresVersion stackGresVersion();

  Optional<String> getOs();

  Optional<String> getArch();
}

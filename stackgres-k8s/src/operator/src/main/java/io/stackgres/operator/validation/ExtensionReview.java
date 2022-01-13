/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import org.immutables.value.Value;
import org.jooq.lambda.tuple.Tuple2;

@Value.Immutable
public interface ExtensionReview {
  Optional<String> getArch();

  Optional<String> getOs();

  String getPostgresVersion();

  StackGresComponent getPostgresFlavor();

  StackGresVersion getStackGresVersion();

  List<Tuple2<String, Optional<String>>> getDefaultExtensions();

  List<StackGresClusterExtension> getRequiredExtensions();

  List<StackGresClusterInstalledExtension> getToInstallExtensions();
}

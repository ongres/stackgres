/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import org.immutables.value.Value;

@Value.Immutable
public interface PostgresContainerContext extends ContainerContext {

  String getPostgresVersion();

  String getPostgresMajorVersion();

  String getImageBuildMajorVersion();

  Optional<String> getOldPostgresVersion();

  Optional<String> getOldMajorVersion();

  Optional<String> getOldImageBuildMajorVersion();

  List<StackGresClusterInstalledExtension> getInstalledExtensions();

}

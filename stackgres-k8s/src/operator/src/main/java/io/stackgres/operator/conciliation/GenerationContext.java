/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;

import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.StackGresVersion;

public interface GenerationContext<T> {

  T getSource();

  StackGresVersion getVersion();

  Optional<VersionInfo> getKubernetesVersion();

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import org.jetbrains.annotations.NotNull;

public interface PatroniApiMetadataFinder {

  PatroniApiMetadata findPatroniRestApi(@NotNull String clusterName, @NotNull String namespace);
}

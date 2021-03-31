/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import org.immutables.value.Value;

@Value.Immutable
public interface PatroniApiMetadata {

  String getHost();

  int getPort();

  String getUsername();

  String getPassword();
}

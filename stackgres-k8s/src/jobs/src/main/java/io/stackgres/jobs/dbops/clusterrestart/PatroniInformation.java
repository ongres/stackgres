/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import org.immutables.value.Value;

@Value.Immutable
public interface PatroniInformation {

  MemberState getState();

  MemberRole getRole();

  int getServerVersion();

  String getPatroniVersion();

  String getPatroniScope();
}

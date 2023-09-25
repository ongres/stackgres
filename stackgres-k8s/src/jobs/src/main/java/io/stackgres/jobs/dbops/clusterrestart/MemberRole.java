/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

public enum MemberRole {

  LEADER, REPLICA;

  static MemberRole fromString(String role) {
    return switch (role) {
      case "leader", "master", "standby_leader" -> MemberRole.LEADER;
      case "replica" -> MemberRole.REPLICA;
      default -> null;
    };
  }

}

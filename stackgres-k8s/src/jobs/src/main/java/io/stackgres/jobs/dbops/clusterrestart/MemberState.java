/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

public enum MemberState {

  RUNNING, STOPPED, STARTING, RESTARTING;

  static MemberState fromString(String state) {
    return switch (state) {
      case "running", "streaming", "in archive recovery" -> MemberState.RUNNING;
      case "stopped" -> MemberState.STOPPED;
      case "starting" -> MemberState.STARTING;
      case "restarting" -> MemberState.RESTARTING;
      default -> null;
    };
  }

}

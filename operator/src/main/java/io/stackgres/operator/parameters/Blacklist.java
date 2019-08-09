/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.parameters;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class Blacklist {

  private static final List<String> bl;

  static {
    bl = ImmutableList.of("listen_addresses",
        "port",
        "cluster_name",
        "hot_standby",
        "fsync",
        "full_page_writes",
        "log_destination",
        "logging_collector",
        "max_replication_slots",
        "max_wal_senders",
        "wal_keep_segments",
        "wal_level",
        "wal_log_hints",
        "archive_mode");
  }

  private Blacklist() {}

  public static List<String> getBlacklistParameters() {
    return bl;
  }

}

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
    bl = ImmutableList.<String>builder()
        .add("listen_addresses")
        .add("port")
        .add("cluster_name")
        .add("hot_standby")
        .add("fsync")
        .add("full_page_writes")
        .add("log_destination")
        .add("logging_collector")
        .add("max_replication_slots")
        .add("max_wal_senders")
        .add("wal_keep_segments")
        .add("wal_level")
        .add("wal_log_hints")
        .add("archive_mode")
        .build();
  }

  private Blacklist() {}

  public static List<String> getBlacklistParameters() {
    return bl;
  }

}

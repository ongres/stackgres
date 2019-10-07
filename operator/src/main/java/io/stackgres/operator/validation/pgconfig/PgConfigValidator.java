/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.validation.PgConfigReview;
import io.stackgres.operator.validation.Validator;

public interface PgConfigValidator extends Validator<PgConfigReview> {

  String[] BLACKLIST_PROPERTIES = {"max_connections",
      "max_locks_per_transaction", "max_worker_processes", "max_prepared_transactions",
      "wal_level", "wal_log_hints", "track_commit_timestamp", "max_wal_senders",
      "max_replication_slots", "wal_keep_segments", "listen_addresses", "port", "cluster_name",
      "hot_standby"};
}

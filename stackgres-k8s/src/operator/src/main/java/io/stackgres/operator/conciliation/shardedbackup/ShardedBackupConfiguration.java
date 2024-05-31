/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import java.util.List;

public record ShardedBackupConfiguration(
    Integer retention,
    String cronSchedule,
    String compression,
    List<String> paths,
    ShardedBackupPerformance performance,
    Boolean useVolumeSnapshot,
    String volumeSnapshotStorageClass,
    Boolean fastVolumeSnapshot,
    Integer timeout,
    Integer reconciliationTimeout,
    Integer maxRetries,
    Boolean retainWalsForUnmanagedLifecycle
) {
}

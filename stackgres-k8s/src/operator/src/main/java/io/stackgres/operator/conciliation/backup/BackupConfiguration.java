/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

public record BackupConfiguration(
    Integer retention,
    String cronSchedule,
    String compression,
    String path,
    BackupPerformance performance,
    Boolean useVolumeSnapshot,
    String volumeSnapshotStorageClass,
    Boolean fastVolumeSnapshot
) {
}

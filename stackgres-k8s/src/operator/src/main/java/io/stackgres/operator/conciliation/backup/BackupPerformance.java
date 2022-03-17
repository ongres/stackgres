/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

public record BackupPerformance(
    Long maxNetworkBandwitdh,
    Long maxDiskBandwitdh,
    Integer uploadDiskConcurrency
) {
}

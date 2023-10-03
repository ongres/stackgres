/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

public record ShardedBackupPerformance(
    Long maxNetworkBandwidth,
    Long maxDiskBandwidth,
    Integer uploadDiskConcurrency,
    Integer uploadConcurrency,
    Integer downloadConcurrency) {}

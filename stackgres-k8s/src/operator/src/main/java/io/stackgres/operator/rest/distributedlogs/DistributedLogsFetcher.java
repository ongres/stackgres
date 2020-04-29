/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.distributedlogs;

import java.util.List;

import io.stackgres.operator.rest.dto.cluster.ClusterLogEntryDto;

public interface DistributedLogsFetcher {

  List<ClusterLogEntryDto> logs(DistributedLogsQueryParameters parameters);

}

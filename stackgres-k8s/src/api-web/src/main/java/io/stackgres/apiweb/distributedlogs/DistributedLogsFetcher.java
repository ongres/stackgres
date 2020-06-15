/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs;

import java.util.List;

import io.stackgres.apiweb.dto.cluster.ClusterLogEntryDto;

public interface DistributedLogsFetcher {

  List<ClusterLogEntryDto> logs(DistributedLogsQueryParameters parameters);

}

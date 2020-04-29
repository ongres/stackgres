/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.distributedlogs;

import java.time.Instant;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterLogEntryDto;
import org.jooq.lambda.tuple.Tuple2;

public interface DistributedLogsFetcher {

  List<ClusterLogEntryDto> logs(
      ClusterDto cluster,
      int records,
      Tuple2<Instant, Integer> from,
      Tuple2<Instant, Integer> to,
      ImmutableMap<String, String> filters,
      boolean sortAsc,
      String text);

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import org.immutables.value.Value;
import org.jooq.lambda.tuple.Tuple2;

@Value.Immutable
public abstract class DistributedLogsQueryParameters {

  public abstract ClusterDto getCluster();

  public abstract int getRecords();

  public abstract Optional<Tuple2<Instant, Integer>> getFromTimeAndIndex();

  public abstract Optional<Tuple2<Instant, Integer>> getToTimeAndIndex();

  public abstract ImmutableMap<String, List<String>> getFilters();

  public abstract boolean isSortAsc();

  public abstract Optional<FullTextSearchQuery> getFullTextSearchQuery();

  public abstract boolean isFromInclusive();

}

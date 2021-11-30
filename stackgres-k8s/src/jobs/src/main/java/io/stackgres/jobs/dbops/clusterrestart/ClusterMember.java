/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface ClusterMember {

  String getClusterName();

  String getNamespace();

  String getName();

  Optional<MemberState> getState();

  Optional<MemberRole> getRole();

  Optional<String> getApiUrl();

  Optional<String> getHost();

  Optional<Integer> getPort();

  Optional<Integer> getTimeline();

  Optional<Integer> getLag();
}

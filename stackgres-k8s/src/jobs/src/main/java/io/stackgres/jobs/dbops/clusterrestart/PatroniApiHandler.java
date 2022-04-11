/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;

import io.smallrye.mutiny.Uni;

public interface PatroniApiHandler {

  Uni<List<ClusterMember>> getClusterMembers(String name, String namespace);

  Uni<List<PatroniInformation>> getClusterMembersPatroniInformation(String name, String namespace);

  Uni<PatroniInformation> getClusterMemberPatroniInformation(ClusterMember member);

  Uni<Void> performSwitchover(ClusterMember leader, ClusterMember candidate);

  Uni<Void> restartPostgres(ClusterMember member);

}

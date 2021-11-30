/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;

import io.smallrye.mutiny.Uni;

public interface PatroniApiHandler {

  Uni<List<ClusterMember>> getClusterMembers(String name, String namespace);

  Uni<List<PatroniInformation>> getMembersPatroniInformation(String name, String namespace);

  Uni<Void> performSwitchover(ClusterMember leader, ClusterMember candidate);

  Uni<Void> restartPostgres(ClusterMember member);

}

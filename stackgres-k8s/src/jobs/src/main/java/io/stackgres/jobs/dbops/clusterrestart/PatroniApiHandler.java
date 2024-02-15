/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PatroniApiHandler {

  @Inject
  PatroniCtlFinder ctlFinder;

  @Inject
  DbOpsExecutorService executorService;

  public Uni<List<PatroniMember>> getClusterMembers(String name, String namespace) {
    return executorService.itemAsync(() -> ctlFinder.findPatroniCtl(name, namespace).list());
  }

  public Uni<List<PatroniInformation>> getClusterMembersPatroniInformation(String name, String namespace) {
    final Uni<List<PatroniMember>> clusterMembers = getClusterMembers(name, namespace);
    return clusterMembers.chain(this::getPatroniInformationForClusterMembers);
  }

  public Uni<Integer> getLatestPrimaryIndexFromPatroni(String name, String namespace) {
    return executorService.itemAsync(() -> PatroniUtil
        .getLatestPrimaryIndexFromPatroni(ctlFinder.findPatroniCtl(name, namespace)));
  }

  public Uni<Integer> getClusterPostgresVersion(String name, String namespace) {
    return executorService.itemAsync(() -> ctlFinder.findPatroniCtl(name, namespace)
        .queryPrimary("SHOW server_version_num")
        .get(0).get("server_version_num").intValue());
  }

  private Uni<List<PatroniInformation>> getPatroniInformationForClusterMembers(
      List<PatroniMember> members) {
    return Multi.createFrom().iterable(members)
        .onItem()
        .transform(this::getClusterMemberPatroniInformation)
        .collect()
        .asList();
  }

  public PatroniInformation getClusterMemberPatroniInformation(PatroniMember member) {
    return ImmutablePatroniInformation.builder()
        .role(member.getMemberRole())
        .state(member.getMemberState())
        .isPendingRestart(member.getPendingRestart() != null)
        .build();
  }

  public Uni<Void> performSwitchover(
      String name,
      String namespace,
      PatroniMember leader,
      PatroniMember candidate) {
    return executorService.invokeAsync(() -> ctlFinder
        .findPatroniCtl(name, namespace)
        .switchover(leader.getMember(), candidate.getMember()));
  }

  public Uni<Void> restartPostgres(
      String name,
      String namespace,
      PatroniMember member) {
    return executorService.invokeAsync(() -> ctlFinder
        .findPatroniCtl(name, namespace)
        .restart(member.getMember()));
  }

}

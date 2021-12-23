/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PostgresRestartImpl implements PostgresRestart {

  private final PatroniApiHandler patroniApi;

  @Inject
  public PostgresRestartImpl(PatroniApiHandler patroniApi) {
    this.patroniApi = patroniApi;
  }

  @Override
  public Uni<Void> restartPostgres(String memberName, String clusterName, String namespace) {
    return patroniApi.getClusterMembers(clusterName, namespace)
        .onItem()
        .transform(members -> members.stream()
            .filter(member -> member.getName().equals(memberName))
            .findFirst().orElseThrow())
        .chain((member) -> patroniApi.restartPostgres(member)
            .onFailure()
            .retry()
            .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
            .atMost(10)
        );
  }

}

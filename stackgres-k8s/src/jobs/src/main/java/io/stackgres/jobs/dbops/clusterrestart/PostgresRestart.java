/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.Optional;

import com.google.common.base.Predicates;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.RetryUtil;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.patroni.PatroniMember.MemberState;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.MutinyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PostgresRestart {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgresRestart.class);

  @Inject
  PatroniApiHandler patroniApi;

  @Inject
  DbOpsExecutorService executorService;

  public Uni<Object> restartPostgres(String memberName, String clusterName, String namespace) {
    return restartPostgresWithoutRetry(memberName, clusterName, namespace)
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry(
            "performing the restart of postgres"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .atMost(10);
  }

  Uni<Object> restartPostgresWithoutRetry(String memberName, String clusterName, String namespace) {
    return patroniApi.getClusterMembers(clusterName, namespace)
        .onItem()
        .transform(members -> members.stream()
            .filter(member -> member.getMember().equals(memberName))
            .findFirst().orElseThrow())
        .chain(member -> restartOrWaitUntilNoPendingRestart(member, clusterName, namespace));
  }

  private Uni<Object> restartOrWaitUntilNoPendingRestart(
      PatroniMember member, String clusterName, String namespace) {
    return restartOrWaitUntilNoPendingRestart(member, 0, Optional.empty(), clusterName, namespace);
  }

  private Uni<Object> restartOrWaitUntilNoPendingRestart(
      PatroniMember member, int retry, Optional<Throwable> restartThrowable,
      String clusterName, String namespace) {
    return Uni.createFrom().item(patroniApi.getClusterMemberPatroniInformation(member))
        .chain(patroniInformation -> {
          if (patroniInformation.getState()
              .filter(Predicates.or(
                  MemberState.STARTING::equals,
                  MemberState.RESTARTING::equals))
              .isPresent()) {
            LOGGER.info("Postgres of Pod {} is already restarting,"
                + " wait {}.{} seconds for the restart to complete...",
                member.getMember(),
                calculateExponentialBackoffDelay(retry).toSeconds(),
                calculateExponentialBackoffDelay(retry).toMillisPart());
            return Uni.createFrom().voidItem()
                .onItem()
                .delayIt()
                .by(calculateExponentialBackoffDelay(retry))
                .chain(() -> restartOrWaitUntilNoPendingRestart(
                    member, retry + 1, restartThrowable, clusterName, namespace));
          }
          if (patroniInformation.isPendingRestart()) {
            if (restartThrowable.isPresent()) {
              return Uni.createFrom()
                  .failure(restartThrowable.orElseThrow());
            }
            return patroniApi.restartPostgres(clusterName, namespace, member)
                .onFailure()
                .call(throwable -> {
                  LOGGER.info("Postgres of Pod {} failed restarting,"
                      + " wait {}.{} seconds for a possible already"
                      + " existing restart operation to complete...",
                      member.getMember(),
                      calculateExponentialBackoffDelay(retry).toSeconds(),
                      calculateExponentialBackoffDelay(retry).toMillisPart());
                  return Uni.createFrom().voidItem()
                      .onItem()
                      .delayIt()
                      .by(calculateExponentialBackoffDelay(retry))
                      .chain(() -> restartOrWaitUntilNoPendingRestart(
                          member, retry + 1, Optional.of(throwable), clusterName, namespace));
                });
          } else {
            return Uni.createFrom().voidItem();
          }
        });
  }

  private Duration calculateExponentialBackoffDelay(int retry) {
    return Duration.ofMillis(RetryUtil.calculateExponentialBackoffDelay(10, 600, 10, retry));
  }

}

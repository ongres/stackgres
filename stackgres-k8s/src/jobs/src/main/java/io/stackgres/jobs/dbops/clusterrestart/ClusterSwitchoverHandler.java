/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.stackgres.common.PatroniUtil;
import io.stackgres.jobs.dbops.MutinyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterSwitchoverHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSwitchoverHandler.class);

  private final PatroniApiHandler patroniApi;

  @Inject
  public ClusterSwitchoverHandler(PatroniApiHandler patroniApi) {
    this.patroniApi = patroniApi;
  }

  public Uni<Void> performSwitchover(String leader, String clusterName, String clusterNamespace) {
    return patroniApi.getClusterMembers(clusterName, clusterNamespace)
        .chain(members -> doSwitchover(members, leader))
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("performing the switchover"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .indefinitely();
  }

  private Uni<Void> doSwitchover(List<ClusterMember> members, String givenLeader) {
    Optional<ClusterMember> candidate = members.stream()
        .filter(member -> member.getRole().map(MemberRole.REPLICA::equals).orElse(false))
        .filter(member -> member.getState().map(MemberState.RUNNING::equals).orElse(false))
        .filter(member -> member.getTags()
            .filter(tags -> tags.entrySet().stream().anyMatch(
                tag -> tag.getKey().equals(PatroniUtil.NOFAILOVER_TAG)
                && tag.getValue().equals(PatroniUtil.TRUE_TAG_VALUE)))
            .isEmpty())
        .min((m1, m2) -> {
          if (m1.getLag().isPresent() && m2.getLag().isPresent()) {
            return m1.getLag().get().compareTo(m2.getLag().get());
          } else if (m1.getLag().isPresent() && m2.getLag().isEmpty()) {
            return -1;
          } else if (m1.getLag().isEmpty() && m2.getLag().isPresent()) {
            return 1;
          } else {
            return 0;
          }
        });

    if (candidate.isEmpty()) {
      LOGGER.info("No candidate primary found. Skipping switchover");
      return Uni.createFrom().voidItem();
    } else if (candidate.get().getRole().map(role -> role == MemberRole.LEADER).orElse(false)) {
      LOGGER.info("Candidate is already primary. Skipping switchover");
      return Uni.createFrom().voidItem();
    } else {
      Optional<ClusterMember> leader = members.stream()
          .filter(member -> member.getRole().map(MemberRole.LEADER::equals).orElse(false))
          .findFirst();

      if (leader.isPresent()) {
        ClusterMember actualLeader = leader.get();
        if (Objects.equals(actualLeader.getName(), givenLeader)) {
          return patroniApi.performSwitchover(leader.get(), candidate.get());
        } else {
          LOGGER.info("Leader of the cluster is not {} anymore. Skipping switchover", givenLeader);
          return Uni.createFrom().voidItem();
        }
      } else {
        return Uni.createFrom().failure(() -> new FailoverException(
            "Leader was not found just before performing the switchover"));
      }
    }
  }

}

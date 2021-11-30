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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterSwitchoverHandlerImpl implements ClusterSwitchoverHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSwitchoverHandlerImpl.class);

  private final PatroniApiHandler patroniApi;

  @Inject
  public ClusterSwitchoverHandlerImpl(PatroniApiHandler patroniApi) {
    this.patroniApi = patroniApi;
  }

  @Override
  public Uni<Void> performSwitchover(String leader, String clusterName, String clusterNamespace) {

    return patroniApi.getClusterMembers(clusterName, clusterNamespace)
        .chain(members -> doSwitchOver(members, leader));

  }

  private Uni<Void> doSwitchOver(List<ClusterMember> members, String givenLeader) {
    if (members.size() == 1) {
      return Uni.createFrom().voidItem();
    } else {
      Optional<ClusterMember> leader = members.stream()
          .filter(member -> member.getRole().map(MemberRole.LEADER::equals).orElse(false))
          .findFirst();

      Optional<ClusterMember> candidate = members.stream()
          .filter(member -> member.getRole().map(MemberRole.REPlICA::equals).orElse(false))
          .filter(member -> member.getState().map(MemberState.RUNNING::equals).orElse(false))
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

      if (leader.isPresent() && candidate.isPresent()) {
        ClusterMember actualLeader = leader.get();
        if (Objects.equals(actualLeader.getName(), givenLeader)) {
          return Uni.createFrom().emitter(em -> patroniApi
              .performSwitchover(leader.get(), candidate.get())
              .onFailure()
              .retry()
              .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
              .indefinitely()
              .subscribe().with(item -> em.complete(null), em::fail));
        } else {
          LOGGER.info("Leader of the cluster is not {} anymore. Skipping switchover", givenLeader);
          return Uni.createFrom().voidItem();
        }

      } else {
        return Uni.createFrom().failure(() -> new FailoverException("Cluster not healthy"));
      }
    }
  }

}

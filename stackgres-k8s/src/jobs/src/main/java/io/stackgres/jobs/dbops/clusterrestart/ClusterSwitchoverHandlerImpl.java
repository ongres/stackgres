/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ClusterSwitchoverHandlerImpl implements ClusterSwitchoverHandler {

  private final PatroniApiHandler patroniApi;

  @Inject
  public ClusterSwitchoverHandlerImpl(PatroniApiHandler patroniApi) {
    this.patroniApi = patroniApi;
  }

  @Override
  public Uni<Void> performSwitchover(String clusterName, String clusterNamespace) {

    return patroniApi.getClusterMembers(clusterName, clusterNamespace)
        .chain(this::doSwitchOver);

  }


  private Uni<Void> doSwitchOver(List<ClusterMember> members) {
    if (members.size() == 1) {
      return Uni.createFrom().voidItem();
    } else {
      Optional<ClusterMember> leader = members.stream()
          .filter(member -> member.getRole() == MemberRole.LEADER)
          .findFirst();

      Optional<ClusterMember> candidate = members.stream()
          .filter(member -> member.getRole() == MemberRole.REPlICA)
          .filter(member -> member.getState() == MemberState.RUNNING)
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
        return Uni.createFrom().emitter(em -> patroniApi
            .performSwitchover(leader.get(), candidate.get())
            .onFailure()
            .retry()
            .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
            .atMost(50)
            .subscribe().with(item -> em.complete(null), em::fail));

      } else {
        return Uni.createFrom().failure(() -> new FailoverException("Cluster not healthy"));
      }
    }
  }

}

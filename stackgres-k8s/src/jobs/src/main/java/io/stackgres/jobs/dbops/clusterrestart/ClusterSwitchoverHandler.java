/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.patroni.PatroniMember.MemberRole;
import io.stackgres.jobs.dbops.DbOpsExecutorService;
import io.stackgres.jobs.dbops.MutinyUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterSwitchoverHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSwitchoverHandler.class);

  @Inject
  PatroniApiHandler patroniApi;

  @Inject
  DbOpsExecutorService executorService;

  public Uni<Void> performSwitchover(String leader, String clusterName, String clusterNamespace) {
    return patroniApi.getClusterMembers(clusterName, clusterNamespace)
        .chain(members -> doSwitchover(members, leader, clusterName, clusterNamespace))
        .onFailure()
        .transform(MutinyUtil.logOnFailureToRetry("performing the switchover"))
        .onFailure()
        .retry()
        .withBackOff(Duration.ofMillis(10), Duration.ofSeconds(5))
        .indefinitely();
  }

  private Uni<Void> doSwitchover(List<PatroniMember> members, String givenLeader,
      String clusterName, String clusterNamespace) {
    Pattern nameWithIndexPattern = ResourceUtil.getNameWithIndexPattern(clusterName);
    Optional<PatroniMember> candidate = members.stream()
        .filter(member -> nameWithIndexPattern.matcher(member.getMember()).find())
        .filter(PatroniMember::isReplica)
        .filter(PatroniMember::isRunning)
        .filter(member -> Optional.ofNullable(member.getTags())
            .filter(tags -> tags.entrySet().stream().anyMatch(
                tag -> tag.getKey().equals(PatroniUtil.NOFAILOVER_TAG)
                && tag.getValue() != null && tag.getValue().getValue() != null
                && Objects.equals(tag.getValue().getValue().toString(), Boolean.TRUE.toString())))
            .isEmpty())
        .min((m1, m2) -> {
          var l1 = Optional.ofNullable(m1.getLagInMb())
              .map(IntOrString::getIntVal);
          var l2 = Optional.ofNullable(m2.getLagInMb())
              .map(IntOrString::getIntVal);
          if (l1.isPresent() && l2.isPresent()) {
            return l1.get().compareTo(l2.get());
          } else if (l1.isPresent() && l2.isEmpty()) {
            return -1;
          } else if (l1.isEmpty() && l2.isPresent()) {
            return 1;
          } else {
            return 0;
          }
        });

    if (candidate.isEmpty()) {
      LOGGER.info("No candidate primary found. Skipping switchover");
      return Uni.createFrom().voidItem();
    } else if (MemberRole.LEADER.equals(candidate.get().getMemberRole())) {
      LOGGER.info("Candidate is already primary. Skipping switchover");
      return Uni.createFrom().voidItem();
    } else {
      Optional<PatroniMember> leader = members.stream()
          .filter(member -> MemberRole.LEADER.equals(member.getMemberRole()))
          .findFirst();

      if (leader.isPresent()) {
        PatroniMember actualLeader = leader.get();
        if (Objects.equals(actualLeader.getMember(), givenLeader)) {
          return patroniApi.performSwitchover(clusterName, clusterNamespace, leader.get(), candidate.get());
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

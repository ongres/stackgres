/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class DbOpsJobsGenerator implements ResourceGenerator<StackGresDbOpsContext> {

  private final DbOpsJobsDiscoverer jobsDiscoverer;

  @Inject
  public DbOpsJobsGenerator(DbOpsJobsDiscoverer jobsDiscoverer) {
    this.jobsDiscoverer = jobsDiscoverer;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDbOpsContext config) {
    Instant now = Instant.now();
    Map<String, DbOpsJobFactory> factories = jobsDiscoverer.discoverFactories(config);
    return Seq.of(config.getSource())
        .filter(dbOp -> !DbOpsUtil.isAlreadyCompleted(dbOp))
        .filter(dbOp -> !DbOpsUtil.isToRunAfter(dbOp, now))
        .map(dbOp -> {
          if (DbOpsUtil.ROLLOUT_OPS.contains(dbOp.getSpec().getOp())) {
            return buildClusterForDbOps(
                config,
                new StackGresClusterBuilder()
                .withNewMetadata()
                .withAnnotations(
                    Seq.seq(
                      Optional.ofNullable(config.getCluster().getMetadata().getAnnotations())
                      .map(Map::entrySet)
                      .stream()
                      .flatMap(Set::stream))
                    .append(Map.entry(StackGresContext.ROLLOUT_DBOPS_KEY, dbOp.getMetadata().getName()))
                    .toMap(Map.Entry::getKey, Map.Entry::getValue))
                .endMetadata()
                .withNewStatus()
                .withNewDbOps()
                .endDbOps()
                .endStatus())
                .build();
          }
          DbOpsJobFactory jobFactory = factories.get(dbOp.getSpec().getOp());
          if (jobFactory == null) {
            throw new UnsupportedOperationException("DbOps "
                + dbOp.getSpec().getOp() + " not implemented!");
          }
          return jobFactory.createJob(config);
        });
  }

  private StackGresClusterBuilder buildClusterForDbOps(StackGresDbOpsContext config, StackGresClusterBuilder builder) {
    final List<String> initialInstances = config.getClusterPods()
        .stream()
        .map(pod -> pod.getMetadata().getName())
        .toList();
    final String primaryInstance = config.getClusterPods()
        .stream()
        .map(pod -> pod.getMetadata().getName())
        .filter(name -> config.getClusterPatroniMembers().stream()
            .anyMatch(patroniMember -> patroniMember.getMember().equals(name)
                && patroniMember.isPrimary()))
        .findAny()
        .orElse(null);

    if ("restart".equals(config.getSource().getSpec().getOp())
        && Optional.ofNullable(config.getCluster().getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getRestart)
        .isEmpty()) {
      builder = builder
          .editStatus()
          .editDbOps()
          .withNewRestart()
          .withInitialInstances(initialInstances)
          .withPrimaryInstance(primaryInstance)
          .endRestart()
          .endDbOps()
          .endStatus();
    } else if ("securityUpgrade".equals(config.getSource().getSpec().getOp())
        && Optional.ofNullable(config.getCluster().getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getSecurityUpgrade)
        .isEmpty()) {
      builder = builder
          .editStatus()
          .editDbOps()
          .withNewSecurityUpgrade()
          .withInitialInstances(initialInstances)
          .withPrimaryInstance(primaryInstance)
          .endSecurityUpgrade()
          .endDbOps()
          .endStatus();
    } else if ("minorVersionUpgrade".equals(config.getSource().getSpec().getOp())
        && Optional.ofNullable(config.getCluster().getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade)
        .isEmpty()) {
      builder = builder
          .editSpec()
          .editPostgres()
          .withVersion(config.getSource().getSpec().getMinorVersionUpgrade().getPostgresVersion())
          .endPostgres()
          .endSpec()
          .editStatus()
          .editDbOps()
          .withNewMinorVersionUpgrade()
          .withInitialInstances(initialInstances)
          .withPrimaryInstance(primaryInstance)
          .endMinorVersionUpgrade()
          .endDbOps()
          .endStatus();
    }
    return builder;
  }

}

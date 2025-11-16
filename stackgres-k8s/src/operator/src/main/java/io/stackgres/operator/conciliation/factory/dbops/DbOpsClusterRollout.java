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
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMinorVersionUpgradeStatusBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsRestartStatusBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsSecurityUpgradeStatusBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgrade;
import io.stackgres.operator.common.DbOpsUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class DbOpsClusterRollout implements ResourceGenerator<StackGresDbOpsContext> {

  public static final Set<String> ROLLOUT_DBOPS_KEYS = Set.of(
      StackGresContext.ROLLOUT_DBOPS_KEY,
      StackGresContext.ROLLOUT_DBOPS_METHOD_KEY);

  @Override
  public Stream<HasMetadata> generateResource(StackGresDbOpsContext config) {
    Instant now = Instant.now();
    return Seq.of(config.getSource())
        .filter(dbOp -> DbOpsUtil.ROLLOUT_OPS.contains(dbOp.getSpec().getOp()))
        .filter(dbOp -> !DbOpsUtil.isToRunAfter(dbOp, now))
        .filter(dbOp -> !DbOpsUtil.isAlreadyCompleted(dbOp))
        .map(dbOp -> {
          final boolean isAlreadyRolloutOrTimeoutExpired =
              DbOpsUtil.isAlreadyRollout(dbOp)
              || DbOpsUtil.isTimeoutExpired(dbOp, now);
          final Map<String, Optional<String>> annotations = Map.of(
              StackGresContext.UPDATE_UNOWNED_RESOURCE_KEY,
              Optional.of("true"),
              StackGresContext.ROLLOUT_DBOPS_KEY,
              Optional.of(dbOp.getMetadata().getName())
              .filter(name -> !isAlreadyRolloutOrTimeoutExpired),
              StackGresContext.ROLLOUT_DBOPS_METHOD_KEY,
              Optional.ofNullable(dbOp.getSpec().getRestart())
              .map(StackGresDbOpsRestart::getMethod)
              .or(() -> Optional.ofNullable(dbOp.getSpec().getSecurityUpgrade())
                  .map(StackGresDbOpsSecurityUpgrade::getMethod))
              .or(() -> Optional.ofNullable(dbOp.getSpec().getMinorVersionUpgrade())
                  .map(StackGresDbOpsMinorVersionUpgrade::getMethod))
              .map(DbOpsMethodType::fromString)
              .map(DbOpsMethodType::annotationValue)
              .filter(name -> !isAlreadyRolloutOrTimeoutExpired));
          StackGresClusterBuilder builder = new StackGresClusterBuilder()
              .withNewMetadata()
              .withNamespace(dbOp.getMetadata().getNamespace())
              .withName(dbOp.getSpec().getSgCluster())
              .withAnnotations(
                  Seq.seq(
                    Optional.ofNullable(config.getCluster().getMetadata().getAnnotations())
                    .map(Map::entrySet)
                    .stream()
                    .flatMap(Set::stream)
                    .filter(annotation -> !annotations.containsKey(annotation.getKey())))
                  .append(annotations.entrySet().stream()
                      .filter(annotation -> annotation.getValue().isPresent())
                      .map(annotation -> Map.entry(annotation.getKey(), annotation.getValue().get())))
                  .toMap(Map.Entry::getKey, Map.Entry::getValue))
              .endMetadata()
              .withNewStatus()
              .withNewDbOps()
              .endDbOps()
              .endStatus();
          if (!isAlreadyRolloutOrTimeoutExpired) {
            builder = appendDbOpsChangesToCluster(
                config,
                builder);
          }
          return builder.build();
        });
  }

  private StackGresClusterBuilder appendDbOpsChangesToCluster(
      StackGresDbOpsContext config,
      StackGresClusterBuilder builder) {
    final Supplier<List<String>> initialInstancesSupplier = () -> config.getClusterPods()
        .stream()
        .map(pod -> pod.getMetadata().getName())
        .toList();
    final Supplier<String> primaryInstanceSupplier = () -> config.getClusterPods()
        .stream()
        .map(pod -> pod.getMetadata().getName())
        .filter(name -> config.getClusterPatroniMembers().stream()
            .anyMatch(patroniMember -> patroniMember.getMember().equals(name)
                && patroniMember.isPrimary()))
        .findAny()
        .orElse(null);

    if ("restart".equals(config.getSource().getSpec().getOp())) {
      builder = builder
          .editStatus()
          .editDbOps()
          .withName(config.getSource().getMetadata().getName())
          .withRestart(Optional.ofNullable(config.getCluster().getStatus())
              .map(StackGresClusterStatus::getDbOps)
              .map(StackGresClusterDbOpsStatus::getRestart)
              .orElseGet(() -> new StackGresClusterDbOpsRestartStatusBuilder()
                  .withInitialInstances(initialInstancesSupplier.get())
                  .withPrimaryInstance(primaryInstanceSupplier.get())
                  .build()))
          .endDbOps()
          .endStatus();
    } else if ("securityUpgrade".equals(config.getSource().getSpec().getOp())) {
      builder = builder
          .editStatus()
          .editDbOps()
          .withName(config.getSource().getMetadata().getName())
          .withSecurityUpgrade(Optional.ofNullable(config.getCluster().getStatus())
              .map(StackGresClusterStatus::getDbOps)
              .map(StackGresClusterDbOpsStatus::getSecurityUpgrade)
              .orElseGet(() -> new StackGresClusterDbOpsSecurityUpgradeStatusBuilder()
                  .withInitialInstances(initialInstancesSupplier.get())
                  .withPrimaryInstance(primaryInstanceSupplier.get())
                  .build()))
          .endDbOps()
          .endStatus();
    } else if ("minorVersionUpgrade".equals(config.getSource().getSpec().getOp())) {
      builder = builder
          .editSpec()
          .editPostgres()
          .withVersion(config.getSource().getSpec().getMinorVersionUpgrade().getPostgresVersion())
          .endPostgres()
          .endSpec()
          .editStatus()
          .editDbOps()
          .withName(config.getSource().getMetadata().getName())
          .withMinorVersionUpgrade(Optional.ofNullable(config.getCluster().getStatus())
              .map(StackGresClusterStatus::getDbOps)
              .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade)
              .orElseGet(() -> new StackGresClusterDbOpsMinorVersionUpgradeStatusBuilder()
                  .withInitialInstances(initialInstancesSupplier.get())
                  .withPrimaryInstance(primaryInstanceSupplier.get())
                  .build()))
          .endDbOps()
          .endStatus();
    }
    return builder;
  }

}

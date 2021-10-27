/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitr;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PatroniEnvironmentVariablesFactory;

@Singleton
public class ClusterPatroniEnvVarFactory
    extends PatroniEnvironmentVariablesFactory<StackGresClusterContext> {

  @Override
  public List<EnvVar> createResource(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();

    List<EnvVar> additionalEnvVars = new ArrayList<>();
    Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getInitData)
        .map(StackGresClusterInitData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getPointInTimeRecovery)
        .map(StackGresClusterRestorePitr::getRestoreToTimestamp)
        .map(Instant::parse)
        .map(restoreToTimestamp -> new EnvVarBuilder().withName("RECOVERY_TARGET_TIME")
            .withValue(DateTimeFormatter.ISO_LOCAL_DATE
                .withZone(ZoneId.from(ZoneOffset.UTC))
                .format(restoreToTimestamp)
                + " " + DateTimeFormatter.ISO_LOCAL_TIME
                .withZone(ZoneId.from(ZoneOffset.UTC))
                .format(restoreToTimestamp))
            .build())
        .ifPresent(additionalEnvVars::add);

    List<EnvVar> patroniEnvVars = createPatroniEnvVars(cluster);

    return ImmutableList.<EnvVar>builder()
        .add(new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_LISTEN")
            .withValue("0.0.0.0:" + EnvoyUtil.PATRONI_PORT)
            .build())
        .addAll(patroniEnvVars)
        .addAll(additionalEnvVars)
        .build();

  }
}

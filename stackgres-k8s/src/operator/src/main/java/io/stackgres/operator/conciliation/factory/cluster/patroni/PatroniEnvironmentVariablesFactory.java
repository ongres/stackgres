/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitr;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractPatroniEnvironmentVariablesFactory;
import jakarta.inject.Singleton;

@Singleton
public class PatroniEnvironmentVariablesFactory
    extends AbstractPatroniEnvironmentVariablesFactory<StackGresClusterContext> {

  @Override
  public List<EnvVar> createResource(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();

    List<EnvVar> additionalEnvVars = new ArrayList<>();

    var replicateFrom = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getReplicateFrom);
    appendEnvVarIfPresent("REPLICATE_FROM_BACKUP",
        replicateFrom, additionalEnvVars,
        Function.<StackGresClusterReplicateFrom>identity(),
        Function.<StackGresClusterReplicateFrom>identity()
        .andThen(value -> Boolean.TRUE.toString()));

    var fromBackup = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getInitialData)
        .map(StackGresClusterInitialData::getRestore)
        .map(StackGresClusterRestore::getFromBackup);
    appendEnvVarIfPresent("RECOVERY_FROM_BACKUP",
        fromBackup, additionalEnvVars,
        Function.<StackGresClusterRestoreFromBackup>identity(),
        Function.<StackGresClusterRestoreFromBackup>identity()
        .andThen(value -> Boolean.TRUE.toString()));
    appendEnvVarIfPresent("RECOVERY_TARGET",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTarget,
        Function.identity());
    appendEnvVarIfPresent("RECOVERY_TARGET_TIMELINE",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetTimeline,
        Function.identity());
    appendEnvVarIfPresent("RECOVERY_TARGET_INCLUSIVE",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetInclusive,
        Function.<Boolean>identity()
        .andThen(value -> value ? "on" : "off"));
    appendEnvVarIfPresent("RECOVERY_TARGET_NAME",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetName,
        Function.identity());
    appendEnvVarIfPresent("RECOVERY_TARGET_XID",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetXid,
        Function.identity());
    appendEnvVarIfPresent("RECOVERY_TARGET_LSN",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetLsn,
        Function.identity());
    appendEnvVarIfPresent("RECOVERY_TARGET_TIME",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getPointInTimeRecovery,
        Function.<StackGresClusterRestorePitr>identity()
        .andThen(StackGresClusterRestorePitr::getRestoreToTimestamp)
        .andThen(Instant::parse)
        .andThen(restoreToTimestamp -> DateTimeFormatter.ISO_LOCAL_DATE
            .withZone(ZoneId.from(ZoneOffset.UTC))
            .format(restoreToTimestamp)
            + " " + DateTimeFormatter.ISO_LOCAL_TIME
            .withZone(ZoneId.from(ZoneOffset.UTC))
            .format(restoreToTimestamp)));
    appendEnvVarIfPresent("INITDB_AUTH_HOST",
        Optional.of(context.getPostgresConfig()),
        additionalEnvVars,
        Function.<StackGresPostgresConfig>identity()
        .andThen(StackGresPostgresConfig::getSpec)
        .andThen(StackGresPostgresConfigSpec::getPostgresqlConf)
        .andThen(conf -> conf.get("password_encryption")),
        Function.<String>identity());
    if (additionalEnvVars.stream().map(EnvVar::getName).noneMatch("INITDB_AUTH_HOST"::equals)
        && getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH) {
      additionalEnvVars.add(new EnvVarBuilder()
          .withName("INITDB_AUTH_HOST")
          .withValue("md5")
          .build());
    }

    List<EnvVar> patroniEnvVars = createPatroniEnvVars(cluster)
        .stream()
        .filter(envVar -> !envVar.getName()
            .equals(StackGresPasswordKeys.SUPERUSER_PASSWORD_ENV))
        .filter(envVar -> !envVar.getName()
            .equals(StackGresPasswordKeys.REPLICATION_PASSWORD_ENV))
        .filter(envVar -> !envVar.getName()
            .equals(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV))
        .filter(envVar -> !envVar.getName()
            .equals(StackGresPasswordKeys.RESTAPI_USERNAME_ENV))
        .filter(envVar -> !envVar.getName()
            .equals(StackGresPasswordKeys.RESTAPI_PASSWORD_ENV))
        .toList();

    return ImmutableList.<EnvVar>builder()
        .addAll(patroniEnvVars)
        .addAll(additionalEnvVars)
        .build();

  }

  private <S, T> void appendEnvVarIfPresent(String name,
      Optional<S> source, List<EnvVar> additionalEnvVars,
      Function<S, T> extractvalue,
      Function<T, String> convertValue) {
    source
        .map(extractvalue)
        .map(convertValue)
        .map(value -> new EnvVarBuilder()
            .withName(name)
            .withValue(value)
            .build())
        .ifPresent(additionalEnvVars::add);
  }
}

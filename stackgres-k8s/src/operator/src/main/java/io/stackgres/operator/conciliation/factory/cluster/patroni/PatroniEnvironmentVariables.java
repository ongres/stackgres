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
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitr;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresReplicationInitializationMode;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.EnvVarProvider;
import io.stackgres.operator.initialization.DefaultClusterPostgresConfigFactory;
import jakarta.inject.Singleton;

@Singleton
public class PatroniEnvironmentVariables implements EnvVarProvider<StackGresClusterContext> {

  private final DefaultClusterPostgresConfigFactory defaultPostgresConfigFactory;

  public PatroniEnvironmentVariables(DefaultClusterPostgresConfigFactory defaultPostgresConfigFactory) {
    this.defaultPostgresConfigFactory = defaultPostgresConfigFactory;
  }

  @Override
  public List<EnvVar> getEnvVars(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();

    List<EnvVar> additionalEnvVars = new ArrayList<>();

    additionalEnvVars.add(ClusterPath.PATRONI_START_FILE_PATH.envVar());
    additionalEnvVars.add(
        new EnvVarBuilder()
        .withName(PatroniUtil.PATRONI_READ_ONLY_SERVICE_NAME)
        .withValue(PatroniUtil.readOnlyName(cluster))
        .build());
    additionalEnvVars.add(
        new EnvVarBuilder()
        .withName(PatroniUtil.REPLICATION_SERVICE_PORT_ENV)
        .withValue(String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT))
        .build());
    var replication = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getReplication);
    appendEnvVarIfPresent(
        "REPLICATION_INITIALIZATION_FROM_BACKUP",
        replication, additionalEnvVars,
        Function.<StackGresClusterReplication>identity()
        .andThen(StackGresClusterReplication::getInitializationModeOrDefault)
        .andThen(mode -> StackGresReplicationInitializationMode.FROM_EXISTING_BACKUP.ordinal() >= mode.ordinal()),
        Object::toString);
    appendEnvVarIfPresent(
        "REPLICATION_INITIALIZATION_FROM_REPLICA",
        replication, additionalEnvVars,
        Function.<StackGresClusterReplication>identity()
        .andThen(StackGresClusterReplication::getInitializationModeOrDefault)
        .andThen(mode -> StackGresReplicationInitializationMode.FROM_REPLICA.ordinal() >= mode.ordinal()),
        Object::toString);

    var replicateFrom = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getReplicateFrom);
    appendEnvVarIfPresent(
        "REPLICATE_FROM_BACKUP",
        replicateFrom, additionalEnvVars,
        Function.<StackGresClusterReplicateFrom>identity(),
        Function.<StackGresClusterReplicateFrom>identity()
        .andThen(value -> Boolean.TRUE.toString()));

    var fromBackup = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getInitialData)
        .map(StackGresClusterInitialData::getRestore)
        .map(StackGresClusterRestore::getFromBackup);
    appendEnvVarIfPresent(
        "RECOVERY_FROM_BACKUP",
        fromBackup, additionalEnvVars,
        Function.<StackGresClusterRestoreFromBackup>identity(),
        Function.<StackGresClusterRestoreFromBackup>identity()
        .andThen(value -> Boolean.TRUE.toString()));
    appendEnvVarIfPresent(
        "RECOVERY_TARGET",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTarget,
        Function.identity());
    appendEnvVarIfPresent(
        "RECOVERY_TARGET_TIMELINE",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetTimeline,
        Function.identity());
    appendEnvVarIfPresent(
        "RECOVERY_TARGET_INCLUSIVE",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetInclusive,
        Function.<Boolean>identity()
        .andThen(value -> value ? "on" : "off"));
    appendEnvVarIfPresent(
        "RECOVERY_TARGET_NAME",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetName,
        Function.identity());
    appendEnvVarIfPresent(
        "RECOVERY_TARGET_XID",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetXid,
        Function.identity());
    appendEnvVarIfPresent(
        "RECOVERY_TARGET_LSN",
        fromBackup, additionalEnvVars,
        StackGresClusterRestoreFromBackup::getTargetLsn,
        Function.identity());
    appendEnvVarIfPresent(
        "RECOVERY_TARGET_TIME",
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
        context.getPostgresConfig()
            .or(() -> Optional.of(defaultPostgresConfigFactory.buildResource(cluster))),
        additionalEnvVars,
        Function.<StackGresPostgresConfig>identity()
        .andThen(StackGresPostgresConfig::getSpec)
        .andThen(StackGresPostgresConfigSpec::getPostgresqlConf)
        .andThen(conf -> conf.get("password_encryption")),
        Function.<String>identity());
    if (additionalEnvVars.stream().map(EnvVar::getName).noneMatch("INITDB_AUTH_HOST"::equals)
        && getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH) {
      additionalEnvVars.add(
          new EnvVarBuilder()
          .withName("INITDB_AUTH_HOST")
          .withValue("md5")
          .build());
    }

    List<EnvVar> patroniEnvVars =
        List.of(
            new EnvVarBuilder()
            .withName("PATRONI_NAME")
            .withNewValueFrom()
            .withNewFieldRef()
            .withFieldPath("metadata.name")
            .endFieldRef()
            .endValueFrom()
            .build(),
            new EnvVarBuilder()
            .withName("POD_IP")
            .withNewValueFrom()
            .withNewFieldRef()
            .withFieldPath("status.podIP")
            .endFieldRef()
            .endValueFrom()
            .build(),
            new EnvVarBuilder()
            .withName("POD_NAME")
            .withNewValueFrom()
            .withNewFieldRef()
            .withFieldPath("metadata.name")
            .endFieldRef()
            .endValueFrom()
            .build(),
            new EnvVarBuilder()
            .withName("POD_NAMESPACE")
            .withNewValueFrom()
            .withNewFieldRef()
            .withFieldPath("metadata.namespace")
            .endFieldRef()
            .endValueFrom()
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_UID")
            .withNewValueFrom()
            .withNewFieldRef()
            .withFieldPath("metadata.labels['"
                + StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_UID_KEY + "']")
            .endFieldRef()
            .endValueFrom()
            .build(),
            new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_CONNECT_ADDRESS")
            .withValue(
                getPatroniConnectAddressExpansion(cluster)
                + ":" + EnvoyUtil.PATRONI_PORT)
            .build());

    return ImmutableList.<EnvVar>builder()
        .addAll(patroniEnvVars)
        .addAll(additionalEnvVars)
        .build();

  }

  public static String getPatroniConnectAddressExpansion(StackGresCluster cluster) {
    if (Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getConnectUsingFqdn)
        .orElse(false)) {
      return "${POD_NAME}.${POD_NAMESPACE}";
    }
    return "${POD_IP}";
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

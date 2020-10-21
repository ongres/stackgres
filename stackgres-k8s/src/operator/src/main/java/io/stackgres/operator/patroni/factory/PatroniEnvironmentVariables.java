/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitr;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniEnvironmentVariables
    implements SubResourceStreamFactory<EnvVar, StackGresCluster> {

  @Override
  public Stream<EnvVar> streamResources(StackGresCluster context) {
    return Seq.of(
        new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_LISTEN")
            .withValue("0.0.0.0:" + EnvoyUtil.PATRONI_ENTRY_PORT)
            .build(),
        new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_CONNECT_ADDRESS")
            .withValue("${PATRONI_KUBERNETES_POD_IP}:" + EnvoyUtil.PATRONI_ENTRY_PORT)
            .build(),
        new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_USERNAME")
            .withValue("superuser")
            .build(),
        new EnvVarBuilder()
            .withName("PATRONI_RESTAPI_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getMetadata().getName())
                        .withKey("restapi-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_NAME")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(
                    new ObjectFieldSelectorBuilder()
                        .withFieldPath("metadata.name").build())
                .build()).build(),

        new EnvVarBuilder().withName("PATRONI_KUBERNETES_NAMESPACE")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(
                    new ObjectFieldSelectorBuilder()
                        .withFieldPath("metadata.namespace")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_KUBERNETES_POD_IP")
            .withValueFrom(
                new EnvVarSourceBuilder()
                    .withFieldRef(
                        new ObjectFieldSelectorBuilder()
                            .withFieldPath("status.podIP")
                            .build())
                    .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_SUPERUSER_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getMetadata().getName())
                        .withKey("superuser-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getMetadata().getName())
                        .withKey("replication-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getMetadata().getName())
                        .withKey("authenticator-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_OPTIONS")
            .withValue("superuser")
            .build())
        .append(Seq.of(Optional.ofNullable(context.getSpec())
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
                .build()))
            .filter(Optional::isPresent)
            .map(Optional::get));
  }

}

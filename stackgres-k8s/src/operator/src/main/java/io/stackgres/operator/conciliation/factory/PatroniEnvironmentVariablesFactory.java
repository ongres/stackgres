/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.patroni.StackGresPasswordKeys;

public abstract class PatroniEnvironmentVariablesFactory<T>
    implements ResourceFactory<T, List<EnvVar>> {

  protected List<EnvVar> createPatroniEnvVars(HasMetadata cluster) {
    return List.of(
        new EnvVarBuilder().withName("PATRONI_NAME")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(
                    new ObjectFieldSelectorBuilder()
                        .withFieldPath("metadata.name").build())
                .build())
            .build(),
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
                        .withName(cluster.getMetadata().getName())
                        .withKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY)
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(cluster.getMetadata().getName())
                        .withKey(StackGresPasswordKeys.REPLICATION_PASSWORD_KEY)
                        .build())
                .build())
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
                        .withName(cluster.getMetadata().getName())
                        .withKey(StackGresPasswordKeys.RESTAPI_PASSWORD_KEY)
                        .build())
                .build())
            .build());
  }
}

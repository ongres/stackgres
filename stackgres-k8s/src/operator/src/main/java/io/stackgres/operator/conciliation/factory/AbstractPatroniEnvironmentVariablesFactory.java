/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.patroni.StackGresPasswordKeys;

public abstract class AbstractPatroniEnvironmentVariablesFactory<T>
    implements EnvVarProvider<T> {

  protected List<EnvVar> createPatroniEnvVars(HasMetadata cluster) {
    return List.of(
        new EnvVarBuilder().withName("PATRONI_NAME")
        .withNewValueFrom()
        .withNewFieldRef()
        .withFieldPath("metadata.name")
        .endFieldRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder().withName("POD_IP")
        .withNewValueFrom()
        .withNewFieldRef()
        .withFieldPath("status.podIP")
        .endFieldRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder().withName("POD_NAME")
        .withNewValueFrom()
        .withNewFieldRef()
        .withFieldPath("metadata.name")
        .endFieldRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder().withName("CLUSTER_UID")
        .withNewValueFrom()
        .withNewFieldRef()
        .withFieldPath("metadata.labels['"
            + StackGresContext.STACKGRES_KEY_PREFIX + StackGresContext.CLUSTER_UID_KEY + "']")
        .endFieldRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder().withName("PATRONI_SUPERUSER_PASSWORD")
        .withNewValueFrom()
        .withNewSecretKeyRef()
        .withName(cluster.getMetadata().getName())
        .withKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY)
        .endSecretKeyRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
        .withNewValueFrom()
        .withNewSecretKeyRef()
        .withName(cluster.getMetadata().getName())
        .withKey(StackGresPasswordKeys.REPLICATION_PASSWORD_KEY)
        .endSecretKeyRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder()
        .withName("PATRONI_RESTAPI_CONNECT_ADDRESS")
        .withValue("${POD_IP}:" + EnvoyUtil.PATRONI_PORT)
        .build(),
        new EnvVarBuilder()
        .withName("PATRONI_RESTAPI_USERNAME")
        .withValue("superuser")
        .build(),
        new EnvVarBuilder()
        .withName("PATRONI_RESTAPI_PASSWORD")
        .withNewValueFrom()
        .withNewSecretKeyRef()
        .withName(cluster.getMetadata().getName())
        .withKey(StackGresPasswordKeys.RESTAPI_PASSWORD_KEY)
        .endSecretKeyRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder()
        .withName(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_ENV)
        .withNewValueFrom()
        .withNewSecretKeyRef()
        .withName(cluster.getMetadata().getName())
        .withKey(StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_KEY)
        .endSecretKeyRef()
        .endValueFrom()
        .build(),
        new EnvVarBuilder()
        .withName(StackGresPasswordKeys.AUTHENTICATOR_OPTIONS_ENV)
        .withValue("superuser")
        .build());
  }
}

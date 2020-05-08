/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniEnvironmentVariables
    implements SubResourceStreamFactory<EnvVar, StackGresClusterContext> {

  @Override
  public Stream<EnvVar> streamResources(StackGresClusterContext context) {
    return Seq.of(
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
                        .withName(context.getCluster().getMetadata().getName())
                        .withKey("superuser-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getCluster().getMetadata().getName())
                        .withKey("replication-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getCluster().getMetadata().getName())
                        .withKey("authenticator-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_OPTIONS")
            .withValue("superuser")
            .build());
  }

}

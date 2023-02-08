/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
    io.fabric8.kubernetes.api.model.Pod.class,
    io.fabric8.kubernetes.api.model.PodBuilder.class,
    io.fabric8.kubernetes.api.model.Status.class,
    io.fabric8.kubernetes.client.dsl.internal.HasMetadataOperation.class
})
public class ReflectionConfiguration {
}

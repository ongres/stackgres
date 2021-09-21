/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import org.jetbrains.annotations.NotNull;

public interface StackGresKubernetesClient extends KubernetesClient {

  <T extends HasMetadata> T serverSideApply(@NotNull PatchContext patchContext, @NotNull T intent);

}

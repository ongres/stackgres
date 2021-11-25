/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import org.jetbrains.annotations.NotNull;

public interface StackGresKubernetesClient extends KubernetesClient {

  <T extends HasMetadata> T serverSideApply(@NotNull PatchContext patchContext, @NotNull T intent);

  <T extends HasMetadata> List<T> findManagedIntents(Class<T> resource,
                                                     String fieldManager,
                                                     Map<String, String> labels,
                                                     String namespace);

  <T extends HasMetadata, S, L extends KubernetesResourceList<T>> T updateStatus(
      @NotNull Class<T> resourceClass, @NotNull Class<L> resourceListClass, @NotNull T intent,
      @NotNull Function<T, S> statusGetter, @NotNull BiConsumer<T, S> statusSettes);

}

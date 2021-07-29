/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.resource.ResourceUtil;

public interface LabelFactory<T extends CustomResource<?, ?>> {

  Map<String, String> genericLabels(T resource);

  LabelMapper<T> labelMapper();

  default String resourceName(T resource) {
    return resource.getMetadata().getName();
  }

  default String resourceNamespace(T resource) {
    return resource.getMetadata().getNamespace();
  }

  default String resourceUid(T resource) {
    return resource.getMetadata().getUid();
  }

  default List<OwnerReference> ownerReferences(T resource) {
    return ImmutableList.of(ResourceUtil.getOwnerReference(resource));
  }

}

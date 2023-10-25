/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
public abstract class StorageConfig {

  public abstract String getSize();

  @Nullable
  public abstract String getStorageClass();

  /**
   * The request definition for StorageConfig.
   */
  @Value.Lazy
  public Map<String, Quantity> getStorage() {
    final ImmutableMap.Builder<String, Quantity> req = ImmutableMap.builder();
    req.put("storage", new Quantity(getSize()));
    return req.build();
  }

  /**
   * The request definition for StorageConfig.
   */
  @Value.Lazy
  public ResourceRequirements getResourceRequirements() {
    ResourceRequirements rr = new ResourceRequirements();
    rr.setRequests(getStorage());
    return rr;
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Quantity;

public class ResourcesConfig {

  Map<String, Quantity> request;
  Map<String, Quantity> storage;

  /**
   * Resources Configuration holder.
   *
   * @param cpu Request of CPU
   * @param memory Request of Memory
   * @param storage Request of Storage
   */
  public ResourcesConfig(String cpu, String memory, String storage) {
    this.request = ImmutableMap.of("cpu", new Quantity(cpu),
        "memory", new Quantity(memory));
    this.storage = ImmutableMap.of("storage", new Quantity(storage));
  }

  public Map<String, Quantity> getRequests() {
    return request;
  }

  public Map<String, Quantity> getStorage() {
    return storage;
  }

}

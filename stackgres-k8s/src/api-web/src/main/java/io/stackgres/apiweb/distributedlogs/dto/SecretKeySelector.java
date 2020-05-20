/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs.dto;

import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SecretKeySelector {

  @NotNull
  private String name;

  @NotNull
  private String key;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("name", name)
        .add("key", key)
        .toString();
  }

  public static SecretKeySelector create(String name, String key) {
    SecretKeySelector secretKeySelector = new SecretKeySelector();
    secretKeySelector.setName(name);
    secretKeySelector.setKey(key);
    return secretKeySelector;
  }
}

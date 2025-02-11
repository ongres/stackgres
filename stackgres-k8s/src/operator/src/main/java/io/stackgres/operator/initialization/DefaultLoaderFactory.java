/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.google.common.collect.Maps;

public abstract class DefaultLoaderFactory<T> {

  protected static final JavaPropsMapper MAPPER = new JavaPropsMapper();
  private volatile Properties defaultValues = null;

  public synchronized Properties getDefaultValuesProperties() {
    if (this.defaultValues == null) {
      this.defaultValues = getDefaultPropertiesFile();
    }
    return this.defaultValues;
  }

  protected <R> R buildFromDefaults(Class<R> clazz) {
    try {
      return MAPPER.readPropertiesAs(getDefaultValuesProperties(), clazz);
    } catch (IOException ex) {
      throw new RuntimeException("Couldn't map default properties to spec", ex);
    }
  }

  protected Map<String, String> getDefaultValues() {
    return Maps.fromProperties(getDefaultValuesProperties());
  }

  abstract Properties getDefaultPropertiesFile();

  public abstract T buildResource();

}

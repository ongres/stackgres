/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresUtil;

public abstract class DefaultCustomResourceFactory<T, S extends HasMetadata> {

  public static final String DEFAULT_SUFFIX = "-default";

  public abstract T buildResource(S source);

  public String getDefaultResourceName(S resource) {
    return resource.getMetadata().getName() + DEFAULT_SUFFIX;
  }

  protected static final JavaPropsMapper MAPPER = new JavaPropsMapper();
  private Map<String, Properties> defaultProperties = new HashMap<>();

  private Properties getDefaultProperties(S source) {
    final String defaultPropertyResourceName = getDefaultPropertyResourceName(source);
    synchronized (defaultProperties) {
      Properties properties = defaultProperties.get(defaultPropertyResourceName);
      if (properties == null) {
        properties = loadDefaultProperties(defaultPropertyResourceName);
        defaultProperties.put(
            defaultPropertyResourceName,
            properties);
      }
      return properties;
    }
  }

  protected abstract String getDefaultPropertyResourceName(S source);

  protected Properties loadDefaultProperties(String defaultPropertyResourceName) {
    return StackGresUtil.loadProperties(defaultPropertyResourceName);
  }

  protected <R> R buildFromDefaults(S source, Class<R> clazz) {
    try {
      return MAPPER.readPropertiesAs(getDefaultProperties(source), clazz);
    } catch (IOException ex) {
      throw new RuntimeException("Couldn't map default properties to spec", ex);
    }
  }

  protected Map<String, String> getDefaultValues(S source) {
    return Maps.fromProperties(getDefaultProperties(source));
  }

}

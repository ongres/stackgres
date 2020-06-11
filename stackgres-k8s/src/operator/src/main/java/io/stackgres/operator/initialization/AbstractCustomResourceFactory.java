/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import io.stackgres.common.ConfigContext;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;

public abstract class AbstractCustomResourceFactory<T>
    implements DefaultCustomResourceFactory<T> {

  protected static final JavaPropsMapper MAPPER = new JavaPropsMapper();
  protected Properties defaultValues;

  private ConfigContext<OperatorProperty> context;
  private transient String installedNamespace;

  @PostConstruct
  public void init() throws IOException {
    loadDefaultProperties(getDefaultPropertiesFile());
    installedNamespace = context.getProperty(OperatorProperty.OPERATOR_NAMESPACE)
        .orElseThrow(() -> new IllegalStateException("Operator not configured properly"));
  }

  protected void loadDefaultProperties(String propertiesPath) throws IOException {
    defaultValues = StackGresUtil.loadProperties(propertiesPath);
    getExclusionProperties().forEach(p -> defaultValues.remove(p));
  }

  protected <S> S buildSpec(Class<S> specClazz) {
    try {
      return MAPPER.readPropertiesAs(defaultValues, specClazz);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't map default properties to spec", e);
    }
  }

  protected Map<String, String> getDefaultValues() {
    return defaultValues.stringPropertyNames().stream()
        .collect(Collectors.toMap(Function.identity(), k -> defaultValues.getProperty(k)));
  }

  @Inject
  public void setContext(ConfigContext<OperatorProperty> context) {
    this.context = context;
  }

  abstract String getDefaultPropertiesFile();

  List<String> getExclusionProperties() {
    return Collections.emptyList();
  }

  @Override
  public T buildResource() {
    return buildResource(installedNamespace);
  }

  abstract T buildResource(String namespace);
}

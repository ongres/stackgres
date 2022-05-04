/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.google.common.collect.Maps;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;

public abstract class AbstractCustomResourceFactory<T>
    implements DefaultCustomResourceFactory<T> {

  protected static final JavaPropsMapper MAPPER = new JavaPropsMapper();
  protected Properties defaultValues;

  private StackGresPropertyContext<OperatorProperty> context;
  private String installedNamespace;

  @PostConstruct
  public void init() {
    loadDefaultProperties();
    installedNamespace = context.get(OperatorProperty.OPERATOR_NAMESPACE)
        .orElseThrow(() -> new IllegalStateException("Operator not configured properly"));
  }

  protected void loadDefaultProperties() {
    defaultValues = getDefaultPropertiesFile();
  }

  protected <S> S buildFromDefaults(Class<S> clazz) {
    try {
      return MAPPER.readPropertiesAs(defaultValues, clazz);
    } catch (IOException ex) {
      throw new RuntimeException("Couldn't map default properties to spec", ex);
    }
  }

  protected Map<String, String> getDefaultValues() {
    return Maps.fromProperties(defaultValues);
  }

  @Inject
  public void setContext(StackGresPropertyContext<OperatorProperty> context) {
    this.context = context;
  }

  abstract Properties getDefaultPropertiesFile();

  @Override
  public T buildResource() {
    return buildResource(installedNamespace);
  }

  abstract T buildResource(String namespace);
}

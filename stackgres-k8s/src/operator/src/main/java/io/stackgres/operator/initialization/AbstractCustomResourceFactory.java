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
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresPropertyContext;

public abstract class AbstractCustomResourceFactory<T>
    implements DefaultCustomResourceFactory<T> {

  protected static final JavaPropsMapper MAPPER = new JavaPropsMapper();
  private final StackGresPropertyContext<OperatorProperty> context;
  protected Properties defaultValues;
  private String installedNamespace;

  protected AbstractCustomResourceFactory(StackGresPropertyContext<OperatorProperty> context) {
    this.context = context;
  }

  public AbstractCustomResourceFactory() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.context = null;
  }

  public void init() {
    this.defaultValues = getDefaultPropertiesFile();
    this.installedNamespace = context.get(OperatorProperty.OPERATOR_NAMESPACE)
        .orElseThrow(() -> new IllegalArgumentException("Operator not configured properly"));
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

  abstract Properties getDefaultPropertiesFile();

  @Override
  public T buildResource() {
    return buildResource(installedNamespace);
  }

  abstract T buildResource(String namespace);
}

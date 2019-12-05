/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ConfigProperty;

public abstract class AbstractCustomResourceFactory<T extends CustomResource>
    implements DefaultCustomResourceFactory<T> {

  protected static final JavaPropsMapper MAPPER = new JavaPropsMapper();
  protected Properties defaultValues;

  private ConfigContext context;
  private transient String installedNamespace;

  @PostConstruct
  public void init() throws IOException {
    loadDefaultProperties(getDefaultPropertiesFile());
    installedNamespace = context.getProperty(ConfigProperty.OPERATOR_NAMESPACE)
        .orElseThrow(() -> new IllegalStateException("Operator not configured properly"));
  }

  protected void loadDefaultProperties(String propertiesPath) throws IOException {
    defaultValues = loadProperties(propertiesPath);
    getExclusionProperties().forEach(p -> defaultValues.remove(p));
  }

  protected Properties loadProperties(String propertiesPath) throws IOException {
    try (InputStream is = ClassLoader
        .getSystemResourceAsStream(propertiesPath)) {
      Properties props = new Properties();
      props.load(is);
      return props;
    }
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
  public void setContext(ConfigContext context) {
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

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.config.ConfigContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ConfigLoader implements ConfigContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);

  private Map<String, String> contextProperties = new HashMap<>();

  void onStart(@Observes StartupEvent ev) {

    LOGGER.info("Configuration loaded");

    contextProperties.put(ConfigContext.CONTAINER_BUILD, StackGresUtil.CONTAINER_BUILD);
    contextProperties.put(ConfigContext.CRD_GROUP, StackGresUtil.CRD_GROUP);
    contextProperties.put(ConfigContext.CRD_VERSION, StackGresUtil.CRD_VERSION);
    contextProperties.put(ConfigContext.OPERATOR_NAMESPACE, StackGresUtil.OPERATOR_NAMESPACE);
    contextProperties.put(ConfigContext.OPERATOR_NAME, StackGresUtil.OPERATOR_NAME);
    contextProperties.put(ConfigContext.OPERATOR_VERSION, StackGresUtil.OPERATOR_VERSION);
    contextProperties.put(ConfigContext.PROMETHEUS_AUTOBIND, StackGresUtil.PROMETHEUS_AUTOBIND);

  }

  @Override
  public Optional<String> getProp(String prop) {
    return Optional.ofNullable(contextProperties.get(prop));
  }
}

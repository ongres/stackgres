/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ObjectMapperProvider {

  @Singleton
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper;
  }

}

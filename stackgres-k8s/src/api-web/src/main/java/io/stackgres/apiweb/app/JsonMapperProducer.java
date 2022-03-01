/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.app;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.json.JsonMapper;

@Singleton
public class JsonMapperProducer {

  @Produces
  public JsonMapper buildJsonMapper() {
    return new JsonMapper();
  }
}

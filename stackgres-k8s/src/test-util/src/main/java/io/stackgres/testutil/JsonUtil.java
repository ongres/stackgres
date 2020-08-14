/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class JsonUtil {

  private static final ObjectMapper jsonMapper = new ObjectMapper();

  private JsonUtil() {}

  public static <T> T readFromJson(String resource, Class<T> clazz) {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(clazz, "clazz");
    if (clazz.getPackage().getName().startsWith("io.stackgres")
        && clazz.getAnnotation(RegisterForReflection.class) == null) {
      throw new IllegalStateException("class " + clazz.getName() + " must have the annotation: "
          + RegisterForReflection.class);
    }
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("resource " + resource + " not found");
      }
      try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        return jsonMapper.readValue(CharStreams.toString(reader), clazz);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("could not open resource " + resource, e);
    }
  }

}

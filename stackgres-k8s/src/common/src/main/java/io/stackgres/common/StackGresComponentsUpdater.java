/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.io.Files;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;

public interface StackGresComponentsUpdater {

  static void main(String[] args) throws Exception {
    ObjectMapper objectMapper = new YAMLMapper();
    JsonNode versions = objectMapper.readTree(
        new URL("https://stackgres.io/downloads/stackgres-k8s/stackgres/components/"
            + StackGresProperty.CONTAINER_BUILD.getString() + "/versions.yaml"));
    Properties properties = new Properties();
    Seq.seq(versions.get("components").fields())
        .map(component -> Tuple.tuple(
          component.getKey(), component.getValue().get("versions")))
        .filter(t -> t.v2 != null)
        .map(t -> t.map2(v2 -> v2.isArray()
            ? Seq.seq((ArrayNode) v2)
                .map(JsonNode::asText)
                .toString(",")
            : v2.asText()))
        .peek(t -> System.out.println(String.format(
            "Component %s version %s", t.v1, t.v2)))
        .forEach(t -> properties.put(t.v1, t.v2));
    File file = new File(args[0]);
    Files.createParentDirs(file);
    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
      properties.store(fileOutputStream, null);
    }
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class KubernetesSchemaTestUtil {

  private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";

  private KubernetesSchemaTestUtil() {
  }

  /**
   * Generates a {@link JsonNode} with random data matching the given OpenAPI V3 schema.
   *
   * @param openApiV3Schema the schema node from a CRD YAML
   * @return a JsonNode populated with random data conforming to the schema
   */
  public static JsonNode createWithRandomData(JsonNode openApiV3Schema) {
    return generateNode(openApiV3Schema, new Random(7));
  }

  private static JsonNode generateNode(JsonNode schema, Random random) {
    if (schema == null || schema.isMissingNode() || schema.isNull()) {
      return null;
    }

    String type = schema.has("type") ? schema.get("type").asText() : null;

    if ("object".equals(type)
        || (type == null && schema.has("properties"))
        || (type == null && schema.has("additionalProperties"))
        || (type == null
            && schema.path("x-kubernetes-preserve-unknown-fields").asBoolean(false))) {
      return generateObject(schema, random);
    }
    if ("array".equals(type)) {
      return generateArray(schema, random);
    }
    if ("string".equals(type)) {
      return generateString(schema, random);
    }
    if ("integer".equals(type)) {
      return generateInteger(schema, random);
    }
    if ("number".equals(type)) {
      return generateNumber(random);
    }
    if ("boolean".equals(type)) {
      return BooleanNode.TRUE;
    }

    return null;
  }

  private static ObjectNode generateObject(JsonNode schema, Random random) {
    ObjectNode obj = JsonUtil.jsonMapper().createObjectNode();

    if (schema.has("properties")) {
      var fields = schema.get("properties").properties();
      for (Map.Entry<String, JsonNode> entry : fields) {
        JsonNode value = generateNode(entry.getValue(), random);
        if (value != null) {
          obj.set(entry.getKey(), value);
        }
      }
    } else if (schema.has("additionalProperties")) {
      JsonNode additionalPropsSchema = schema.get("additionalProperties");
      int count = 1 + random.nextInt(2);
      for (int i = 0; i < count; i++) {
        String key = "rnd-" + randomAlphanumeric(random, 10);
        if (additionalPropsSchema.isBoolean()) {
          obj.put(key, "rnd-" + randomAlphanumeric(random, 10));
        } else {
          JsonNode value = generateNode(additionalPropsSchema, random);
          if (value != null) {
            obj.set(key, value);
          }
        }
      }
    } else if (schema.path("x-kubernetes-preserve-unknown-fields").asBoolean(false)) {
      int count = 1 + random.nextInt(2);
      for (int i = 0; i < count; i++) {
        obj.put("rnd-" + randomAlphanumeric(random, 10),
            "rnd-" + randomAlphanumeric(random, 10));
      }
    }

    return obj;
  }

  private static ArrayNode generateArray(JsonNode schema, Random random) {
    ArrayNode arr = JsonUtil.jsonMapper().createArrayNode();
    JsonNode items = schema.get("items");
    if (items != null && !items.isMissingNode()) {
      int count = 1 + random.nextInt(2);
      for (int i = 0; i < count; i++) {
        JsonNode value = generateNode(items, random);
        if (value != null) {
          arr.add(value);
        }
      }
    }
    return arr;
  }

  private static TextNode generateString(JsonNode schema, Random random) {
    if (schema.has("enum")) {
      JsonNode enumValues = schema.get("enum");
      int index = random.nextInt(enumValues.size());
      return new TextNode(enumValues.get(index).asText());
    }
    // There is no other way to detect a quantity type
    if (schema.has("description") && schema.get("description").asText()
        .startsWith("Quantity is a fixed-point representation of a number.")) {
      return new TextNode(random.nextInt() + "Mi");
    }
    return new TextNode("rnd-" + randomAlphanumeric(random, 10));
  }

  private static JsonNode generateInteger(JsonNode schema, Random random) {
    if ("int64".equals(schema.path("format").asText(null))) {
      return LongNode.valueOf(random.nextLong());
    }
    return IntNode.valueOf(random.nextInt());
  }

  private static DecimalNode generateNumber(Random random) {
    return DecimalNode.valueOf(BigDecimal.valueOf(random.nextInt()));
  }

  private static String randomAlphanumeric(Random random, int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
    }
    return sb.toString();
  }

}

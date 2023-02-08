/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.dsl.internal.ObjectMetaMixIn;
import io.fabric8.zjsonpatch.JsonDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class DefaultComparator implements ResourceComparator {

  protected static final Logger LOGGER = LoggerFactory.getLogger("io.stackgres.comparator");

  public static final ObjectMapper PATCH_MAPPER = new ObjectMapper();

  static {
    /*
     * This patch mapper is based in io.fabric8.kubernetes.client.internal.PatchUtils class.
     * It just adds an ORDER_MAP_ENTRIES_BY_KEYS
     */
    PATCH_MAPPER.addMixIn(ObjectMeta.class, ObjectMetaMixIn.class);
    PATCH_MAPPER.setConfig(
        PATCH_MAPPER.getSerializationConfig()
            .without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS));
    PATCH_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    PATCH_MAPPER.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  }

  @Override
  public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {
    ArrayNode diff = getJsonDiff(required, deployed);
    return diff.size() == 0;
  }

  @Override
  public ArrayNode getJsonDiff(HasMetadata required, HasMetadata deployed) {
    final JsonNode source = PATCH_MAPPER.valueToTree(required);
    final JsonNode target = PATCH_MAPPER.valueToTree(deployed);
    ArrayNode diff = (ArrayNode) JsonDiff.asJson(source, target);
    return diff;
  }

}

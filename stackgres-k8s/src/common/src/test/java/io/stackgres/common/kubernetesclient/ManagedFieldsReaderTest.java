/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.GeneratorTestUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ManagedFieldsReaderTest {

  private final ObjectNode managedFieldsService = JsonUtil
      .readFromJsonAsJson("services/primary-service-with-managed-fields.json");

  private final ObjectNode cleanedUpService = JsonUtil
      .readFromJsonAsJson("services/primary-service.json");

  private final ObjectNode managedFieldsSecret = JsonUtil
      .readFromJsonAsJson("secret/backup-secret-with-managed-fields.json");

  private final ObjectNode cleanedUpSecret = JsonUtil
      .readFromJsonAsJson("secret/backup-secret.json");

  private final ObjectNode managedFieldsSts = JsonUtil
      .readFromJsonAsJson("statefulset/statefulset-with-managed-fields.json");

  private final StatefulSet cleanedUpSta = JsonUtil
      .readFromJson("statefulset/statefulset-without-managed-fields.json", StatefulSet.class);

  @Test
  @DisplayName("getOnlyManagedFields should return the same without the fields that are not "
      + "managed")
  void testGetOnlyManagedFields() throws JsonProcessingException {

    var actualReturn = ManagedFieldsReader.readManagedFields(managedFieldsService,
        ResourceWriter.STACKGRES_FIELD_MANAGER);

    assertEquals(cleanedUpService, actualReturn);

    var actualSecret = ManagedFieldsReader.readManagedFields(managedFieldsSecret,
        ResourceWriter.STACKGRES_FIELD_MANAGER);

    assertEquals(cleanedUpSecret, actualSecret);

    var actualSts = ManagedFieldsReader.readManagedFields(managedFieldsSts,
        ResourceWriter.STACKGRES_FIELD_MANAGER);

    GeneratorTestUtil.assertResourceEquals(
        Serialization.jsonMapper().treeToValue(actualSts, StatefulSet.class),
        cleanedUpSta
    );

  }

  @Test
  @DisplayName("getManagedFieldConfiguration should return the appropriate "
      + "managed fields configuration")
  void testGetManagedFieldConfiguration() {

    var managedFieldConfiguration = ManagedFieldsReader
        .getManagedFieldConfiguration(
            managedFieldsService,
            ResourceWriter.STACKGRES_FIELD_MANAGER
        );

    var expectedManagedFieldConfiguration = (ObjectNode) managedFieldsService.get("metadata")
        .get("managedFields").get(0);

    assertEquals(expectedManagedFieldConfiguration, managedFieldConfiguration);
  }

  @Test
  @DisplayName("findMatchingItem should return the appropriate item")
  void testFindMatchingItem() throws JsonProcessingException {
    ArrayNode arrayNode = (ArrayNode) JsonUtil.JSON_MAPPER.readTree(
        "[\n"
            + "{\n"
            + "  \"name\": \"pgport\",\n"
            + "  \"port\": 5432,\n"
            + "  \"protocol\": \"TCP\",\n"
            + "  \"targetPort\": \"pgport\"\n"
            + "},\n"
            + "  {\n"
            + "    \"name\": \"pgreplication\",\n"
            + "    \"port\": 5433,\n"
            + "    \"protocol\": \"TCP\",\n"
            + "    \"targetPort\": \"pgreplication\"\n"
            + "  }\n"
            + "]"
    );

    ObjectNode key = (ObjectNode) JsonUtil.JSON_MAPPER.readTree(
        "{\n"
            + "  \"port\": 5433,\n"
            + "  \"protocol\": \"TCP\"\n"
            + "}"
    );

    ObjectNode expectedItem = (ObjectNode) JsonUtil.JSON_MAPPER.readTree(
        "{\n"
            + "  \"name\": \"pgreplication\",\n"
            + "  \"port\": 5433,\n"
            + "  \"protocol\": \"TCP\",\n"
            + "  \"targetPort\": \"pgreplication\"\n"
            + "}"
    );

    assertEquals(expectedItem, ManagedFieldsReader.findMatchingItem(arrayNode, key));

  }
}

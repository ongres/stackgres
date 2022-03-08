/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.GeneratorTestUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ManagedFieldsReaderTest {

  private final ObjectNode managedFieldsService = Fixtures.jsonService()
      .loadPrimaryServiceWithManagedFields().get();

  private final ObjectNode cleanedUpService = Fixtures.jsonService()
      .loadPrimaryService().get();

  private final ObjectNode managedFieldsSecret = Fixtures.jsonSecret()
      .loadBackupWithManagedFields().get();

  private final ObjectNode cleanedUpSecret = Fixtures.jsonSecret()
      .loadBackup().get();

  private final ObjectNode managedFieldsSts = Fixtures.jsonStatefulSet()
      .loadWithManagedFields().get();

  private final StatefulSet cleanedUpSta = Fixtures.statefulSet()
      .loadWithoutManagedFields().get();

  @Test
  @DisplayName("getOnlyManagedFields should return the same without the fields that are not "
      + "managed")
  void testGetOnlyManagedFields() throws JsonProcessingException {

    var actualReturn = ManagedFieldsReader.readManagedFields(managedFieldsService,
        ResourceWriter.STACKGRES_FIELD_MANAGER);

    JsonUtil.assertJsonEquals(cleanedUpService, actualReturn);

    var actualSecret = ManagedFieldsReader.readManagedFields(managedFieldsSecret,
        ResourceWriter.STACKGRES_FIELD_MANAGER);

    JsonUtil.assertJsonEquals(cleanedUpSecret, actualSecret);

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

    JsonUtil.assertJsonEquals(expectedManagedFieldConfiguration, managedFieldConfiguration);
  }

  @Test
  @DisplayName("findMatchingItem should return the appropriate item")
  void testFindMatchingItem() throws JsonProcessingException {
    ArrayNode arrayNode = (ArrayNode) JsonUtil.jsonMapper().readTree(
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

    ObjectNode key = (ObjectNode) JsonUtil.jsonMapper().readTree(
        "{\n"
            + "  \"port\": 5433,\n"
            + "  \"protocol\": \"TCP\"\n"
            + "}"
    );

    ObjectNode expectedItem = (ObjectNode) JsonUtil.jsonMapper().readTree(
        "{\n"
            + "  \"name\": \"pgreplication\",\n"
            + "  \"port\": 5433,\n"
            + "  \"protocol\": \"TCP\",\n"
            + "  \"targetPort\": \"pgreplication\"\n"
            + "}"
    );

    JsonUtil.assertJsonEquals(expectedItem, ManagedFieldsReader.findMatchingItem(arrayNode, key));

  }
}

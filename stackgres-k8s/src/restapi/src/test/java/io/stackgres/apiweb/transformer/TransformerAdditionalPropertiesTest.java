/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.crd.storages.BackupStorage;
import org.junit.jupiter.api.Test;

/**
 * The REST API rebuilds the custom resource from the DTO on update, so properties the models do not
 * declare have to survive the trip in both directions or the web console drops them on save.
 */
class TransformerAdditionalPropertiesTest {

  private static ObjectMeta metadata() {
    ObjectMeta metadata = new ObjectMeta();
    metadata.setNamespace("default");
    metadata.setName("test");
    return metadata;
  }

  /**
   * Transformers that convert the whole spec carry the captured properties on their own.
   */
  @Test
  void convertingTransformerKeepsUnknownProperties() {
    ObjectStorageTransformer transformer =
        new ObjectStorageTransformer(JsonMapper.builder().build());
    StackGresObjectStorage objectStorage = new StackGresObjectStorage();
    objectStorage.setMetadata(metadata());
    objectStorage.setSpec(new BackupStorage());
    objectStorage.getSpec().setAdditionalProperty("futureField", Map.of("aKey", "aValue"));

    ObjectStorageDto dto = transformer.toResource(objectStorage, List.of());
    assertEquals(Map.of("aKey", "aValue"),
        dto.getSpec().getAdditionalProperties().get("futureField"));

    StackGresObjectStorage back = transformer.toCustomResource(dto, objectStorage);
    assertEquals(Map.of("aKey", "aValue"),
        back.getSpec().getAdditionalProperties().get("futureField"));
  }

  /**
   * Transformers that rebuild the spec field by field have to copy them explicitly.
   */
  @Test
  void rebuildingTransformerKeepsUnknownProperties() {
    StackGresPostgresConfig config = new StackGresPostgresConfig();
    config.setMetadata(metadata());
    config.setSpec(new StackGresPostgresConfigSpec());
    config.getSpec().setPostgresVersion("17");
    config.getSpec().setPostgresqlConf(Map.of());
    config.getSpec().setAdditionalProperty("futureField", Map.of("aKey", "aValue"));

    PostgresConfigTransformer transformer =
        new PostgresConfigTransformer(JsonMapper.builder().build());
    PostgresConfigDto dto = transformer.toResource(config, List.of());
    assertEquals(Map.of("aKey", "aValue"),
        dto.getSpec().getAdditionalProperties().get("futureField"));

    StackGresPostgresConfig back = transformer.toCustomResource(dto, config);
    assertEquals(Map.of("aKey", "aValue"),
        back.getSpec().getAdditionalProperties().get("futureField"));
  }

}

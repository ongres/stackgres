/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRestoreMutatorTest {

  protected static final ObjectMapper MAPPER = JsonUtil.JSON_MAPPER;

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresClusterReview review;

  @Mock
  private DefaultCustomResourceFactory<StackGresClusterRestore> defaultRestoreFactory;

  private DefaultRestoreMutator mutator;

  private Properties defaultRestoreValues;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {

    review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    defaultRestoreValues = new Properties();

    try (InputStream defaultPropertiesStream = ClassLoader
        .getSystemResourceAsStream("restore-default-values.properties")) {
      defaultRestoreValues.load(defaultPropertiesStream);
    }

    StackGresClusterRestore restore = PROPS_MAPPER
        .readPropertiesAs(defaultRestoreValues, StackGresClusterRestore.class);
    when(defaultRestoreFactory.buildResource()).thenReturn(restore);

    mutator = new DefaultRestoreMutator();
    mutator.setDefaultRestoreFactory(defaultRestoreFactory);
    mutator.setObjectMapper(MAPPER);
    mutator.init();

  }

  @Test
  void clusterWithNoRestore_shouldNotDoAnything() {

    review.getRequest().getObject().getSpec().getInitData().setRestore(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());
  }

  @Test
  void clusteRestorerWithNoDownloadDiskConcurrency_shouldSetDefaultValue()
      throws JsonPatchException {

    StackGresClusterRestore restore = new StackGresClusterRestore();
    restore.setDownloadDiskConcurrency(null);
    restore.setFromBackup(new StackGresClusterRestoreFromBackup());
    restore.getFromBackup().setUid(UUID.randomUUID().toString());

    review.getRequest().getObject().getSpec().getInitData().setRestore(restore);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode crJson = MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    Integer defaultDownloadDisConcurrency = Integer
        .parseInt(defaultRestoreValues.getProperty("downloadDiskConcurrency"));

    int actualDownloadDiskConcurrency = newConfig.get("spec").get("initialData").get("restore")
        .get("downloadDiskConcurrency").asInt();
    assertEquals(defaultDownloadDisConcurrency, actualDownloadDiskConcurrency);

  }
}

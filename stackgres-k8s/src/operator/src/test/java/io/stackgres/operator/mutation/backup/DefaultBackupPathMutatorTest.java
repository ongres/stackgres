/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class DefaultBackupPathMutatorTest {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private BackupReview review;
  private DefaultBackupPathMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = JsonUtil.readFromJson("backup_allow_request/create.json",
        BackupReview.class);

    mutator = new DefaultBackupPathMutator();
    mutator.init();
  }

  @Test
  void backupWithBackupPath_shouldSetNothing() {
    StackGresBackup actualBackup = mutate(review);

    assertEquals(review.getRequest().getObject(), actualBackup);
  }

  @Test
  void backupWithoutBackupPath_shouldSetNothing() {
    review.getRequest().getObject().getStatus().setBackupPath(null);
    final StackGresBackup actualBackup = mutate(review);

    assertEquals(review.getRequest().getObject(), actualBackup);
  }

  @Test
  void oldBackupWithoutBackupPath_shouldSetItWithPreviousVersion() {
    review.getRequest().getObject().getMetadata().setAnnotations(new HashMap<>());
    review.getRequest().getObject().getMetadata().getAnnotations()
        .put(StackGresContext.VERSION_KEY, "1.1");
    review.getRequest().getObject().getStatus().setBackupPath(null);
    final StackGresBackup actualBackup = mutate(review);

    final StackGresBackup backup = review.getRequest().getObject();
    assertEquals(
        BackupStorageUtil.getPathPre_1_2(
            backup.getMetadata().getNamespace(),
            backup.getMetadata().getName(),
            backup.getStatus().getBackupConfig().getStorage()),
        actualBackup.getStatus().getBackupPath());
  }

  private StackGresBackup mutate(BackupReview review) {
    try {
      List<JsonPatchOperation> operations = mutator.mutate(review);
      JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());
      JsonNode newConfig = new JsonPatch(operations).apply(crJson);
      return JSON_MAPPER.treeToValue(newConfig, StackGresBackup.class);
    } catch (JsonPatchException | JsonProcessingException | IllegalArgumentException e) {
      throw new AssertionFailedError(e.getMessage(), e);
    }
  }
}

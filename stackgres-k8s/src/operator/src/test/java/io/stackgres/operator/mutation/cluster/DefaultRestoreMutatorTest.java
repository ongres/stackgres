/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultClusterRestoreFactory;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRestoreMutatorTest {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresClusterReview review;

  private DefaultRestoreMutator mutator;

  private Properties defaultRestoreValues;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {

    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    defaultRestoreValues = new Properties();

    try (InputStream defaultPropertiesStream = ClassLoader
        .getSystemResourceAsStream("restore-default-values.properties")) {
      defaultRestoreValues.load(defaultPropertiesStream);
    }

    DefaultClusterRestoreFactory defaultRestoreFactory = new DefaultClusterRestoreFactory();
    mutator = new DefaultRestoreMutator(defaultRestoreFactory, JSON_MAPPER);
  }

  @Test
  void clusterWithNoRestore_shouldNotDoAnything() {
    review.getRequest().getObject().getSpec().getInitialData().setRestore(null);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusteRestorerWithNoDownloadDiskConcurrency_shouldSetDefaultValue()
      throws JsonPatchException {
    StackGresClusterRestore restore = new StackGresClusterRestore();
    restore.setDownloadDiskConcurrency(null);
    restore.setFromBackup(new StackGresClusterRestoreFromBackup());
    restore.getFromBackup().setUid(UUID.randomUUID().toString());

    review.getRequest().getObject().getSpec().getInitialData().setRestore(restore);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    Integer defaultDownloadDisConcurrency = Integer
        .parseInt(defaultRestoreValues.getProperty("downloadDiskConcurrency"));

    assertEquals(defaultDownloadDisConcurrency,
        result.getSpec().getInitialData().getRestore().getDownloadDiskConcurrency());
  }

}

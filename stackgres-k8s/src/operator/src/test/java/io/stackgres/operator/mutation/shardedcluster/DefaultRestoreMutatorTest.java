/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterInitialDataBuilder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultShardedClusterRestoreFactory;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRestoreMutatorTest {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresShardedClusterReview review;

  private DefaultRestoreMutator mutator;

  private Properties defaultRestoreValues;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec().setInitialData(
        new StackGresShardedClusterInitialDataBuilder()
        .withNewRestore()
        .withNewFromBackup()
        .withName("test")
        .endFromBackup()
        .endRestore()
        .build());

    defaultRestoreValues = new Properties();

    try (InputStream defaultPropertiesStream = ClassLoader
        .getSystemResourceAsStream("restore-default-values.properties")) {
      defaultRestoreValues.load(defaultPropertiesStream);
    }

    var defaultRestoreFactory = new DefaultShardedClusterRestoreFactory();
    mutator = new DefaultRestoreMutator(defaultRestoreFactory, JSON_MAPPER);
  }

  @Test
  void clusterWithNoRestore_shouldNotDoAnything() {
    review.getRequest().getObject().getSpec().getInitialData().setRestore(null);

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusteRestorerWithNoDownloadDiskConcurrency_shouldSetDefaultValue()
      throws JsonPatchException {
    review.getRequest().getObject().getSpec().getInitialData()
        .getRestore().setDownloadDiskConcurrency(null);

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    Integer defaultDownloadDisConcurrency = Integer
        .parseInt(defaultRestoreValues.getProperty("downloadDiskConcurrency"));

    assertEquals(defaultDownloadDisConcurrency,
        result.getSpec().getInitialData().getRestore().getDownloadDiskConcurrency());
  }

}

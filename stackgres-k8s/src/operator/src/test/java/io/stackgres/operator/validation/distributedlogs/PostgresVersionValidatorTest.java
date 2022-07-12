/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PostgresVersionValidatorTest {

  private static String getMajorPostgresVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

  private PostgresConfigValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresPostgresConfig> configFinder;

  private String distributedLogsPostgresVersion;
  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    validator = new PostgresConfigValidator(configFinder);
    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);
    StackGresDistributedLogs distributedLogs = new StackGresDistributedLogs();
    distributedLogs.setMetadata(new ObjectMeta());
    distributedLogs.getMetadata().setAnnotations(
        Map.of(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion()));
    distributedLogsPostgresVersion = StackGresDistributedLogsUtil
        .getPostgresVersion(distributedLogs);
  }

  @Test
  void givenValidCreation_shouldNotFail() throws ValidationFailed {
    final StackGresDistributedLogsReview review = JsonUtil
        .readFromJson("distributedlogs_allow_request/create.json",
            StackGresDistributedLogsReview.class);

    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    String postgresConfigName = spec.getConfiguration().getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresConfigName), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(
        distributedLogsPostgresVersion));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresConfigName), eq(namespace));
  }

  @Test
  void givenInconsistentPostgresVersion_shouldFail() {
    final StackGresDistributedLogsReview review = JsonUtil
        .readFromJson("distributedlogs_allow_request/create.json",
            StackGresDistributedLogsReview.class);
    postgresConfig.getSpec().setPostgresVersion("10");

    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    String postgresConfigName = spec.getConfiguration().getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresConfigName), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals(
        "Invalid postgres version for sgPostgresConfig " + postgresConfigName
        + ", it must be " + getMajorPostgresVersion(distributedLogsPostgresVersion),
        resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresConfigName), eq(namespace));
  }

  @Test
  void givenSamePostgresVersionUpdate_shouldNotFail() throws ValidationFailed {
    final StackGresDistributedLogsReview review = JsonUtil
        .readFromJson("distributedlogs_allow_request/update.json",
            StackGresDistributedLogsReview.class);

    validator.validate(review);
  }

  @Test
  void givenInvalidPostgresConfigReference_shouldFail() {
    final StackGresDistributedLogsReview review = JsonUtil
        .readFromJson("distributedlogs_allow_request/create.json",
            StackGresDistributedLogsReview.class);
    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    String postgresConfigName = spec.getConfiguration().getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresConfigName), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid sgPostgresConfig value " + postgresConfigName, resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresConfigName), eq(namespace));
  }

  @Test
  void givenEmptyPostgresConfigReference_shouldFail() {
    final StackGresDistributedLogsReview review = JsonUtil
        .readFromJson("distributedlogs_allow_request/create.json",
            StackGresDistributedLogsReview.class);

    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    spec.getConfiguration().setPostgresConfig("");

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("sgPostgresConfig must be provided", resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenNoPostgresConfigReference_shouldFail() {
    final StackGresDistributedLogsReview review = JsonUtil
        .readFromJson("distributedlogs_allow_request/create.json",
            StackGresDistributedLogsReview.class);

    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    spec.getConfiguration().setPostgresConfig(null);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("sgPostgresConfig must be provided", resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenPostgresConfigUpdateWithInvalidVersion_shouldFail() throws ValidationFailed {
    final StackGresDistributedLogsReview review = JsonUtil
        .readFromJson("distributedlogs_allow_request/update.json",
            StackGresDistributedLogsReview.class);

    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    spec.getConfiguration().setPostgresConfig("test");
    String postgresConfigName = spec.getConfiguration().getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresConfigName), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid sgPostgresConfig value " + postgresConfigName, resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresConfigName), eq(namespace));
  }

  @Test
  void givenADeleteUpdate_shouldDoNothing() throws ValidationFailed {
    final StackGresDistributedLogsReview review = JsonUtil
        .readFromJson("distributedlogs_allow_request/delete.json",
            StackGresDistributedLogsReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);
    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

}

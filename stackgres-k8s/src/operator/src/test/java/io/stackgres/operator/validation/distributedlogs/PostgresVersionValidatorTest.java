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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.StackGresDistributedLogsUtil;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
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
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    StackGresDistributedLogs distributedLogs = new StackGresDistributedLogs();
    distributedLogs.setMetadata(new ObjectMeta());
    distributedLogs.getMetadata().setAnnotations(
        Map.of(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion()));
    distributedLogsPostgresVersion = StackGresDistributedLogsUtil
        .getPostgresVersion(distributedLogs);
  }

  @Test
  void givenValidCreation_shouldNotFail() throws ValidationFailed {
    final StackGresDistributedLogsReview review =
        AdmissionReviewFixtures.distributedLogs().loadCreate().get();

    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    String postgresConfigName = spec.getConfigurations().getSgPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresConfigName), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(
        distributedLogsPostgresVersion));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(eq(postgresConfigName), eq(namespace));
  }

  @Test
  void givenInconsistentPostgresVersion_shouldFail() {
    final StackGresDistributedLogsReview review =
        AdmissionReviewFixtures.distributedLogs().loadCreate().get();
    postgresConfig.getSpec().setPostgresVersion("10");

    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    String postgresConfigName = spec.getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresConfigName), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals(
        "Invalid postgres version for SGPostgresConfig " + postgresConfigName
        + ", it must be " + getMajorPostgresVersion(distributedLogsPostgresVersion),
        resultMessage);

    verify(configFinder, times(2)).findByNameAndNamespace(eq(postgresConfigName), eq(namespace));
  }

  @Test
  void givenSamePostgresVersionUpdate_shouldNotFail() throws ValidationFailed {
    final StackGresDistributedLogsReview review =
        AdmissionReviewFixtures.distributedLogs().loadUpdate().get();

    validator.validate(review);
  }

  @Test
  void givenInvalidPostgresConfigReference_shouldFail() {
    final StackGresDistributedLogsReview review =
        AdmissionReviewFixtures.distributedLogs().loadCreate().get();
    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    String postgresConfigName = spec.getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresConfigName), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("SGPostgresConfig " + postgresConfigName + " not found", resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresConfigName), eq(namespace));
  }

  @Test
  void givenPostgresConfigUpdateWithInvalidVersion_shouldFail() throws ValidationFailed {
    final StackGresDistributedLogsReview review =
        AdmissionReviewFixtures.distributedLogs().loadUpdate().get();

    StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    spec.getConfigurations().setSgPostgresConfig("test");
    String postgresConfigName = spec.getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresConfigName), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Cannot update to SGPostgresConfig "
        + postgresConfigName + " because it doesn't exists", resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresConfigName), eq(namespace));
  }

  @Test
  void givenADeleteUpdate_shouldDoNothing() throws ValidationFailed {
    final StackGresDistributedLogsReview review =
        AdmissionReviewFixtures.distributedLogs().loadDelete().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);
    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

}

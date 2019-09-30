package io.stackgres.operator.validation.validators;

import java.util.Optional;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.services.PostgresConfigFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.AdmissionReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PostgresVersionTest {

  private static final String[] supportedPostgresMajorVersions = {"9.4", "9.5", "9.6", "10", "11"};
  private static final String[] latestPostgresMinorVersions = {"24", "19", "15", "10", "5"};

  private static final ObjectMapper mapper = new ObjectMapper();




  private static String getRandomPostgresVersion() {
    Random r = new Random();
    int versionIndex = r.nextInt(5);
    String majorVersion = supportedPostgresMajorVersions[versionIndex];
    String minorVersion = latestPostgresMinorVersions[versionIndex];
    return majorVersion + "." + minorVersion;
  }

  private PostgresVersion validator;

  private PostgresConfigFinder configFinder;

  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() throws Exception {
    configFinder = mock(PostgresConfigFinder.class);
    validator = new PostgresVersion(configFinder);

    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);

  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();

    when(configFinder.findPostgresConfig(eq(postgresProfile))).thenReturn(Optional.of(postgresConfig));

    final String randomMajorVersion = getRandomPostgresVersion();
    spec.setPostgresVersion(randomMajorVersion);
    postgresConfig.getSpec().setPgVersion(randomMajorVersion);

    validator.validate(review);

    verify(configFinder).findPostgresConfig(eq(postgresProfile));
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/major_postgres_version_update.json",
            AdmissionReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pg_version update, only minor version of postgres can be " +
        "updated, current major version: 10", resultMessage);

  }

  @Test
  void givenInvalidPostgresConfigReference_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();

    when(configFinder.findPostgresConfig(eq(postgresProfile))).thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pg_config value " + postgresProfile, resultMessage);

    verify(configFinder).findPostgresConfig(eq(postgresProfile));
  }


}

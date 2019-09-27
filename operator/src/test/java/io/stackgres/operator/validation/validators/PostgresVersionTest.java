package io.stackgres.operator.validation.validators;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.services.PostgresConfigFinder;
import io.stackgres.operator.validation.AdmissionReview;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostgresVersionTest {

  private static final String[] supportedPostgresMajorVersions = {"9.4", "9.5", "9.6", "10", "11"};
  private static final String[] latestPostgresMinorVersions = {"24", "19", "15", "10", "5"};

  private static final ObjectMapper mapper = new ObjectMapper();

  private static AdmissionReview loadAdmissionReviewFromFile(String resource) {
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("resource " + resource + "not found");
      }
      String json = IOUtils.toString(is, StandardCharsets.UTF_8);
      return mapper.readValue(json, AdmissionReview.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("could not open resource " + resource);
    }
  }



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

    postgresConfig = new StackGresPostgresConfig();
    postgresConfig.setApiVersion("stackgres.io/v1");
    postgresConfig.setSpec();
  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = loadAdmissionReviewFromFile(
        "allowed_requests/valid_creation.json");

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();

    when(configFinder.findPostgresConfig(eq(postgresProfile))).thenReturn(Optional.of(Spec));

    final String randomMajorVersion = getRandomPostgresVersion();
    spec.setPostgresVersion(randomMajorVersion);

    validator.validate(review);

  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldFail() {

    final AdmissionReview review = loadAdmissionReviewFromFile(
        "allowed_requests/major_postgres_version_update.json");

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Postgres major version cannot be updated", resultMessage);

  }

}

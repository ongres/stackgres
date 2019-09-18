package io.stackgres.operator.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResourceTest extends JerseyTest {

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Override
  protected Application configure() {
    return new ResourceConfig(ValidationResource.class);
  }

  private static final ObjectMapper mapper = new ObjectMapper();

  static String getFileAsString(String resource){
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)){
      if (is == null){
        throw new IllegalArgumentException("resource " + resource + "not found");
      }
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("could not open resource " + resource);
    }
  }

  @Test
  void givenValidAllowedRequest_thenReturnedStatusShouldBe200() {

    String requestBody = getFileAsString("allowed_requests/sample_valid_allowed_request.json");

    Response response = target("/validation").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody));

    assertEquals(200, response.getStatus());

  }

  @Test
  void givenValidAllowedRequest_thenResponseShouldNotContainStatusProperty() throws IOException {

    String requestBody = getFileAsString("allowed_requests/sample_valid_allowed_request.json");

    Response response = target("/validation").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody));

    String rawContent = response.readEntity(String.class);



    JsonNode admissionResponse = mapper.readTree(rawContent);

    assertTrue(admissionResponse.has("status"));

  }

  @Test
  void givenValidAllowedRequest_thenAdmissionShouldBeAllowed() {

    String requestBody = getFileAsString("allowed_requests/sample_valid_allowed_request.json");

    Response response = target("/validation").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody));

    AdmissionResponse admissionResponse = response.readEntity(AdmissionResponse.class);

    assertTrue(admissionResponse.isAllowed());

  }

  @Test
  void givenValidAllowedRequest_thenResponseUidShouldMatchRequestUid() throws IOException {

    String requestBody = getFileAsString("allowed_requests/sample_valid_allowed_request.json");

    JsonNode admissionRequest = mapper.readTree(requestBody);

    UUID requestUid = UUID.fromString(admissionRequest.get("request").get("uid").asText());

    Response response = target("/validation").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody));

    AdmissionResponse admissionResponse = response.readEntity(AdmissionResponse.class);

    assertEquals(requestUid, admissionResponse.getUid());

  }
}

/*
 *
 *  * Copyright (C) 2019 OnGres, Inc.
 *  * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 *
 */

package io.stackgres.operator.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.operator.validation.cluster.AlwaysSuccess;
import io.stackgres.operator.validation.cluster.ClusterValidationPipeline;
import io.stackgres.operator.validation.cluster.ClusterValidator;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Instance;
import javax.inject.Singleton;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ClusterValidationResourceTest extends JerseyTest {

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
    return new ResourceConfig(ClusterValidationResource.class)
        .register(new AbstractBinder(){

      @Override
      protected void configure() {

        ClusterValidationPipeline pipeline = new ClusterValidationPipeline(getValidators());

        bind(pipeline).to(ClusterValidationPipeline.class).in(Singleton.class);

      }
    });
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

    String requestBody = getFileAsString("allowed_requests/valid_creation.json");

    Response response = target("/cluster/validation").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody));

    assertEquals(200, response.getStatus());

  }

  @Test
  void givenValidAllowedRequest_thenResponseShouldNotContainStatusProperty() throws IOException {

    String requestBody = getFileAsString("allowed_requests/valid_creation.json");

    Response response = target("/cluster/validation").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody));

    String rawContent = response.readEntity(String.class);



    JsonNode admissionResponse = mapper.readTree(rawContent).get("response");

    assertFalse(admissionResponse.has("status"));

  }

  @Test
  void givenValidAllowedRequest_thenAdmissionShouldBeAllowed() {

    String requestBody = getFileAsString("allowed_requests/valid_creation.json");

    Response response = target("/cluster/validation").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody));

    AdmissionReviewResponse admissionResponse = response.readEntity(AdmissionReviewResponse.class);

    assertTrue(admissionResponse.getResponse().isAllowed());

  }

  @Test
  void givenValidAllowedRequest_thenResponseUidShouldMatchRequestUid() throws IOException {

    String requestBody = getFileAsString("allowed_requests/valid_creation.json");

    JsonNode admissionRequest = mapper.readTree(requestBody);

    UUID requestUid = UUID.fromString(admissionRequest.get("request").get("uid").asText());

    Response response = target("/cluster/validation").request(MediaType.APPLICATION_JSON).post(Entity.json(requestBody));

    AdmissionReviewResponse admissionResponse = response.readEntity(AdmissionReviewResponse.class);

    assertEquals(requestUid, admissionResponse.getResponse().getUid());

  }

  private static Instance<ClusterValidator> getValidators(){
    return new Instance<ClusterValidator>() {
      @Override
      public Instance<ClusterValidator> select(Annotation... annotations) {
        return null;
      }

      @Override
      public <U extends ClusterValidator> Instance<U> select(Class<U> aClass, Annotation... annotations) {
        return null;
      }

      @Override
      public <U extends ClusterValidator> Instance<U> select(javax.enterprise.util.TypeLiteral<U> typeLiteral, Annotation... annotations) {
        return null;
      }


      @Override
      public boolean isUnsatisfied() {
        return false;
      }

      @Override
      public boolean isAmbiguous() {
        return false;
      }

      @Override
      public void destroy(ClusterValidator validator) {

      }

      @Override
      public Iterator<ClusterValidator> iterator() {
        Iterable<ClusterValidator> validators = Collections.singletonList(new AlwaysSuccess());
        return validators.iterator();
      }

      @Override
      public ClusterValidator get() {
        return null;
      }
    };
  }
}

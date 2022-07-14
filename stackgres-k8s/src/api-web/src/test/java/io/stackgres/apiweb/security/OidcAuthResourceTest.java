/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.MalformedURLException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.Cookie;
import io.stackgres.apiweb.testprofile.EnableOidcAuth;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(EnableOidcAuth.class)
class OidcAuthResourceTest {

  private static WebClient webClient;

  @BeforeAll
  public static void init()
      throws FailingHttpStatusCodeException, MalformedURLException, IOException {
    webClient = new WebClient();
    webClient.setCssErrorHandler(new SilentCssErrorHandler());
    HtmlPage page = webClient.getPage("http://localhost:8081/stackgres/applications");
    HtmlForm loginForm = page.getForms().get(0);
    loginForm.getInputByName("username").setValueAttribute("alice");
    loginForm.getInputByName("password").setValueAttribute("alice");
    loginForm.getInputByName("login").click();
  }

  @AfterAll
  public static void close() {
    webClient.close();
  }

  @Test
  @DisplayName("Enpoint without auth should redirect to Keycloak")
  void givenEndpointWithoutAuth_shouldRedirect() {
    given()
        .auth().none()
        .get("/stackgres/applications")
        .then()
        .statusCode(200)
        .body("html.head.title", is("Sign in to quarkus"));
  }

  @Test
  @DisplayName("Enpoint with auth Cookie should validate")
  void givenEndpointWithAuthCookie_shouldNotFail() {
    given()
        .cookie(getRestAssuredSessionCookie(webClient))
        .get("/stackgres/applications")
        .then()
        .body("applications", hasSize(1))
        .body("applications.name", hasItems("babelfish-compass"))
        .body("applications.publisher", hasItems("com.ongres"))
        .statusCode(200);
  }

  @Test
  @DisplayName("Enpoint requested with JavaScript should return client error response")
  void givenEndpoint_shouldReturnClientError() {
    given()
        .header("X-Requested-With", "JavaScript")
        .when()
        .get("/stackgres/applications")
        .then()
        .header("WWW-Authenticate", "OIDC")
        .statusCode(499);
  }

  @Test
  @DisplayName("Enpoint type should be OIDC")
  void givenEndpoint_shouldReturnOidc() {
    given()
        .get("/stackgres/auth/type")
        .then()
        .header("WWW-Authenticate", "OIDC")
        .body("type", is("OIDC"))
        .statusCode(200);
  }

  /**
   * Transform htmlunit Cookie to restassured Cookie.
   */
  private Cookie getRestAssuredSessionCookie(WebClient webClient) {
    var cookie = webClient.getCookieManager().getCookie("q_session");
    return new Cookie.Builder(cookie.getName(), cookie.getValue())
        .setExpiryDate(cookie.getExpires())
        .setHttpOnly(cookie.isHttpOnly())
        .setSecured(cookie.isSecure())
        .setDomain(cookie.getDomain())
        .setPath(cookie.getPath())
        .build();
  }

}

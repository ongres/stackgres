
package io.stackgres.apiweb.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.security.AuthenticationFailedException;
import io.stackgres.apiweb.config.WebApiContext;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SecretVerificationTest {

  @Mock
  private ResourceScanner<Secret> secretScanner;

  private Secret secret;

  private SecretVerification secretVerification;

  @BeforeEach
  void setUp() {
    secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace("stackgres")
        .withName("test")
        .withLabels(ImmutableMap.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .endMetadata()
        .withData(ImmutableMap.of(
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("test"),
            StackGresContext.REST_PASSWORD_KEY, ResourceUtil.encodeSecret(TokenUtils.sha256("test"))))
        .build();
    secretVerification = new SecretVerification(secretScanner, new WebApiContext());
  }

  @Test
  public void login_shouldSucceedTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(secret));
    assertEquals("test", secretVerification.verifyCredentials("test", "test"));
  }

  @Test
  public void wrongPasswordLogin_shouldFailTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(secret));
    assertThrows(AuthenticationFailedException.class,
        () -> secretVerification.verifyCredentials("test", "wrong"));
  }

  @Test
  public void secretWithoutLabelsLogin_shouldFailTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(new SecretBuilder(secret)
        .editMetadata()
        .withLabels(null)
        .endMetadata()
        .build()));
    assertThrows(AuthenticationFailedException.class,
        () -> secretVerification.verifyCredentials("test", "test"));
  }

  @Test
  public void secretWithWrongLabelLogin_shouldFailTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(new SecretBuilder(secret)
        .editMetadata()
        .withLabels(ImmutableMap.of(StackGresContext.AUTH_KEY, "wrong"))
        .endMetadata()
        .build()));
    assertThrows(AuthenticationFailedException.class,
        () -> secretVerification.verifyCredentials("test", "test"));
  }

  @Test
  public void secretWithoutPasswordLogin_shouldFailTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(new SecretBuilder(secret)
        .withData(ImmutableMap.of(
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("test")))
        .build()));
    assertThrows(AuthenticationFailedException.class,
        () -> secretVerification.verifyCredentials("test", "test"));
  }

  @Test
  public void secretWithoutK8sUsernameLogin_shouldFailTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(new SecretBuilder(secret)
        .withData(ImmutableMap.of(
            StackGresContext.REST_PASSWORD_KEY, ResourceUtil.encodeSecret(TokenUtils.sha256("test"))))
        .build()));
    assertThrows(AuthenticationFailedException.class,
        () -> secretVerification.verifyCredentials("test", "test"));
  }

  @Test
  public void secretWithEmptyPasswordHashLogin_shouldFailTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(new SecretBuilder(secret)
        .withData(ImmutableMap.of(
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("test"),
            StackGresContext.REST_PASSWORD_KEY, ResourceUtil.encodeSecret("")))
        .build()));
    assertThrows(AuthenticationFailedException.class,
        () -> secretVerification.verifyCredentials("test", "test"));
  }

  @Test
  public void secretWithEmptyK8sUsernameLogin_shouldFailTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(new SecretBuilder(secret)
        .withData(ImmutableMap.of(
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret(""),
            StackGresContext.REST_PASSWORD_KEY, ResourceUtil.encodeSecret(TokenUtils.sha256("test"))))
        .build()));
    assertThrows(AuthenticationFailedException.class,
        () -> secretVerification.verifyCredentials("test", "test"));
  }

  @Test
  public void secretWithApiUsernameLogin_shouldSucceedTest() throws Exception {
    when(secretScanner.findResourcesInNamespace("stackgres")).thenReturn(ImmutableList.of(new SecretBuilder(secret)
        .withData(ImmutableMap.of(
            StackGresContext.REST_K8SUSER_KEY, ResourceUtil.encodeSecret("test2"),
            StackGresContext.REST_APIUSER_KEY, ResourceUtil.encodeSecret("test"),
            StackGresContext.REST_PASSWORD_KEY, ResourceUtil.encodeSecret(TokenUtils.sha256("test"))))
        .build()));
    assertEquals("test2", secretVerification.verifyCredentials("test", "test"));
  }

}

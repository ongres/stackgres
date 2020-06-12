
package io.stackgres.apiweb.security;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

//@QuarkusTest
public class LocalTokenTest {

  @Test
  public void generateToken() throws Exception {
    String token = TokenUtils.generateTokenString("admin", "stackgres-user", 60,
        "src/test/resources/jwt/rsa_private.key");
    assertNotNull(token);
  }

}

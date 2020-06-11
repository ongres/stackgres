
package io.stackgres.apiweb.security;

import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

//@QuarkusTest
public class LocalTokenTest {

  @Test
  public void generateToken() throws Exception {
    String token = TokenUtils.generateTokenString("admin", "stackgres-user", 60);
    assertNotNull(token);
  }

}

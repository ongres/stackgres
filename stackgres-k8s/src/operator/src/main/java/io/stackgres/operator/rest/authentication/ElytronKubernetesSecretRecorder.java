/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.authentication;

import java.security.Provider;
import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityRealm;

@Recorder
public class ElytronKubernetesSecretRecorder {
  static final Logger log = Logger.getLogger(ElytronKubernetesSecretRecorder.class);

  @SuppressWarnings("deprecation")
  private static final Provider[] PROVIDERS =
    new Provider[] { new org.wildfly.security.WildFlyElytronProvider() };

  public RuntimeValue<SecurityRealm> createRealm() throws Exception {
    log.debugf("createRealm");

    SecurityRealm realm = new KubernetesSecretSecurityRealm(new Supplier<Provider[]>() {
          @Override
          public Provider[] get() {
            return PROVIDERS;
          }
        });
    return new RuntimeValue<>(realm);
  }

}

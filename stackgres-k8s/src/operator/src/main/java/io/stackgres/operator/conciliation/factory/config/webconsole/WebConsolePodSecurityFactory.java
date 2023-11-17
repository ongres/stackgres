/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgconfig.StackGresConfigImage;
import io.stackgres.common.crd.sgconfig.StackGresConfigRestapi;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WebConsolePodSecurityFactory {

  public static final Long RESTAPI_JVM_USER = 185L;
  public static final Long RESTAPI_JVM_GROUP = 185L;
  public static final Long RESTAPI_USER = 1000L;
  public static final Long RESTAPI_GROUP = 1000L;
  public static final Long ADMINUI_USER = 998L;
  public static final Long ADMINUI_GROUP = 996L;
  public static final Long KUBECTL_USER = 1000L;
  public static final Long KUBECTL_GROUP = 1000L;

  private final OperatorPropertyContext operatorContext;

  @Inject
  public WebConsolePodSecurityFactory(OperatorPropertyContext operatorContext) {
    this.operatorContext = operatorContext;
  }

  public PodSecurityContext createRestApiPodSecurityContext(StackGresConfigContext context) {
    PodSecurityContextBuilder podSecurityContextBuilder = new PodSecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!operatorContext.getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      podSecurityContextBuilder
          .withFsGroup(
              Optional.of(context.getSource().getSpec())
              .map(StackGresConfigSpec::getRestapi)
              .map(StackGresConfigRestapi::getImage)
              .map(StackGresConfigImage::getTag)
              .or(() -> StackGresProperty.OPERATOR_IMAGE_VERSION.get())
              .map(tag -> tag.endsWith("-jvm"))
              .isPresent() ? RESTAPI_JVM_GROUP : RESTAPI_GROUP);
    }
    return podSecurityContextBuilder.build();
  }

  public PodSecurityContext createGrafanaIntegrationPodSecurityContext(
      StackGresConfigContext context) {
    PodSecurityContextBuilder podSecurityContextBuilder = new PodSecurityContextBuilder();
    if (!operatorContext.getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      podSecurityContextBuilder
          .withRunAsNonRoot(true)
          .withRunAsUser(KUBECTL_USER)
          .withRunAsGroup(KUBECTL_GROUP)
          .withFsGroup(KUBECTL_GROUP);
    }
    return podSecurityContextBuilder.build();
  }

  public SecurityContext createRestapiSecurityContext(StackGresConfigContext context) {
    SecurityContextBuilder securityContextBuilder = new SecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!operatorContext.getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      securityContextBuilder
          .withRunAsUser(
              Optional.of(context.getSource().getSpec())
              .map(StackGresConfigSpec::getRestapi)
              .map(StackGresConfigRestapi::getImage)
              .map(StackGresConfigImage::getTag)
              .or(() -> StackGresProperty.OPERATOR_IMAGE_VERSION.get())
              .map(tag -> tag.endsWith("-jvm"))
              .isPresent() ? RESTAPI_JVM_USER : RESTAPI_USER)
          .withRunAsGroup(
              Optional.of(context.getSource().getSpec())
              .map(StackGresConfigSpec::getRestapi)
              .map(StackGresConfigRestapi::getImage)
              .map(StackGresConfigImage::getTag)
              .or(() -> StackGresProperty.OPERATOR_IMAGE_VERSION.get())
              .map(tag -> tag.endsWith("-jvm"))
              .isPresent() ? RESTAPI_JVM_GROUP : RESTAPI_GROUP);
    }
    return securityContextBuilder.build();
  }

  public SecurityContext createAdminuiSecurityContext(StackGresConfigContext context) {
    SecurityContextBuilder securityContextBuilder = new SecurityContextBuilder()
        .withRunAsNonRoot(true);
    if (!operatorContext.getBoolean(OperatorProperty.USE_ARBITRARY_USER)) {
      securityContextBuilder
          .withRunAsUser(ADMINUI_USER)
          .withRunAsGroup(ADMINUI_GROUP);
    }
    return securityContextBuilder.build();
  }

}

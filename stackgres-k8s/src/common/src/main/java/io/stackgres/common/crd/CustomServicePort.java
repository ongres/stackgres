/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true,
    value = {"optional"})
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.ServicePort.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.IntOrString.class),
    })
public class CustomServicePort extends io.fabric8.kubernetes.api.model.ServicePort {

  private static final long serialVersionUID = 1L;

  public CustomServicePort() {
    super();
  }

  public CustomServicePort(String appProtocol, String name, Integer nodePort, Integer port,
      String protocol, IntOrString targetPort) {
    super(appProtocol, name, nodePort, port, protocol, targetPort);
  }

  @Override
  public String getAppProtocol() {
    return super.getAppProtocol();
  }

  @Override
  public void setAppProtocol(String appProtocol) {
    super.setAppProtocol(appProtocol);
  }

  @Override
  public String getName() {
    return super.getName();
  }

  @Override
  public void setName(String name) {
    super.setName(name);
  }

  @Override
  public Integer getNodePort() {
    return super.getNodePort();
  }

  @Override
  public void setNodePort(Integer nodePort) {
    super.setNodePort(nodePort);
  }

  @Override
  public Integer getPort() {
    return super.getPort();
  }

  @Override
  public void setPort(Integer port) {
    super.setPort(port);
  }

  @Override
  public String getProtocol() {
    return super.getProtocol();
  }

  @Override
  public void setProtocol(String protocol) {
    super.setProtocol(protocol);
  }

  @Override
  public IntOrString getTargetPort() {
    return super.getTargetPort();
  }

  @Override
  public void setTargetPort(IntOrString targetPort) {
    super.setTargetPort(targetPort);
  }

}

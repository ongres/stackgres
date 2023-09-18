/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigAdminui {

  private StackGresConfigImage image;

  private ResourceRequirements resources;

  private StackGresConfigAdminuiService service;

  public StackGresConfigImage getImage() {
    return image;
  }

  public void setImage(StackGresConfigImage image) {
    this.image = image;
  }

  public ResourceRequirements getResources() {
    return resources;
  }

  public void setResources(ResourceRequirements resources) {
    this.resources = resources;
  }

  public StackGresConfigAdminuiService getService() {
    return service;
  }

  public void setService(StackGresConfigAdminuiService service) {
    this.service = service;
  }

  @Override
  public int hashCode() {
    return Objects.hash(image, resources, service);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigAdminui)) {
      return false;
    }
    StackGresConfigAdminui other = (StackGresConfigAdminui) obj;
    return Objects.equals(image, other.image) && Objects.equals(resources, other.resources)
        && Objects.equals(service, other.service);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

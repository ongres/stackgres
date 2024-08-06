/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.model.HasMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ResourceDto {

  private String apiVersion;

  private String kind;

  private Metadata metadata = new Metadata();

  protected ResourceDto() {
    ResourceClassForDto resourceClassForDto = getClass().getAnnotation(ResourceClassForDto.class);
    if (resourceClassForDto != null) {
      this.apiVersion = HasMetadata.getApiVersion(resourceClassForDto.value());
      this.kind = HasMetadata.getKind(resourceClassForDto.value());
    }
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

}

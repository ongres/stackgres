/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgrestoreconfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.storages.PgpConfiguration;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackgresRestoreConfigSpec implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("source")
  private StackgresRestoreConfigSource source;

  @JsonProperty("compressionMethod")
  private String compressionMethod;

  @JsonProperty("downloadDiskConcurrency")
  private int downloadDiskConcurrency;

  @JsonProperty("pgpConfiguration")
  private PgpConfiguration pgpConfiguration;

  public StackgresRestoreConfigSource getSource() {
    return source;
  }

  public void setSource(StackgresRestoreConfigSource source) {
    this.source = source;
  }

  public String getCompressionMethod() {
    return compressionMethod;
  }

  public void setCompressionMethod(String compressionMethod) {
    this.compressionMethod = compressionMethod;
  }

  public int getDownloadDiskConcurrency() {
    return downloadDiskConcurrency;
  }

  public void setDownloadDiskConcurrency(int downloadDiskConcurrency) {
    this.downloadDiskConcurrency = downloadDiskConcurrency;
  }

  public PgpConfiguration getPgpConfiguration() {
    return pgpConfiguration;
  }

  public void setPgpConfiguration(PgpConfiguration pgpConfiguration) {
    this.pgpConfiguration = pgpConfiguration;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("source", getSource())
        .add("compressionMethod", compressionMethod)
        .add("downloadDiskConcurrency", downloadDiskConcurrency)
        .add("pgpConfiguration", pgpConfiguration)
        .toString();
  }
}

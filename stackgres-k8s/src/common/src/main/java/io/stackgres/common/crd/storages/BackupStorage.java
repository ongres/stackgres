/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class BackupStorage {

  @JsonProperty("type")
  @NotNull(message = "The storage type is required")
  private String type;

  @JsonProperty("s3")
  @Valid
  private AwsS3Storage s3;

  @JsonProperty("s3Compatible")
  @Valid
  private AwsS3CompatibleStorage s3Compatible;

  @JsonProperty("gcs")
  @Valid
  private GoogleCloudStorage gcs;

  @JsonProperty("azureBlob")
  @Valid
  private AzureBlobStorage azureBlob;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public AwsS3Storage getS3() {
    return s3;
  }

  public void setS3(AwsS3Storage s3) {
    this.s3 = s3;
  }

  @JsonIgnore
  public Optional<AwsS3Storage> getS3Opt() {
    return Optional.ofNullable(s3);
  }

  public AwsS3CompatibleStorage getS3Compatible() {
    return s3Compatible;
  }

  public void setS3Compatible(AwsS3CompatibleStorage s3Compatible) {
    this.s3Compatible = s3Compatible;
  }

  @JsonIgnore
  public Optional<AwsS3CompatibleStorage> getS3CompatibleOpt() {
    return Optional.ofNullable(s3Compatible);
  }

  public GoogleCloudStorage getGcs() {
    return gcs;
  }

  public void setGcs(GoogleCloudStorage gcs) {
    this.gcs = gcs;
  }

  @JsonIgnore
  public Optional<GoogleCloudStorage> getGcsOpt() {
    return Optional.ofNullable(gcs);
  }

  public AzureBlobStorage getAzureBlob() {
    return azureBlob;
  }

  public void setAzureBlob(AzureBlobStorage azureBlob) {
    this.azureBlob = azureBlob;
  }

  @JsonIgnore
  public Optional<AzureBlobStorage> getAzureBlobOpt() {
    return Optional.ofNullable(azureBlob);
  }

  @Override
  public int hashCode() {
    return Objects.hash(azureBlob, gcs, s3, s3Compatible, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BackupStorage)) {
      return false;
    }
    BackupStorage other = (BackupStorage) obj;
    return Objects.equals(azureBlob, other.azureBlob) && Objects.equals(gcs, other.gcs)
        && Objects.equals(s3, other.s3) && Objects.equals(s3Compatible, other.s3Compatible)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

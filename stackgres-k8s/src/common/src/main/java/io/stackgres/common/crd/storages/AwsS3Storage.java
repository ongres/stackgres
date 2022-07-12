/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class AwsS3Storage implements PrefixedStorage {

  @JsonProperty("bucket")
  @NotNull(message = "The bucket is required")
  private String bucket;

  @JsonProperty("path")
  @Deprecated(forRemoval = true)
  private String path;

  @JsonProperty("awsCredentials")
  @NotNull(message = "The credentials is required")
  @Valid
  private AwsCredentials awsCredentials;

  @JsonProperty("region")
  private String region;

  @JsonProperty("storageClass")
  @ValidEnum(enumClass = StorageClassS3.class, allowNulls = true,
      message = "storageClass must be one of STANDARD, STANDARD_IA or REDUCED_REDUNDANCY.")
  private String storageClass;

  @Override
  public String getSchema() {
    return "s3";
  }

  @Override
  public String getBucket() {
    return bucket;
  }

  @Override
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  @Override
  @Deprecated(forRemoval = true)
  public String getPath() {
    return path;
  }

  @Override
  @Deprecated(forRemoval = true)
  public void setPath(String path) {
    this.path = path;
  }

  public AwsCredentials getAwsCredentials() {
    return awsCredentials;
  }

  public void setAwsCredentials(AwsCredentials awsCredentials) {
    this.awsCredentials = awsCredentials;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  @Override
  public int hashCode() {
    return Objects.hash(awsCredentials, bucket, path, region, storageClass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AwsS3Storage)) {
      return false;
    }
    AwsS3Storage other = (AwsS3Storage) obj;
    return Objects.equals(awsCredentials, other.awsCredentials)
        && Objects.equals(bucket, other.bucket) && Objects.equals(path, other.path)
        && Objects.equals(region, other.region) && Objects.equals(storageClass, other.storageClass);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

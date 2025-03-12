/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StorageEncryption {

  @ValidEnum(enumClass = StorageEncryptionMethod.class, allowNulls = true,
      message = "mode can be sodium or openpgp")
  private String method;

  @Valid
  private SodiumStorageEncryption sodium;

  @Valid
  private OpenPgpStorageEncryption openpgp;

  @ReferencedField("sodium")
  interface Sodium extends FieldReference { }

  @ReferencedField("openpgp")
  interface Openpgp extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "sodium section is not specified.",
      payload = { Sodium.class })
  public boolean isSodiumRequired() {
    return method == null || !method.equals(StorageEncryptionMethod.SODIUM.toString()) || sodium != null;
  }

  @JsonIgnore
  @AssertTrue(message = "openpgp section is not specified.",
      payload = { Openpgp.class })
  public boolean isOpenpgpRequired() {
    return method == null || !method.equals(StorageEncryptionMethod.OPENPGP.toString()) || openpgp != null;
  }

  @JsonIgnore
  @AssertTrue(message = "sodium and openpgp sections are mutually exclusive.",
      payload = { Openpgp.class, Sodium.class })
  public boolean isEncryptionMethodSectionsMutuallyExclusive() {
    return method == null
        || (sodium == null && openpgp == null)
        || (sodium == null && openpgp != null)
        || (sodium != null && openpgp == null);
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public SodiumStorageEncryption getSodium() {
    return sodium;
  }

  public void setSodium(SodiumStorageEncryption sodium) {
    this.sodium = sodium;
  }

  public OpenPgpStorageEncryption getOpenpgp() {
    return openpgp;
  }

  public void setOpenpgp(OpenPgpStorageEncryption openpgp) {
    this.openpgp = openpgp;
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, openpgp, sodium);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StorageEncryption)) {
      return false;
    }
    StorageEncryption other = (StorageEncryption) obj;
    return Objects.equals(method, other.method) && Objects.equals(openpgp, other.openpgp)
        && Objects.equals(sodium, other.sodium);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

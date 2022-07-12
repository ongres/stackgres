/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true,
    value = {"optional"})
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class SecretKeySelector extends io.fabric8.kubernetes.api.model.SecretKeySelector {

  private static final long serialVersionUID = 1L;

  public SecretKeySelector() {
    super();
  }

  public SecretKeySelector(String key, String name) {
    super(key, name, false);
  }

  @ReferencedField("key")
  interface Key extends FieldReference { }

  @ReferencedField("name")
  interface Name extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "key must not be empty.",
      payload = Key.class)
  public boolean isKeyNotEmpty() {
    return getKey() != null && !getKey().isEmpty();
  }

  @JsonIgnore
  @AssertTrue(message = "name must not be empty.",
      payload = Name.class)
  public boolean isNameNotEmpty() {
    return getName() != null && !getName().isEmpty();
  }

}

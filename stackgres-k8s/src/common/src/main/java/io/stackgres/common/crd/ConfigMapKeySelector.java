/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true,
    value = {"optional"})
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class ConfigMapKeySelector extends io.fabric8.kubernetes.api.model.ConfigMapKeySelector {

  private static final long serialVersionUID = 1L;

  public ConfigMapKeySelector() {
    super();
  }

  public ConfigMapKeySelector(String key, String name) {
    super(key, name, false);
  }

}

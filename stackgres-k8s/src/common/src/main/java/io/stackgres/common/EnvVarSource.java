/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.function.Function;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;

public interface EnvVarSource<R extends HasMetadata, C extends EnvVarContext<R>> {

  String substVar();

  Function<R, EnvVar> getEnvVar();

  String name();

  default String getSubstVar() {
    return "$(" + name() + ")";
  }

  default String value(C context) {
    return value(context.getResource());
  }

  default String value(R context) {
    return getEnvVar().apply(context).getValue();
  }

  default String value() {
    return getEnvVar().apply(null).getValue();
  }

  default EnvVar envVar(C context) {
    return envVar(context.getResource());
  }

  default EnvVar envVar(R context) {
    return getEnvVar().apply(context);
  }

  default EnvVar envVar() {
    return getEnvVar().apply(null);
  }
}

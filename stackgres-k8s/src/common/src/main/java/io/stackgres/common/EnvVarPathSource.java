/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;

public interface EnvVarPathSource<R extends HasMetadata> extends VolumePath<EnvVarContext<R>> {

  String name();

  String rawPath();

  @Override
  default String path() {
    return path(Map.of());
  }

  @Override
  default String path(EnvVarContext<R> context) {
    return path(context.getEnvironmentVariables());
  }

  @Override
  default String path(EnvVarContext<R> context, Map<String, String> envVars) {
    return path(envVars(context, envVars));
  }

  @Override
  default String path(Map<String, String> envVars) {
    StringBuilder path = new StringBuilder();
    int startIndexOf = rawPath().indexOf("$(");
    int endIndexOf = -1;
    while (startIndexOf >= 0) {
      path.append(rawPath(), endIndexOf + 1, startIndexOf);
      endIndexOf = rawPath().indexOf(")", startIndexOf);
      if (endIndexOf == -1) {
        throw new IllegalArgumentException(
            "Path " + rawPath() + " do not close variable substitution."
                + " Expected a `)` character after position " + startIndexOf);
      }
      String variable = rawPath().substring(startIndexOf + 2, endIndexOf);
      String value = envVars.get(variable);
      if (value == null) {
        throw new IllegalArgumentException(
            "Path " + rawPath() + " specify variable " + variable
            + " for substitution. But was not found in map " + envVars);
      }
      path.append(value);
      startIndexOf = rawPath().indexOf("$(", endIndexOf + 1);
    }
    if (endIndexOf == -1) {
      return rawPath();
    }
    if (endIndexOf < rawPath().length()) {
      path.append(rawPath(), endIndexOf + 1, rawPath().length());
    }
    return path.toString();
  }

  @Override
  default String filename() {
    return filename(Map.of());
  }

  @Override
  default String filename(EnvVarContext<R> context) {
    return filename(context.getEnvironmentVariables());
  }

  @Override
  default String filename(EnvVarContext<R> context, Map<String, String> envVars) {
    return filename(envVars(context, envVars));
  }

  @Override
  default String filename(Map<String, String> envVars) {
    String pathFile = path(envVars);
    int indexOfLastSlash = pathFile.lastIndexOf('/');
    return indexOfLastSlash != -1 ? pathFile.substring(indexOfLastSlash + 1) : pathFile;
  }

  @Override
  default String subPath() {
    return subPath(Map.of());
  }

  @Override
  default String subPath(EnvVarContext<R> context) {
    return subPath(context.getEnvironmentVariables());
  }

  @Override
  default String subPath(EnvVarContext<R> context, Map<String, String> envVars) {
    return subPath(envVars(context, envVars));
  }

  @Override
  default String subPath(Map<String, String> envVars) {
    return path(envVars).substring(1);
  }

  @Override
  default String subPath(VolumePath<EnvVarContext<R>> relativeTo) {
    return relativize(subPath(Map.of()), relativeTo.subPath(Map.of()));
  }

  @Override
  default String subPath(EnvVarContext<R> context,
      VolumePath<EnvVarContext<R>> relativeTo) {
    return relativize(subPath(context.getEnvironmentVariables()),
        relativeTo.subPath(context.getEnvironmentVariables()));
  }

  @Override
  default String subPath(EnvVarContext<R> context, Map<String, String> envVars,
      VolumePath<EnvVarContext<R>> relativeTo) {
    return relativize(subPath(envVars(context, envVars)),
        relativeTo.subPath(envVars(context, envVars)));
  }

  @Override
  default String subPath(Map<String, String> envVars,
      VolumePath<EnvVarContext<R>> relativeTo) {
    return relativize(subPath(envVars), relativeTo.subPath(envVars));
  }

  @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "False positive")
  private String relativize(String subPath, String relativeToSubPath) {
    Preconditions.checkArgument(subPath.startsWith(relativeToSubPath + "/"),
        subPath + " is not relative to " + relativeToSubPath + "/");
    return subPath.substring(relativeToSubPath.length() + 1);
  }

  default EnvVar envVar() {
    return envVar(Map.of());
  }

  default EnvVar envVar(EnvVarContext<R> context) {
    return envVar(context.getEnvironmentVariables());
  }

  default EnvVar envVar(EnvVarContext<R> context, Map<String, String> envVars) {
    return envVar(envVars(context, envVars));
  }

  default EnvVar envVar(Map<String, String> envVars) {
    return new EnvVarBuilder()
        .withName(name())
        .withValue(path(envVars))
        .build();
  }

  @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
      justification = "False positive")
  private Map<String, String> envVars(EnvVarContext<R> context, Map<String, String> envVars) {
    Map<String, String> mergedEnvVars = new HashMap<>(context.getEnvironmentVariables());
    mergedEnvVars.putAll(envVars);
    return Map.copyOf(mergedEnvVars);
  }

}

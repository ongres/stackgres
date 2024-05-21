/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.crd.sgstream.StreamStatusCondition.Status;
import io.stackgres.common.crd.sgstream.StreamStatusCondition.Type;
import io.stackgres.operatorframework.resource.ResourceUtil;

public interface StreamUtil {

  Pattern UPPERCASE_LETTER_PATTERN = Pattern.compile("([A-Z])");

  String SUFFIX = "-stream";

  static String roleName(StackGresCluster cluster) {
    return roleName(cluster.getMetadata().getName());
  }

  static String roleName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + SUFFIX);
  }

  static boolean isAlreadyCompleted(StackGresStream stream) {
    return Optional.of(stream)
        .map(StackGresStream::getStatus)
        .map(StackGresStreamStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Status.TRUE.getStatus().equals(condition.getStatus()))
        .anyMatch(condition -> Type.COMPLETED.getType().equals(condition.getType())
            || Type.FAILED.getType().equals(condition.getType()));
  }

  static String jobName(StackGresStream stream) {
    String name = stream.getMetadata().getName();
    return ResourceUtil.resourceName(name);
  }

}

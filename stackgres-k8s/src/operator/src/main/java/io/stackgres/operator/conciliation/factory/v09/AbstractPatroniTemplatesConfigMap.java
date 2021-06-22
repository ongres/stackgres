/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.v09;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractPatroniTemplatesConfigMap<T> implements VolumeFactory<T> {

  private static final String TEMPLATES_SUFFIX = "-templates";

  private static final String TEMPLATES_PATH = "/templates/v09/";

  public static String name(CustomResource<?, ?> resource) {
    return ResourceUtil.resourceName(resource.getMetadata().getName() + TEMPLATES_SUFFIX);
  }

  protected Map<String, String> getPatroniTemplates() {

    String[] templates = {
        "create-backup.sh",
        "exec-with-env",
        "group",
        "gshadow",
        "passwd",
        "post-init.sh",
        "setup-arbitrary-user.sh",
        "setup-data-paths.sh",
        "setup-scripts.sh",
        "shadow",
        "shell-utils",
        "start-patroni.sh",
        "start-patroni-with-restore.sh"
    };

    return Arrays.stream(templates)
        .map(template -> Tuple.tuple(template, TEMPLATES_PATH + template))
        .map(tuple -> tuple.map2(AbstractPatroniTemplatesConfigMap.class::getResource))
        .map(tuple -> tuple.map2(url -> Resources.asCharSource(url, StandardCharsets.UTF_8)))
        .map(tuple -> tuple.map2(charSource -> {
          try {
            return charSource.read();
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        })).collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));

  }

}

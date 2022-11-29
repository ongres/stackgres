/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jooq.lambda.Unchecked;

public class CrdUtils {

  private CrdUtils() {
    // Utility classes should not have public constructors (java:S1118)
    throw new IllegalStateException("Utility class");
  }

  public static File getCrdsFolder() {
    String projectPath = new File(new File("src").getAbsolutePath())
        .getParentFile().getParentFile().getParentFile().getAbsolutePath();

    return new File(projectPath + "/src/common/src/main/resources/crds");
  }

  public static void installCrds(KubernetesClient client) throws IOException {
    YAMLMapper yamlMapper = new YAMLMapper();
    Files.list(Paths.get("../../src/common/src/main/resources/crds"))
        .filter(path -> Optional.ofNullable(path.getFileName())
            .map(name -> name.endsWith(".yaml")).orElse(false))
        .forEach(Unchecked.consumer(path -> client.apiextensions().v1().customResourceDefinitions()
            .resource(yamlMapper.readValue(path.toFile(), CustomResourceDefinition.class))
            .create()));
  }
}

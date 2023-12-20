/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.rbac.Role;
import org.jooq.lambda.Seq;

public final class GeneratorTestUtil {

  private GeneratorTestUtil() {
    throw new IllegalStateException("Should not be instantiated");
  }

  public static void assertResourceEquals(HasMetadata expected, HasMetadata actual) {

    if (expected.getMetadata() != null) {
      assertNotNull(actual.getMetadata(), "Metadata object was expected but not generated");
      assertResourceEquals(expected.getMetadata(), actual.getMetadata());
    } else {
      assertNull(actual, "Metadata object was not expected but was generated");
    }
  }

  public static void assertResourceEquals(ObjectMeta expected, ObjectMeta actual) {
    assertEquals(
        expected.getName(),
        actual.getName(),
        "Resource names don't match"
    );
    assertEquals(
        expected.getNamespace(),
        actual.getNamespace(),
        "Resource namespaces don't match"
    );

    assertEquals(
        Map.copyOf(expected.getAnnotations()),
        Map.copyOf(actual.getAnnotations()),
        "Resource annotations don't match"
    );

    assertEquals(
        Map.copyOf(expected.getLabels()),
        Map.copyOf(actual.getLabels()),
        "Resource labels don't match"
    );
  }

  public static void assertResourceEquals(Service expected, Service actual) {
    assertResourceEquals((HasMetadata) expected, actual);
    if (expected.getSpec() != null) {
      assertNotNull(actual.getSpec(), "An spec was expected but was not generated");
      if (expected.getSpec().getPorts() != null) {
        assertNotNull(actual.getSpec().getPorts(),
            "ports were expected but were not generated");
        assertEquals(
            List.copyOf(expected.getSpec().getPorts()),
            List.copyOf(actual.getSpec().getPorts()),
            "Service ports don't match"
        );
      }
      if (expected.getSpec().getSelector() != null) {
        assertNotNull(actual.getSpec().getSelector(),
            "Service selector was expected but not generated");
        assertEquals(
            Map.copyOf(expected.getSpec().getSelector()),
            Map.copyOf(actual.getSpec().getSelector()),
            "Service ports don't match"
        );
      } else if (actual.getSpec().getSelector() != null) {
        fail("Service selector was not expected but was generated");
      }
      assertEquals(expected.getSpec().getType(), actual.getSpec().getType());
      assertEquals(expected.getSpec().getClusterIP(), actual.getSpec().getClusterIP());
      assertEquals(expected.getSpec().getExternalName(), actual.getSpec().getExternalName());
      assertEquals(expected.getSpec().getExternalTrafficPolicy(),
          actual.getSpec().getExternalTrafficPolicy());
      assertEquals(expected.getSpec().getHealthCheckNodePort(),
          actual.getSpec().getHealthCheckNodePort());
      assertEquals(expected.getSpec().getSessionAffinity(), actual.getSpec().getSessionAffinity());

    } else if (actual.getSpec() != null) {
      fail("An spec was not expected but was generated");
    }
    assertEquals(
        expected.getSpec(),
        actual.getSpec(),
        "Service spec don't match"
    );
  }

  public static void assertResourceEquals(Role expected, Role actual) {
    assertResourceEquals((HasMetadata) expected, actual);
    assertEquals(
        List.copyOf(expected.getRules()),
        List.copyOf(actual.getRules()),
        "Role rules don't match"
    );
  }

  public static void assertResourceEquals(Secret expected, Secret actual) {
    assertResourceEquals((HasMetadata) expected, actual);
    if (expected.getData() != null) {
      assertNotNull(actual.getData(), "Secret data was expected but was not generated");
      assertEquals(
          Map.copyOf(expected.getData()),
          Map.copyOf(actual.getData()),
          "Secret data don't match"
      );
    } else {
      assertNull(actual.getData(), "Secret data was not expected but was generated");
    }

    if (expected.getStringData() != null) {
      assertNotNull(actual.getStringData(),
          "Secret string data was expected but was not generated");
      assertEquals(
          Map.copyOf(expected.getStringData()),
          Map.copyOf(actual.getStringData()),
          "Secret data don't match"
      );
    } else {
      assertNull(actual.getStringData(),
          "Secret string data was not expected but was generated");
    }
    assertEquals(expected.getType(), actual.getType());
  }

  public static void assertResourceEquals(ConfigMap expected, ConfigMap actual) {
    assertResourceEquals((HasMetadata) expected, actual);
    if (expected.getData() != null) {
      assertNotNull(actual.getData(), "ConfigMap data was expected but was not generated");
      assertEquals(
          Map.copyOf(expected.getData()),
          Map.copyOf(actual.getData()),
          "Config Map data don't match"
      );
    } else {
      assertNull(actual.getData(), "ConfigMap data was not expected but was generated");
    }
    if (expected.getBinaryData() != null) {
      assertNotNull(actual.getBinaryData(),
          "ConfigMap binary data was expected but not generated");
      assertEquals(
          Map.copyOf(expected.getBinaryData()),
          Map.copyOf(actual.getBinaryData()),
          "Config Map binary data don't match"
      );
    } else {
      assertNull(actual.getBinaryData(),
          "ConfigMap binary data was not expected but was generated");
    }

    assertEquals(expected.getImmutable(), actual.getImmutable());
  }

  public static void assertResourceEquals(StatefulSet expected, StatefulSet actual) {
    assertResourceEquals((HasMetadata) expected, actual);

    if (expected.getSpec() != null) {
      assertNotNull(actual.getSpec(), "StatefulSet spec was expected but was not generated");
      assertResourceEquals(expected.getSpec(), actual.getSpec());

    } else {
      assertNull(actual.getSpec(), "StatefulSet spec was not expected but was generated");
    }
  }

  public static void assertResourceEquals(StatefulSetSpec expected, StatefulSetSpec actual) {
    assertEquals(expected.getReplicas(), actual.getReplicas());
    assertEquals(expected.getPodManagementPolicy(), actual.getPodManagementPolicy());
    assertEquals(expected.getRevisionHistoryLimit(), actual.getRevisionHistoryLimit());
    assertEquals(expected.getServiceName(), actual.getServiceName());
    assertEquals(expected.getUpdateStrategy(), actual.getUpdateStrategy());

    if (expected.getSelector() != null) {
      assertNotNull(actual.getSelector(),
          "StatefulSet spec selector was expected but was not generated");
      assertResourceEquals(expected.getSelector(), actual.getSelector());
    } else {
      assertNull(actual.getSelector(),
          "StatefulSet spec selector was not expected but was generated");
    }

    if (expected.getTemplate() != null) {
      assertNotNull(actual.getTemplate(),
          "StatefulSet spec template was expected but was not generated");
      assertResourceEquals(expected.getTemplate(), actual.getTemplate());
    } else {
      assertNull(actual.getTemplate(),
          "StatefulSet spec template was not expected but was generated");
    }
  }

  public static void assertResourceEquals(LabelSelector expected, LabelSelector actual) {
    if (expected.getMatchLabels() != null) {
      assertNotNull(actual.getMatchLabels(),
          "Label selector matchLabels was expected but was not generated");
      assertEquals(
          Map.copyOf(expected.getMatchLabels()),
          Map.copyOf(actual.getMatchLabels()),
          "Label selector match labels don't match"
      );
    } else {
      assertNull(actual.getMatchLabels(),
          "Label selector matchLabels was not expected but was generated");
    }

    if (expected.getMatchExpressions() != null) {
      assertNotNull(actual.getMatchExpressions(),
          "Label selector matchExpressions was expected but was not generated");
      assertEquals(
          List.copyOf(expected.getMatchExpressions()),
          List.copyOf(actual.getMatchExpressions()),
          "Label selector matchExpressions don't match"
      );
    } else {
      assertNull(actual.getMatchExpressions(),
          "Label selector matchExpressions was not expected but was generated");
    }
  }

  public static void assertResourceEquals(PodTemplateSpec expected, PodTemplateSpec actual) {
    assertResourceEquals(expected.getMetadata(), actual.getMetadata());

    if (expected.getSpec() != null) {
      assertNotNull(actual.getSpec(), "PodSpec was expected but was not generated");
      assertResourceEquals(expected.getSpec(), actual.getSpec());
    } else {
      assertNull(actual.getSpec(), "PodSpec was not expected but was generated");
    }
  }

  public static void assertResourceEquals(PodSpec expected, PodSpec actual) {

    assertEquals(expected.getDnsPolicy(), actual.getDnsPolicy(),
        "PodSpec DNS policy don't match");
    assertEquals(expected.getHostname(), actual.getHostname(),
        "PodSpec hostname don't match");
    assertEquals(expected.getServiceAccountName(), actual.getServiceAccountName(),
        "Pod spec service account name don't match");
    assertEquals(expected.getServiceAccount(), actual.getServiceAccount(),
        "PodSpec service account don't match");

    assertEquals(expected.getActiveDeadlineSeconds(), actual.getActiveDeadlineSeconds(),
        "PodSpec active deadline seconds don't match");

    assertEquals(expected.getDnsConfig(), actual.getDnsConfig(),
        "PodSpec DNS config don't match");

    if (expected.getNodeSelector() != null) {
      assertNotNull(actual.getNodeSelector(),
          "PodSpec node selector was expected but not generated");
      assertEquals(
          Map.copyOf(expected.getNodeSelector()),
          Map.copyOf(actual.getNodeSelector()),
          "PodSpec node selector don't match"
      );
    } else {
      assertNull(actual.getNodeSelector(),
          "PodSpec node selector was not expected but was generated");
    }

    if (expected.getAffinity() != null) {
      assertNotNull(actual.getAffinity(),
          "PodSpec affinity was expected but was not generated");
      assertEquals(expected.getAffinity(), actual.getAffinity());
    } else {
      assertNull(actual.getAffinity(),
          "PodSpec affinity was not expected but was generated");
    }

    if (expected.getContainers() != null) {
      assertNotNull(actual.getContainers(),
          "PodSpec containers was expected but was not generated");
      assertEquals(expected.getContainers().size(), actual.getContainers().size(),
          "Expected and generated containers don't match");

      Seq.zip(List.copyOf(expected.getContainers()), List.copyOf(actual.getContainers()))
          .collect(Collectors.toList()).forEach(tuple -> {
            var expectedContainer = tuple.v1;
            var actualContainer = tuple.v2;
            assertResourceEquals(expectedContainer, actualContainer);
          });

    } else {
      assertNull(actual.getContainers(),
          "PodSpec containers was not expected but was generated");
    }

    if (expected.getInitContainers() != null) {
      assertNotNull(actual.getInitContainers(),
          "PodSpec init container was expected but was not generated");
      assertEquals(expected.getInitContainers().size(), actual.getInitContainers().size(),
          "The number of init containers don't match");
      Seq.zip(List.copyOf(expected.getInitContainers()), List.copyOf(actual.getInitContainers()))
          .collect(Collectors.toList()).forEach(tuple -> {
            var expectedContainer = tuple.v1;
            var actualContainer = tuple.v2;
            assertResourceEquals(expectedContainer, actualContainer);
          });
    } else {
      assertNull(actual.getInitContainers(),
          "PodSpec init containers was not expected but was generated");
    }

    if (expected.getVolumes() != null) {
      assertNotNull(actual.getVolumes(),
          "PodSpec volumes were expected but were not generated");

      assertEquals(expected.getVolumes().size(), actual.getVolumes().size(),
          "The Number of volumes in the PodSpec don't match");
      List<String> expectedVolumeNames = expected.getVolumes().stream()
          .sorted(Comparator.comparing(Volume::getName))
          .map(Volume::getName)
          .collect(Collectors.toUnmodifiableList());

      List<String> actualVolumeNames = actual.getVolumes().stream()
          .sorted(Comparator.comparing(Volume::getName))
          .map(Volume::getName)
          .collect(Collectors.toUnmodifiableList());

      assertEquals(expectedVolumeNames, actualVolumeNames);

      Seq.zip(
              expected.getVolumes().stream()
                  .sorted(Comparator.comparing(Volume::getName))
                  .collect(Collectors.toUnmodifiableList()),
              actual.getVolumes().stream()
                  .sorted(Comparator.comparing(Volume::getName))
                  .collect(Collectors.toUnmodifiableList())
          )
          .collect(Collectors.toList())
          .forEach(tuple -> assertEquals(tuple.v1, tuple.v2, "PodSpec volume don't match"));
    } else {
      assertNull(actual.getVolumes(), "PodSpec volumes were not expected but were generated");
    }

    if (expected.getSecurityContext() != null) {
      assertNotNull(actual.getSecurityContext(),
          "PodSpec security context was expected but not generated");
      assertEquals(expected.getSecurityContext(), actual.getSecurityContext(),
          "PodSpec security context don't match");
    } else {
      assertNull(actual.getSecurityContext(),
          "PodSpec security context was not expected but was generated");
    }

    assertEquals(expected.getShareProcessNamespace(), actual.getShareProcessNamespace(),
        "PodSpec share process namespace don't match");

    assertEquals(expected.getTerminationGracePeriodSeconds(),
        actual.getTerminationGracePeriodSeconds(),
        "PodSpec termination grace period seconds don't match");
  }

  public static void assertResourceEquals(Container expected, Container actual) {
    final String containerName = "Container " + expected.getName();
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getImage(), actual.getImage());

    if (expected.getResources() != null) {
      assertNotNull(actual.getResources(), containerName
          + " resource requirements was expected but was not generated");
      assertEquals(expected.getResources(), actual.getResources());
    } else {
      assertNull(actual.getResources(), containerName
          + " resource requirements was not expected but was generated");
    }

    assertEquals(expected.getArgs(), actual.getArgs(), containerName
        + " arguments don't match");

    assertEquals(expected.getCommand(), actual.getCommand(), containerName
        + " command don't match");

    assertEquals(expected.getImagePullPolicy(), actual.getImagePullPolicy(), containerName
        + " image pull policy don't match");

    assertEquals(expected.getWorkingDir(), actual.getWorkingDir(), containerName
        + " working dir don't match");

    if (expected.getVolumeMounts() != null) {
      assertNotNull(actual.getVolumeMounts(), containerName
          + " volume mounts was expected but was not generated");
      Seq.zip(List.copyOf(expected.getVolumeMounts()), List.copyOf(actual.getVolumeMounts()))
          .collect(Collectors.toList())
          .forEach(tuple -> assertEquals(
              tuple.v1,
              tuple.v2,
              "Volume mounts of " + containerName + " don't match"));
    } else {
      assertNull(actual.getVolumeMounts(),
          "Container volume mounts was not expected but was generated");
    }

    if (expected.getEnv() != null) {
      assertNotNull(actual.getEnv(), containerName
          + " environments variables was expected but not generated");

      assertEquals(expected.getEnv().size(), actual.getEnv().size(),
          "The number of environment variables of " + containerName + " don't match");
      Seq.zip(List.copyOf(expected.getEnv()), List.copyOf(actual.getEnv()))
          .collect(Collectors.toList())
          .forEach(tuple -> assertEquals(tuple.v1, tuple.v2, containerName
              + " environment variable don't match"));

    } else {
      assertNull(actual.getEnv(), "Container " + containerName
          + " was not expected but was generated");
    }

    if (expected.getEnvFrom() != null) {
      assertNotNull(actual.getEnvFrom(), containerName
          + " envFrom was expected but not generated");
      assertEquals(expected.getEnvFrom().size(), actual.getEnvFrom().size(),
          "The number of envFrom of " + containerName + " don't match");
      Seq.zip(List.copyOf(expected.getEnvFrom()), List.copyOf(actual.getEnvFrom()))
          .collect(Collectors.toList())
          .forEach(tuple -> assertEquals(tuple.v1, tuple.v2, containerName
              + " envFrom don't match"));
    } else {
      assertNull(actual.getEnvFrom(), containerName
          + " envFrom was not expected but not generated");
    }

    assertEquals(expected.getStdin(), actual.getStdin(),
        containerName + " Stdin don't match");

    assertEquals(expected.getStdinOnce(), actual.getStdinOnce(),
        containerName + " StdinOnce don't match");

    assertEquals(expected.getTerminationMessagePath(), actual.getTerminationMessagePath(),
        containerName + " termination message patch don't match");

    assertEquals(expected.getTerminationMessagePolicy(), actual.getTerminationMessagePolicy(),
        containerName + " termination message policy don't match");

    assertEquals(expected.getTty(), actual.getTty(), containerName + " tty don't match");

    assertEquals(expected.getSecurityContext(), actual.getSecurityContext(), containerName
        + " security context don't match");

    assertEquals(expected.getLifecycle(), actual.getLifecycle(),
        containerName + " lifecycle don't match");

    assertEquals(expected.getLivenessProbe(), actual.getLivenessProbe(),
        containerName + " liveness probe don't match");

    assertEquals(expected.getReadinessProbe(), actual.getReadinessProbe(),
        containerName + " readiness probe don't match");

    assertEquals(expected.getStartupProbe(), actual.getStartupProbe(),
        containerName + " startup probe don't match");

    if (expected.getPorts() != null) {
      assertNotNull(actual.getPorts(),
          containerName + " ports was expected but not generated");
      assertEquals(
          List.copyOf(expected.getPorts()),
          List.copyOf(actual.getPorts()),
          containerName + " ports don't match"
      );
    } else {
      assertNull(actual.getPorts(), containerName
          + " ports was not expected but was generated");
    }

    if (expected.getVolumeDevices() != null) {
      assertNotNull(expected.getVolumeDevices(),
          containerName + " volume devices was expected but not generated");
      assertEquals(
          List.copyOf(expected.getVolumeDevices()),
          List.copyOf(actual.getVolumeDevices()),
          containerName + " volume devices don't match"
      );
    } else {
      assertNull(actual.getVolumeDevices(),
          containerName + " volume devices was not expected bet was generated");
    }
  }

}

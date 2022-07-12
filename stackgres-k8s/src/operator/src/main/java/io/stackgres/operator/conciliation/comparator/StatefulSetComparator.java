/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.regex.Pattern;

public class StatefulSetComparator extends AbstractComparator {

  private static final IgnorePatch[] IGNORE_PATTERS = {
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/env/\\d+/valueFrom/fieldRef/apiVersion"),
          "add",
          "v1"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/livenessProbe/successThreshold"),
          "add",
          "1"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/livenessProbe/timeoutSeconds"),
          "add",
          "1"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/livenessProbe/failureThreshold"),
          "add",
          "3"),
      new PatchPattern(Pattern.compile("/spec/template/spec/containers/\\d+/ports/\\d+/protocol"),
          "add",
          "TCP"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/readinessProbe/failureThreshold"),
          "add",
          "3"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/readinessProbe/successThreshold"),
          "add",
          "1"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/readinessProbe/timeoutSeconds"),
          "add",
          "1"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/terminationMessagePath"),
          "add",
          "/dev/termination-log"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/terminationMessagePolicy"),
          "add",
          "File"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/resources"),
          "add",
          "{}"),
      new PatchPattern(Pattern.compile("/spec/template/spec/initContainers/\\d+/imagePullPolicy"),
          "add",
          "IfNotPresent"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/initContainers/\\d+/resources"),
          "add",
          "{}"),
      new PatchPattern(Pattern.compile(
          "/spec/template/spec/initContainers/\\d+/env/\\d+/valueFrom/fieldRef/apiVersion"),
          "add",
          "v1"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/initContainers/\\d+/terminationMessagePath"),
          "add",
          "/dev/termination-log"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/initContainers/\\d+/terminationMessagePolicy"),
          "add",
          "File"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/initContainers/\\d+/volumeMounts/\\d+/readOnly"),
          "remove"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/containers/\\d+/volumeMounts/\\d+/readOnly"),
          "remove"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/volumes/\\d+/configMap/defaultMode"),
          "add",
          "420"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/volumes/\\d+/secret/defaultMode"),
          "add",
          "420"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/volumes/\\d+$"),
          "move"),
      new PatchPattern(Pattern
          .compile("/spec/template/spec/volumes/\\d+/emptyDir"),
          "remove"),
      new SimpleIgnorePatch("/spec/template/spec/dnsPolicy",
          "add",
          "ClusterFirst"),
      new SimpleIgnorePatch("/spec/template/spec/restartPolicy",
          "add",
          "Always"),
      new SimpleIgnorePatch("/spec/template/spec/schedulerName",
          "add",
          "default-scheduler"),
      new SimpleIgnorePatch("/spec/template/spec/serviceAccount",
          "add"),
      new PatchPattern(Pattern.compile("/spec/volumeClaimTemplates/\\d+/spec/volumeMode"),
          "add",
          "Filesystem"),
      new PatchPattern(Pattern.compile("/spec/volumeClaimTemplates/\\d+/status"),
          "add"),
      new PatchPattern(Pattern.compile("/spec/volumeClaimTemplates/\\d+/metadata/annotations"),
          "remove"),
      new PatchPattern(Pattern.compile("/spec/volumeClaimTemplates/\\d+/metadata/annotations/.+"),
          "remove"),
      new PatchPattern(Pattern.compile("/spec/volumeClaimTemplates/\\d+/metadata/annotations/.+"),
          "add"),
      new PatchPattern(Pattern.compile("/spec/volumeClaimTemplates/\\d+/metadata/annotations/.+"),
          "replace"),
      new PatchPattern(Pattern
          .compile("/spec/volumeClaimTemplates/\\d+/metadata/ownerReferences/\\d+/apiVersion"),
          "replace"),
      new SimpleIgnorePatch("/spec/podManagementPolicy",
          "add",
          "OrderedReady"),
      new SimpleIgnorePatch("/spec/revisionHistoryLimit",
          "add",
          "10"),
      new SimpleIgnorePatch("/status",
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATTERS;
  }

}

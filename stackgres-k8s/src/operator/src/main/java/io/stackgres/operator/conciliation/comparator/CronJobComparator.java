/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.regex.Pattern;

public class CronJobComparator extends AbstractComparator {

  private static final IgnorePatch[] IGNORE_PATTERS = {
      new PatchPattern(Pattern
          .compile("/spec/jobTemplate/spec/template/spec/containers/\\d+/"
              + "env/\\d+/valueFrom/fieldRef/apiVersion"),
          "add",
          "v1"),
      new PatchPattern(Pattern
          .compile("/spec/jobTemplate/spec/template/spec/containers/\\d+/resources"),
          "add"),
      new PatchPattern(Pattern
          .compile("/spec/jobTemplate/spec/template/spec/containers/\\d+/terminationMessagePath"),
          "add",
          "/dev/termination-log"),
      new PatchPattern(Pattern
          .compile("/spec/jobTemplate/spec/template/spec/containers/\\d+/terminationMessagePolicy"),
          "add",
          "File"),
      new SimpleIgnorePatch("/spec/jobTemplate/spec/template/spec/dnsPolicy",
          "add",
          "ClusterFirst"),
      new SimpleIgnorePatch("/spec/jobTemplate/spec/template/spec/schedulerName",
          "add",
          "default-scheduler"),
      new SimpleIgnorePatch("/spec/jobTemplate/spec/template/spec/serviceAccount",
          "add"),
      new SimpleIgnorePatch(
          "/spec/jobTemplate/spec/template/spec/terminationGracePeriodSeconds",
          "add",
          "30"),
      new SimpleIgnorePatch("/spec/successfulJobsHistoryLimit",
          "add",
          "3"),
      new SimpleIgnorePatch("/spec/successfulJobsHistoryLimit",
          "add",
          "3"),
      new SimpleIgnorePatch("/spec/suspend",
          "add",
          "false"),
      new SimpleIgnorePatch("/metadata/annotations",
          "add"),
      new SimpleIgnorePatch("/status",
          "add"),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATTERS;
  }
}

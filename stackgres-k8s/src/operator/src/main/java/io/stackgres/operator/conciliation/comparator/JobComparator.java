/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import java.util.regex.Pattern;

public class JobComparator extends StackGresAbstractComparator {

  private static final Pattern SPEC_TEMPLATE_PATTERN = Pattern.compile("/spec/template(|/.*)$");
  private static final Pattern SPEC_TEMPLATE_ANNOTATIONS_PATTERN =
      Pattern.compile("/spec/template/metadata/annotations(|/.*)$");

  private static final IgnorePatch[] IGNORE_PATTERS = {
      new ExcludeExceptPattern(
          new PatchPattern(SPEC_TEMPLATE_PATTERN,
              "add"),
          new PatchPattern(SPEC_TEMPLATE_ANNOTATIONS_PATTERN,
              "add")),
      new ExcludeExceptPattern(
          new PatchPattern(SPEC_TEMPLATE_PATTERN,
              "replace"),
          new PatchPattern(SPEC_TEMPLATE_ANNOTATIONS_PATTERN,
              "replace")),
      new ExcludeExceptPattern(
          new PatchPattern(SPEC_TEMPLATE_PATTERN,
              "remove"),
          new PatchPattern(SPEC_TEMPLATE_ANNOTATIONS_PATTERN,
              "remove")),
      new ExcludeExceptPattern(
          new PatchPattern(SPEC_TEMPLATE_PATTERN,
              "move"),
          new PatchPattern(SPEC_TEMPLATE_ANNOTATIONS_PATTERN,
              "move")),
      new SimpleIgnorePatch("/metadata/annotations",
          "add"),
      new SimpleIgnorePatch("/spec/selector",
          "add"),
      new SimpleIgnorePatch("/status",
          "add"),
      new PatchPattern(Pattern
          .compile("/metadata/ownerReferences/\\d+/apiVersion"),
          "replace"
      ),
  };

  @Override
  protected IgnorePatch[] getPatchPattersToIgnore() {
    return IGNORE_PATTERS;
  }
}

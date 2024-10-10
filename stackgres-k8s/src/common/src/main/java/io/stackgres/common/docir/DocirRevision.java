/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.stackgres.common.StackGresUtil;

public class DocirRevision
    implements Comparable<DocirRevision> {

  private static final Pattern REVISION_PATTERN = Pattern.compile(
      "^(?<revision>[0-9]+)$");

  private final Long revision;

  public DocirRevision(String revision) {
    this.revision = Optional.ofNullable(revision)
        .map(REVISION_PATTERN::matcher)
        .filter(Matcher::find)
        .map(m -> m.group("revision"))
        .map(Long::parseLong)
        .orElse(0L);
  }

  public String getRevision() {
    return String.valueOf(revision);
  }

  public long getValue() {
    return revision;
  }

  @Override
  public int hashCode() {
    return Objects.hash(revision);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirRevision)) {
      return false;
    }
    DocirRevision other = (DocirRevision) obj;
    return Objects.equals(revision, other.revision);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public int compareTo(DocirRevision o) {
    return revision.compareTo(o.revision);
  }

}

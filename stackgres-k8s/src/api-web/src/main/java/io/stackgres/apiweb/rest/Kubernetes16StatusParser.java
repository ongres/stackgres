/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;

public class Kubernetes16StatusParser implements StatusParser {

  private static final Pattern MESSAGE_PATTERN = Pattern.compile("Invalid value: .*: ");

  @Override
  public String parseDetails(Status status) {

    String message;

    if (status.getDetails() != null
        && status.getDetails().getCauses() != null
        && !status.getDetails().getCauses().isEmpty()) {
      StatusCause cause = status.getDetails().getCauses().get(0);
      message = cause.getMessage();
    } else {
      message = status.getMessage();
    }

    Matcher m = MESSAGE_PATTERN.matcher(message);

    if (m.find()) {
      int lastIndex = m.end();
      return message.substring(lastIndex);
    } else {
      return message;
    }

  }

  @Override
  public String[] parseFields(Status status) {

    if (status.getDetails() != null && status.getDetails().getCauses() != null) {
      final List<StatusCause> causes = status.getDetails().getCauses();
      return causes.stream().map(StatusCause::getField).toArray(String[]::new);
    }

    return new String[0];
  }
}

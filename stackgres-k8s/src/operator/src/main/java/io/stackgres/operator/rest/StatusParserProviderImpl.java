/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.apiweb.StatusParser;
import io.stackgres.apiweb.StatusParserProvider;
import io.stackgres.common.KubernetesClientFactory;

@ApplicationScoped
public class StatusParserProviderImpl implements StatusParserProvider {

  private static final Pattern VERSION_PATTERN = Pattern.compile("^([0-9]+)[^0-9]*$");

  private StatusParser statusParser;

  @PostConstruct
  @Inject
  public void init(KubernetesClientFactory clientFactory) {
    try (KubernetesClient client = clientFactory.create()) {
      statusParser = Optional.ofNullable(client.getVersion())
          .map(VersionInfo::getMinor)
          .map(VERSION_PATTERN::matcher)
          .filter(Matcher::find)
          .map(matcher -> matcher.group(1))
          .filter(Objects::nonNull)
          .map(Integer::parseInt)
          .filter(version -> version < 16)
          .map(version -> new Kubernetes12StatusParser())
          .map(StatusParser.class::cast)
          .orElseGet(() -> new Kubernetes16StatusParser());
    }
  }

  @Override
  public StatusParser getStatusParser() {
    return statusParser;
  }

}

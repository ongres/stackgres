/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.utils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StatusParserProviderImpl implements StatusParserProvider {

  private static final Pattern VERSION_PATTERN = Pattern.compile("^([0-9]+)[^0-9]*$");
  private static final Logger LOGGER = LoggerFactory.getLogger(StatusParserProviderImpl.class);

  private StatusParser statusParser;
  private KubernetesClient client;

  @PostConstruct
  public void init() {
    statusParser = getMinorVersion()
        .map(this::getStatusParser)
        .orElseGet(() -> {
          LOGGER.debug("Cannot get k8s version. Using default parser (1.16+)");
          return new Kubernetes16StatusParser();
        });
  }

  @Inject
  public void setClient(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public StatusParser getStatusParser() {
    return statusParser;
  }

  private StatusParser getStatusParser(Integer minorVersion) {
    // Only K8s 1.16+ is supported
    if (minorVersion.intValue() < 16) {
      return null;
    }
    LOGGER.debug("Using parser for k8s version 1.16 or newer");
    return new Kubernetes16StatusParser();
  }

  private Optional<Integer> getMinorVersion() {
    return Optional.ofNullable(client.getKubernetesVersion())
        .map(VersionInfo::getMinor)
        .map(VERSION_PATTERN::matcher)
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1))
        .map(Integer::valueOf);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvConfResolverConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResolvConfResolverConfig.class);

  public @NotNull List<@NotNull String> getSearchPath(@NotNull String path) {
    Path resolvConf = Paths.get(path);
    if (Files.exists(resolvConf)) {
      try (InputStream in = Files.newInputStream(resolvConf, StandardOpenOption.READ)) {
        List<String> parseResolvConf = parseResolvConf(in);
        LOGGER.debug("Returning search paths: {}", parseResolvConf);
        return List.copyOf(parseResolvConf);
      } catch (IOException e) {
        LOGGER.warn("Could not parse resolv.conf file.", e);
      }
    }
    return List.of();
  }

  private List<String> parseResolvConf(InputStream in) throws IOException {
    var searchDomains = new ArrayList<String>(3);
    try (InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)) {
      String line;
      while ((line = br.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line);
        if (!st.hasMoreTokens()) {
          continue;
        }

        if (st.nextToken().equals("search")) {
          searchDomains.clear();
          while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            if (!searchDomains.contains(nextToken)) {
              searchDomains.add(nextToken);
            }
          }
        }
      }
    }
    return searchDomains;
  }

}

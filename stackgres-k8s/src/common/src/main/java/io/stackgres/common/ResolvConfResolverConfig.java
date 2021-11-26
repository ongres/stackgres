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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolvConfResolverConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResolvConfResolverConfig.class);

  private final List<String> searchlist = new ArrayList<>(1);

  public List<String> getSearchPath(String path) {
    searchlist.clear();
    if (tryParseResolveConf(path)) {
      return List.copyOf(searchlist);
    }
    return List.of();
  }

  private boolean tryParseResolveConf(String path) {
    Path p = Paths.get(path);
    if (Files.exists(p)) {
      try (InputStream in = Files.newInputStream(p)) {
        parseResolvConf(in);
        return true;
      } catch (IOException e) {
        // ignore
      }
    }

    return false;
  }

  private void parseResolvConf(InputStream in) throws IOException {
    try (InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)) {
      String line;
      while ((line = br.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line);
        if (!st.hasMoreTokens()) {
          continue;
        }

        if (st.nextToken().equals("search")) {
          searchlist.clear();
          while (st.hasMoreTokens()) {
            addSearchPath(st.nextToken());
          }
        }
      }
    }
  }

  private void addSearchPath(String searchPath) {
    if (searchPath == null || searchPath.isEmpty()) {
      return;
    }

    if (!searchlist.contains(searchPath)) {
      searchlist.add(searchPath);
      LOGGER.debug("Added {} to search paths", searchPath);
    }
  }

}

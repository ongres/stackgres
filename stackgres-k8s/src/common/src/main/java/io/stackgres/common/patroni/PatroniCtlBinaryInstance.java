/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import static io.stackgres.common.StackGresUtil.getPatroniMajorVersion;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.ongres.process.FluentProcess;
import com.ongres.process.FluentProcessBuilder;
import com.ongres.process.Output;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "Also hardcoded in generated image that use this code")
public class PatroniCtlBinaryInstance implements PatroniCtlInstance {

  private static final String BIN_PATH = "/bin";
  private static final String PATRONICTL_BINARY_PREFIX_PATH = "patronictl-";
  private static final Pattern SWITCHOVER_FAILED_PATTERN =
      Pattern.compile(".*(^Switchover failed.*$).*", Pattern.MULTILINE);
  private static final Pattern RESTART_FAILED_PATTERN =
      Pattern.compile(".*(^Failed: .*$).*", Pattern.MULTILINE);
  private static final Pattern ERROR_PATTERN = Pattern.compile("error", Pattern.CASE_INSENSITIVE);
  private static final TypeReference<List<PatroniMember>> LIST_TYPE_REFERENCE =
      new TypeReference<List<PatroniMember>>() { };
  private static final TypeReference<List<PatroniHistoryEntry>> HISTORY_TYPE_REFERENCE =
      new TypeReference<List<PatroniHistoryEntry>>() { };

  private static final Logger LOGGER = LoggerFactory.getLogger(PatroniCtlBinaryInstance.class);

  final ObjectMapper objectMapper;
  final YAMLMapper yamlMapper;
  final LabelFactoryForCluster clusterLabelFactory;

  final StackGresCluster cluster;
  final String scope;
  final Integer group;
  final String[] patroniCtlCommands;
  final int patroniMajorVersion;
  final Path configPath;
  final String config;
  final Duration patroniCtlTimeout = Duration
      .ofSeconds(OperatorProperty.PATRONI_CTL_TIMEOUT.get()
      .map(Long::parseLong)
      .orElse(60L));

  PatroniCtlBinaryInstance(
      ObjectMapper objectMapper,
      YAMLMapper yamlMapper,
      LabelFactoryForCluster clusterLabelFactory,
      StackGresCluster cluster) {
    this.objectMapper = objectMapper;
    this.yamlMapper = yamlMapper;
    this.clusterLabelFactory = clusterLabelFactory;
    this.cluster = cluster;
    this.scope = PatroniUtil.clusterScope(cluster);
    this.group = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .flatMap(StackGresClusterPatroniConfig::getCitusGroup)
        .orElse(null);
    final String patroniVersion = StackGresUtil.getPatroniVersion(cluster);
    int patroniMajorVersion = StackGresUtil.getPatroniMajorVersion(patroniVersion);
    this.patroniMajorVersion = patroniMajorVersion;
    this.patroniCtlCommands = patroniCtlCommands(patroniVersion, patroniMajorVersion);
    this.configPath = getConfigPath();
    this.config = PatroniUtil.getInitialConfig(
        cluster, clusterLabelFactory, yamlMapper, objectMapper);
  }

  final String[] patroniCtlCommands(String version, int patroniMajorVersion) {
    String command = patroniCtlCommand(version);
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(command, StandardCharsets.UTF_8))) {
      String firstLine = bufferedReader.readLine();
      if (firstLine != null && firstLine.startsWith("#!")) {
        return new String[] { firstLine.substring(2), command };
      }
    } catch (IOException ex) {
      LOGGER.error("Error while trying to read file " + command, ex);
    }
    return new String[] { "python3.11", command };
  }

  final String patroniCtlCommand(String version) {
    String command = BIN_PATH + "/" + PATRONICTL_BINARY_PREFIX_PATH + version;
    if (Files.exists(Paths.get(command))) {
      return command;
    }
    int patroniMajorVersion = getPatroniMajorVersion(version);
    if (patroniMajorVersion < PatroniUtil.PATRONI_VERSION_4) {
      patroniMajorVersion = 3;
    }
    final String patroniVersionPrefix = PATRONICTL_BINARY_PREFIX_PATH + patroniMajorVersion + ".";
    try (var list = Unchecked.supplier(() -> Files.list(Paths.get(BIN_PATH))).get()) {
      var pathFound = list
          .filter(path -> path.getFileName().toString().startsWith(patroniVersionPrefix))
          .sorted(new PatroniCtlPathComparator().reversed())
          .findFirst();
      if (pathFound.isPresent()) {
        return pathFound.get().toAbsolutePath().toString();
      }
    }
    throw new RuntimeException("No patronictl binary found for version " + version);
  }

  static class PatroniCtlPathComparator implements Comparator<Path>, Serializable {

    static final long serialVersionUID = 1L;

    @Override
    public int compare(Path o1, Path o2) {
      return sortableVersion(o1).compareTo(sortableVersion(o2));
    }

    String sortableVersion(Path path) {
      return sortableVersion(path.getFileName().toString()
          .substring(PATRONICTL_BINARY_PREFIX_PATH.length()));
    }

    String sortableVersion(String version) {
      final String[] versionParts = version.split("\\.", -1);
      return IntStream.range(0, 5)
          .mapToObj(i -> zeroPad(i < versionParts.length ? versionParts[i] : "", 10))
          .collect(Collectors.joining());
    }

    String zeroPad(String string, int totalLength) {
      string = string.substring(0, Math.min(string.length(), 10));
      return IntStream.range(0, totalLength - string.length())
          .mapToObj(i -> "0")
          .collect(Collectors.joining()) + string;
    }
  }

  final Path getConfigPath() {
    return Paths.get("/tmp/patroni/"
        + cluster.getMetadata().getNamespace()
        + "/" + cluster.getMetadata().getName()
        + "/config.yaml");
  }

  final void writeConfig() {
    try {
      Files.createDirectories(configPath.getParent());
      try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
        fos.write(config.getBytes(StandardCharsets.UTF_8));
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public List<PatroniMember> list() {
    Output output = patronictl("list", "-f", "json", "-e")
        .start()
        .withTimeout(patroniCtlTimeout)
        .withoutCloseAfterLast()
        .tryGet();
    String result = getOutputOrFail(output);
    try {
      return objectMapper.readValue(result, LIST_TYPE_REFERENCE);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public List<PatroniHistoryEntry> history() {
    Output output = patronictl("history", "-f", "json")
        .start()
        .withTimeout(patroniCtlTimeout)
        .withoutCloseAfterLast()
        .tryGet();
    String result = getOutputOrFail(output);
    try {
      return objectMapper.readValue(result, HISTORY_TYPE_REFERENCE);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public PatroniConfig showConfig() {
    Output output = patronictl("show-config")
        .start()
        .withTimeout(patroniCtlTimeout)
        .withoutCloseAfterLast()
        .tryGet();
    String result = getOutputOrFail(output);
    try {
      if (result == null || result.isBlank()) {
        return new PatroniConfig();
      }
      return yamlMapper.readValue(result, PatroniConfig.class);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public ObjectNode showConfigJson() {
    Output output = patronictl("show-config")
        .start()
        .withTimeout(patroniCtlTimeout)
        .withoutCloseAfterLast()
        .tryGet();
    String result = getOutputOrFail(output);
    try {
      JsonNode config = yamlMapper.readTree(result);
      if (config instanceof ObjectNode configObject) {
        return configObject;
      }
      return yamlMapper.createObjectNode();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void editConfig(PatroniConfig patroniConfig) {
    try (InputStream in = new ByteArrayInputStream(yamlMapper.writeValueAsBytes(patroniConfig))) {
      Output output = patronictl("edit-config", scope, "--apply", "-")
          .start()
          .inputStream(in)
          .withTimeout(patroniCtlTimeout)
          .withoutCloseAfterLast()
          .tryGet();
      getOutputOrFail(output);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void editConfigJson(ObjectNode patroniConfig) {
    try (InputStream in = new ByteArrayInputStream(patroniConfig.toString().getBytes(StandardCharsets.UTF_8))) {
      Output output = patronictl("edit-config", scope, "--apply", "-")
          .start()
          .inputStream(in)
          .withTimeout(patroniCtlTimeout)
          .withoutCloseAfterLast()
          .tryGet();
      getOutputOrFail(output);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void restart(String username, String password, String member) {
    Output output = patronictl("restart", scope, member, "--force")
        .environment("PATRONI_RESTAPI_USERNAME", username)
        .environment("PATRONI_RESTAPI_PASSWORD", password)
        .start()
        .withTimeout(patroniCtlTimeout)
        .withoutCloseAfterLast()
        .tryGet();
    getOutputOrFail(output);
    output.output()
        .map(RESTART_FAILED_PATTERN::matcher)
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1))
        .ifPresent(error -> {
          throw new RuntimeException(error);
        });
  }

  @Override
  public void switchover(String username, String password, String leader, String candidate) {
    Output output = patronictl(
        "switchover", scope,
        patroniMajorVersion < PatroniUtil.PATRONI_VERSION_4 ? "--master" : "--primary", leader,
        "--candidate", candidate, "--force")
        .environment("PATRONI_RESTAPI_USERNAME", username)
        .environment("PATRONI_RESTAPI_PASSWORD", password)
        .start()
        .withTimeout(patroniCtlTimeout)
        .withoutCloseAfterLast()
        .tryGet();
    getOutputOrFail(output);
    output.output()
        .map(SWITCHOVER_FAILED_PATTERN::matcher)
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1))
        .ifPresent(error -> {
          throw new RuntimeException(error);
        });
  }

  @Override
  public void remove(String username, String password) {
    Output output = patronictl("remove", scope)
        .environment("PATRONI_RESTAPI_USERNAME", username)
        .environment("PATRONI_RESTAPI_PASSWORD", password)
        .start()
        .inputStream(
            Seq.of(
                scope,
                "Yes I am aware")
            .append(list()
                .stream()
                .filter(member -> member.isPrimary())
                .map(PatroniMember::getMember)))
        .withTimeout(patroniCtlTimeout)
        .withoutCloseAfterLast()
        .tryGet();
    getOutputOrFail(output);
    if (output.error()
        .map(ERROR_PATTERN::matcher)
        .filter(Matcher::find)
        .isPresent()) {
      throw new RuntimeException(output.error().get());
    }
  }

  @Override
  public JsonNode queryPrimary(String query, String username, String password) {
    Output output = patronictl(
        "query", "-c", query, "-U", username,
        "-r", patroniMajorVersion < PatroniUtil.PATRONI_VERSION_4 ? "master" : "primary",
        "--format", "json")
        .environment("PGPASSWORD", password)
        .start()
        .withTimeout(patroniCtlTimeout)
        .withoutCloseAfterLast()
        .tryGet();
    String result = getOutputOrFail(output);
    try {
      return objectMapper.readTree(result);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private FluentProcessBuilder patronictl(String command, String... args) {
    return FluentProcess
        .builder(patroniCtlCommands[0], Seq.of(patroniCtlCommands[1])
            .append("-c", configPath.toString())
            .append(command)
            .append(args)
            .append(group != null ? Seq.of("--group", group.toString()) : Seq.of())
            .toArray(String[]::new));
  }

  private String getOutputOrFail(Output output) {
    if (output.exception().isPresent()) {
      throw new RuntimeException(output.exception().get().getMessage()
          + output.error().map(stderr -> "\n\nstderr: " + stderr).orElse("")
          + output.output().map(stdout -> "\n\nstdout:" + stdout).orElse(""),
          output.exception().get());
    }
    String result = output.output()
        .orElseThrow(() -> new IllegalStateException("Empty output"));
    return result;
  }
}

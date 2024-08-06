/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.pooling.PgBouncerIniParameter;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.dto.pooling.PoolingConfigPgBouncer;
import io.stackgres.apiweb.dto.pooling.PoolingConfigPgBouncerStatus;
import io.stackgres.apiweb.dto.pooling.PoolingConfigSpec;
import io.stackgres.apiweb.dto.pooling.PoolingConfigStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple3;

@ApplicationScoped
public class PoolingConfigTransformer
    extends AbstractDependencyResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> {

  private static final Pattern PARAMETER_PATTERN = Pattern.compile(
      "([^\\s=]+)\\s*=\\s*(:?'([^']+)'|[^ ]+)");

  private final ObjectMapper mapper;

  @Inject
  public PoolingConfigTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresPoolingConfig toCustomResource(PoolingConfigDto source,
                                                 StackGresPoolingConfig original) {
    StackGresPoolingConfig transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresPoolingConfig.class))
        .orElseGet(StackGresPoolingConfig::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public PoolingConfigDto toResource(StackGresPoolingConfig source, List<String> clusters) {
    PoolingConfigDto transformation = new PoolingConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(clusters, source.getStatus(), source.getSpec()));
    return transformation;
  }

  private StackGresPoolingConfigSpec getCustomResourceSpec(PoolingConfigSpec source) {
    if (source == null) {
      return null;
    }
    StackGresPoolingConfigSpec transformation = new StackGresPoolingConfigSpec();
    transformation.setPgBouncer(new StackGresPoolingConfigPgBouncer());

    Optional<PoolingConfigPgBouncer> pgbouncer =
        Optional.of(source).map(PoolingConfigSpec::getPgBouncer);

    INIConfiguration iniConfiguration = new INIConfiguration();
    pgbouncer.map(PoolingConfigPgBouncer::getParameters)
        .ifPresent(ini -> {
          try {
            String cleanup = ini.lines()
                .filter(p -> !p.startsWith("%include"))
                .map(m -> {
                  if (m.indexOf('=') != -1) {
                    int indexOfEquals = m.indexOf('=');
                    return m.substring(0, indexOfEquals) + " = \""
                        + m.substring(indexOfEquals + 1) + "\"";
                  }
                  return m;
                })
                .collect(Collectors.joining("\n"));
            iniConfiguration.read(new StringReader(cleanup));
          } catch (ConfigurationException | IOException cause) {
            throw new RuntimeException("Could not read INI configuration", cause);
          }
        });

    StackGresPoolingConfigPgBouncerPgbouncerIni pgbouncerIni = transformation.getPgBouncer()
        .getPgbouncerIni();

    if (pgbouncerIni == null) {
      pgbouncerIni = new StackGresPoolingConfigPgBouncerPgbouncerIni();
      transformation.getPgBouncer().setPgbouncerIni(pgbouncerIni);
    }
    for (String sectionName : iniConfiguration.getSections()) {
      SubnodeConfiguration section = iniConfiguration.getSection(sectionName);
      if (section != null) {
        Iterator<String> keys = section.getKeys();
        while (keys.hasNext()) {
          String key = keys.next();
          String value = section.getString(key);
          if (value != null) {
            if ("pgbouncer".equals(sectionName) || sectionName == null) {
              if (pgbouncerIni.getPgbouncer() == null) {
                pgbouncerIni.setPgbouncer(new HashMap<>());
              }
              pgbouncerIni
                  .getPgbouncer().put(key, value);
            }
            if ("databases".equals(sectionName)) {
              if (pgbouncerIni.getDatabases() == null) {
                pgbouncerIni.setDatabases(new HashMap<>());
              }
              var databases = new HashMap<String, String>();
              Matcher matcher = PARAMETER_PATTERN.matcher(value);
              while (matcher.find()) {
                String keyItem = matcher.group(1);
                String valueItem = matcher.group(2);
                databases.put(keyItem, valueItem);
              }
              pgbouncerIni.getDatabases()
                  .put(key, databases);
            }
            if ("users".equals(sectionName)) {
              if (pgbouncerIni.getUsers() == null) {
                pgbouncerIni.setUsers(new HashMap<>());
              }
              var users = new HashMap<String, String>();
              Matcher matcher = PARAMETER_PATTERN.matcher(value);
              while (matcher.find()) {
                final String keyItem = matcher.group(1);
                final String valueItem = matcher.group(2);
                users.put(keyItem, valueItem);
              }
              pgbouncerIni.getUsers()
                  .put(key, users);
            }
          }
        }
      }
    }
    return transformation;
  }

  private PoolingConfigSpec getResourceSpec(StackGresPoolingConfigSpec source) {
    PoolingConfigSpec transformation = new PoolingConfigSpec();
    transformation.setPgBouncer(new PoolingConfigPgBouncer());

    INIConfiguration ini = new INIConfiguration();
    StackGresPoolingConfigPgBouncer pgBouncer = source.getPgBouncer();
    if (pgBouncer.getPgbouncerIni().getDatabases() != null) {
      addPropertiesToIni(source.getPgBouncer().getPgbouncerIni()
          .getDatabases().entrySet(), ini, "databases");
    }

    if (pgBouncer.getPgbouncerIni().getUsers() != null) {
      addPropertiesToIni(pgBouncer.getPgbouncerIni().getUsers().entrySet(), ini, "users");
    }

    if (pgBouncer.getPgbouncerIni().getPgbouncer() != null) {
      pgBouncer.getPgbouncerIni().getPgbouncer().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> {
            ini.addProperty("pgbouncer." + entry.getKey(), entry.getValue());
          });
    }

    StringWriter stringWriter = new StringWriter();
    try {
      ini.write(stringWriter);
    } catch (ConfigurationException | IOException cause) {
      throw new RuntimeException("Could not write INI configuration", cause);
    }

    transformation.getPgBouncer().setParameters(stringWriter.toString());
    return transformation;
  }

  private void addPropertiesToIni(
      Set<Entry<String, Map<String, String>>> source,
      INIConfiguration target,
      String section) {
    source.stream().sorted(Map.Entry.comparingByKey())
        .forEach(entry -> {
          String params = entry.getValue().entrySet().stream()
              .map(e -> e.getKey() + "=" + e.getValue())
              .collect(Collectors.joining(" "));
          target.addProperty(section + "." + entry.getKey(), params);
        });
  }

  private PoolingConfigStatus getResourceStatus(
      List<String> clusters,
      StackGresPoolingConfigStatus source,
      StackGresPoolingConfigSpec sourceSpec) {
    PoolingConfigStatus transformation = new PoolingConfigStatus();
    transformation.setClusters(clusters);
    transformation.setPgBouncer(new PoolingConfigPgBouncerStatus());
    transformation.getPgBouncer().setPgbouncerIni(
        Seq.seq(sourceSpec.getPgBouncer().getPgbouncerIni().getPgbouncer())
            .map(t -> t.concat(new PgBouncerIniParameter()))
            .peek(t -> t.v3.setParameter(t.v1))
            .peek(t -> t.v3.setValue(t.v2))
            .map(Tuple3::v3)
            .toList());
    if (source != null && source.getPgBouncer() != null) {
      transformation.getPgBouncer().setDefaultParameters(
          source.getPgBouncer().getDefaultParameters());
    }
    return transformation;
  }

}

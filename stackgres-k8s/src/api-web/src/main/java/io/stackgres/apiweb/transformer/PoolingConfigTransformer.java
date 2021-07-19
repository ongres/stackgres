/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.base.CaseFormat;
import io.stackgres.apiweb.dto.pooling.PgBouncerIniParameter;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.dto.pooling.PoolingConfigPgBouncer;
import io.stackgres.apiweb.dto.pooling.PoolingConfigPgBouncerStatus;
import io.stackgres.apiweb.dto.pooling.PoolingConfigSpec;
import io.stackgres.apiweb.dto.pooling.PoolingConfigStatus;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.common.crd.sgpooling.pgbouncer.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.pgbouncer.StackGresPoolingConfigPgBouncerDatabases;
import io.stackgres.common.crd.sgpooling.pgbouncer.StackGresPoolingConfigPgBouncerUsers;
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

  @Override
  public StackGresPoolingConfig toCustomResource(PoolingConfigDto source,
      StackGresPoolingConfig original) {
    StackGresPoolingConfig transformation = Optional.ofNullable(original)
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
                        + m.substring(indexOfEquals + 1, m.length()) + "\"";
                  }
                  return m;
                })
                .collect(Collectors.joining("\n"));
            iniConfiguration.read(new StringReader(cleanup));
          } catch (ConfigurationException | IOException ignore) {
            // ignore for now
          }
        });

    for (String sectionName : iniConfiguration.getSections()) {
      SubnodeConfiguration section = iniConfiguration.getSection(sectionName);
      if (section != null) {
        Iterator<String> keys = section.getKeys();
        while (keys.hasNext()) {
          String key = keys.next();
          String value = section.getString(key);
          if (value != null) {
            if ("pgbouncer".equals(sectionName) || sectionName == null) {
              if (transformation.getPgBouncer().getParameters() == null) {
                transformation.getPgBouncer().setParameters(new HashMap<>());
              }
              transformation.getPgBouncer()
                  .getParameters().put(key, value);
            }
            if ("databases".equals(sectionName)) {
              if (transformation.getPgBouncer().getDatabases() == null) {
                transformation.getPgBouncer().setDatabases(new HashMap<>());
              }
              var databases = new StackGresPoolingConfigPgBouncerDatabases();
              Matcher matcher = PARAMETER_PATTERN.matcher(value);
              while (matcher.find()) {
                final String keyItem = matcher.group(1);
                final String valueItem = matcher.group(2);

                mapValues(databases, keyItem, valueItem);
              }
              transformation.getPgBouncer().getDatabases()
                  .put(key, databases);
            }
            if ("users".equals(sectionName)) {
              if (transformation.getPgBouncer().getUsers() == null) {
                transformation.getPgBouncer().setUsers(new HashMap<>());
              }
              var users = new StackGresPoolingConfigPgBouncerUsers();
              Matcher matcher = PARAMETER_PATTERN.matcher(value);
              while (matcher.find()) {
                final String keyItem = matcher.group(1);
                final String valueItem = matcher.group(2);

                mapValues(users, keyItem, valueItem);
              }
              transformation.getPgBouncer().getUsers()
                  .put(key, users);
            }
          }
        }
      }
    }
    return transformation;
  }

  private void mapValues(Object clazzObj, String param, String value) {
    String methodName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, "set_" + param);
    for (Method method : clazzObj.getClass().getMethods()) {
      if (method.getName().equals(methodName)) {
        String parameter = value.replace("'", "");
        try {
          if (Integer.class.equals(method.getParameterTypes()[0])) {
            method.invoke(clazzObj, Integer.valueOf(parameter));
          } else {
            method.invoke(clazzObj, parameter);
          }
        } catch (IllegalAccessException | IllegalArgumentException
            | InvocationTargetException ignore) {
          // ignore
        }
      }
    }
  }

  private PoolingConfigSpec getResourceSpec(StackGresPoolingConfigSpec source) {
    PoolingConfigSpec transformation = new PoolingConfigSpec();
    transformation.setPgBouncer(new PoolingConfigPgBouncer());

    INIConfiguration ini = new INIConfiguration();
    if (source.getPgBouncer().getDatabases() != null) {
      source.getPgBouncer().getDatabases().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> {
            final String params = Seq.of("dbname", "pool_size", "reserve_pool", "pool_mode",
                "max_db_connections", "client_encoding", "datestyle", "timezone")
                .map(param -> StackGresUtil.mapMethodParameterValues(param, entry.getValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce((first, second) -> first + " " + second)
                .orElse("");
            ini.addProperty("databases." + entry.getKey(), params);
          });
    }

    if (source.getPgBouncer().getUsers() != null) {
      source.getPgBouncer().getUsers().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> {
            final String params = Seq.of("pool_mode", "max_user_connections")
                .map(param -> StackGresUtil.mapMethodParameterValues(param, entry.getValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce((first, second) -> first + " " + second)
                .orElse("");
            ini.addProperty("users." + entry.getKey(), params);
          });
    }

    if (source.getPgBouncer().getParameters() != null) {
      source.getPgBouncer().getParameters().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> {
            ini.addProperty("pgbouncer." + entry.getKey(), entry.getValue());
          });
    }

    StringWriter stringWriter = new StringWriter();
    try {
      ini.write(stringWriter);
    } catch (ConfigurationException | IOException ignore) {
      // ignore
    }

    transformation.getPgBouncer().setParameters(stringWriter.toString());
    return transformation;
  }

  private PoolingConfigStatus getResourceStatus(List<String> clusters,
      StackGresPoolingConfigStatus source, StackGresPoolingConfigSpec sourceSpec) {
    PoolingConfigStatus transformation = new PoolingConfigStatus();
    transformation.setClusters(clusters);
    transformation.setPgBouncer(new PoolingConfigPgBouncerStatus());
    transformation.getPgBouncer().setParameters(
        Seq.seq(sourceSpec.getPgBouncer().getParameters())
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

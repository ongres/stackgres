/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.source;

import java.util.Optional;
import java.util.Properties;

import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgres;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgresDebeziumProperties;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.stream.jobs.DebeziumUtil;
import io.stackgres.stream.jobs.StreamSourceOperation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@StreamSourceOperation(StreamSourceType.POSTGRES)
public class PostgresDebeziumEngineHandler extends AbstractPostgresDebeziumEngineHandler {

  @Override
  protected void setSourceProperties(StackGresStream stream, Properties props) {
    String namespace = stream.getMetadata().getNamespace();
    var postgres = Optional.of(stream.getSpec().getSource().getPostgres());
    DebeziumUtil.configureDebeziumSectionProperties(
        props,
        postgres
        .map(StackGresStreamSourcePostgres::getDebeziumProperties)
        .orElse(null),
        StackGresStreamSourcePostgresDebeziumProperties.class);
    props.setProperty("slot.name", slotName(stream));
    props.setProperty("publication.name", publicationName(stream));

    DebeziumUtil.configureDebeziumIncludesAndExcludes(props, postgres.orElseThrow());
    String clusterHost = postgres.map(StackGresStreamSourcePostgres::getHost)
        .orElseThrow(() -> new IllegalArgumentException("The postgres host is not specified"));
    String clusterPort = postgres.map(StackGresStreamSourcePostgres::getPort)
        .map(Object::toString)
        .orElse("5432");
    String usernameSecretName = postgres
        .map(StackGresStreamSourcePostgres::getUsername)
        .map(SecretKeySelector::getName)
        .orElseThrow(() -> new IllegalArgumentException("The postgres username secret is not specified"));
    String usernameSecretKey = postgres
        .map(StackGresStreamSourcePostgres::getUsername)
        .map(SecretKeySelector::getKey)
        .orElseThrow(() -> new IllegalArgumentException("The postgres username secret key is not specified"));
    var username = getSecretKeyValue(namespace, usernameSecretName, usernameSecretKey);
    String passwordSecretName = postgres
        .map(StackGresStreamSourcePostgres::getPassword)
        .map(SecretKeySelector::getName)
        .orElseThrow(() -> new IllegalArgumentException("The postgres password secret is not specified"));
    String passwordSecretKey = postgres
        .map(StackGresStreamSourcePostgres::getPassword)
        .map(SecretKeySelector::getKey)
        .orElseThrow(() -> new IllegalArgumentException("The postgres password secret key is not specified"));
    var password = getSecretKeyValue(namespace, passwordSecretName, passwordSecretKey);

    props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
    props.setProperty("database.hostname", clusterHost);
    props.setProperty("database.port", clusterPort);
    props.setProperty("database.dbname", Optional
        .ofNullable(stream.getSpec().getSource().getPostgres())
        .map(StackGresStreamSourcePostgres::getDatabase)
        .orElse("postgres"));
    props.setProperty("database.user", username);
    props.setProperty("database.password", password);
    props.setProperty("database.server.name", clusterHost);
  }

}

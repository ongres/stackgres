package io.stackgres.slon.vector;

/**
 * Vector pipeline templates embedded as Java text blocks.
 *
 * <p>These are the runtime source of truth for the slon-rendered
 * {@code /tmp/postgres/vector/vector.yaml}. The {@code .yaml.template} files
 * under {@code logs/build/agent/} in the public {@code stackgres} repo are
 * kept in sync for readability and used by the agent-build e2e test;
 * if you edit one side, mirror the other.
 *
 * <p>The {@code ${INSTANCE_UUID}}, {@code ${OTLP_ENDPOINT}}, and
 * {@code ${OTLP_AUTH_HEADER}} placeholders are Vector's own env-var
 * interpolation syntax — slon sets those env vars on the spawned
 * vector process and Vector resolves them at config load.
 *
 * <p>The {@code # BEGIN otlp_remote} / {@code # END otlp_remote} comment
 * pair brackets the conditional remote sink; {@code VectorProcesses}
 * strips that block when {@code OTLP_ENDPOINT} is unset.
 *
 * <p>Backslashes in VRL regex patterns ({@code \d}, {@code \w}, {@code \b},
 * {@code \.}) must be Java-escaped ({@code \\d}, etc.) because text
 * blocks still process escape sequences.
 */
public final class VectorPipelineTemplates {

    private VectorPipelineTemplates() {
    }

    public static final String STANDALONE = """
            # Vector pipeline (standalone-mode slon container).
            #
            # Single-tenant by construction: one agent per slon, one slon per Postgres
            # instance. instance.uuid is a slon-level constant.
            #
            # Sources: PG csvlog (typed path) + postmaster stderr + slon's own JSON log.
            # slon writes this rendered yaml to /tmp/postgres/vector/vector.yaml and spawns
            # vector with INSTANCE_UUID / OTLP_ENDPOINT / OTLP_AUTH_HEADER in its env
            # (Vector's native dollar-brace interpolation does the substitution at
            # config load). The BEGIN/END otlp_remote markers bracket the conditional
            # remote sink — VectorProcesses strips that block when OTLP_ENDPOINT is
            # unset.
            #
            # IMPORTANT: do NOT write the literal placeholder syntax in comments —
            # Vector applies env-var interpolation textually across the whole YAML
            # *including comments* and will fail config load if it can't resolve a
            # referenced variable.
            
            data_dir: /tmp/postgres/vector/data
            
            sources:
              pg_csvlog:
                type: file
                include:
                  - /tmp/postgres/log/postgresql-*.csv
                read_from: beginning
                multiline:
                  start_pattern: '^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+ \\w+,'
                  mode: halt_before
                  condition_pattern: '^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+ \\w+,'
                  timeout_ms: 1000
            
              postmaster:
                type: file
                include:
                  - /tmp/server.log
                read_from: beginning
            
              slon:
                type: file
                include:
                  - /tmp/slon.log
                read_from: beginning
            
            transforms:
              tag_instance:
                type: remap
                inputs: [pg_csvlog]
                source: |
                  .instance_uuid = "${INSTANCE_UUID}"
            
              pg_parse:
                type: remap
                inputs: [tag_instance]
                source: |
                  raw = string!(.message)
                  cols, err = parse_csv(raw)
                  if err != null {
                    .parse_error = "csv parse: " + err
                  } else if length(cols) != 26 {
                    .parse_error = "unexpected csvlog column count: " + to_string(length(cols)) + " (expected 26 for PG17+)"
                  } else {
                    .log_time               = cols[0]
                    .user_name              = cols[1]
                    .database_name          = cols[2]
                    .process_id             = cols[3]
                    .connection_from        = cols[4]
                    .session_id             = cols[5]
                    .session_line_num       = cols[6]
                    .command_tag            = cols[7]
                    .session_start_time     = cols[8]
                    .virtual_transaction_id = cols[9]
                    .transaction_id         = cols[10]
                    .severity               = cols[11]
                    .sql_state_code         = cols[12]
                    .pg_message             = cols[13]
                    .detail                 = cols[14]
                    .hint                   = cols[15]
                    .internal_query         = cols[16]
                    .internal_query_pos     = cols[17]
                    .context                = cols[18]
                    .query                  = cols[19]
                    .query_pos              = cols[20]
                    .location               = cols[21]
                    .application_name       = cols[22]
                    .backend_type           = cols[23]
                    .leader_pid             = cols[24]
                    .query_id               = cols[25]
            
                    .message = .log_time + " [" + .process_id + "] " + .severity + ": " + .pg_message
                  }
            
              pg_to_otlp:
                type: remap
                inputs: [pg_parse]
                source: |
                  parsed_ts = parse_timestamp(string!(.log_time), "%Y-%m-%d %H:%M:%S%.f %Z") ?? now()
                  ts_ns = to_unix_timestamp(parsed_ts, unit: "nanoseconds")
            
                  sev_text = string!(.severity)
                  sev_num = if sev_text == "DEBUG1" || sev_text == "DEBUG2" || sev_text == "DEBUG3" || sev_text == "DEBUG4" || sev_text == "DEBUG5" {
                    5
                  } else if sev_text == "INFO" || sev_text == "LOG" {
                    9
                  } else if sev_text == "NOTICE" {
                    10
                  } else if sev_text == "WARNING" {
                    13
                  } else if sev_text == "ERROR" {
                    17
                  } else if sev_text == "FATAL" || sev_text == "PANIC" {
                    21
                  } else {
                    0
                  }
            
                  attrs = [
                    {"key": "db.user",              "value": {"stringValue": string!(.user_name)}},
                    {"key": "db.process_id",        "value": {"stringValue": string!(.process_id)}},
                    {"key": "client.address",       "value": {"stringValue": string!(.connection_from)}},
                    {"key": "db.session_id",        "value": {"stringValue": string!(.session_id)}},
                    {"key": "pg.session_line_num",  "value": {"stringValue": string!(.session_line_num)}},
                    {"key": "db.operation",         "value": {"stringValue": string!(.command_tag)}},
                    {"key": "pg.session_start_time","value": {"stringValue": string!(.session_start_time)}},
                    {"key": "pg.virtual_xid",       "value": {"stringValue": string!(.virtual_transaction_id)}},
                    {"key": "db.transaction_id",    "value": {"stringValue": string!(.transaction_id)}},
                    {"key": "db.sqlstate",          "value": {"stringValue": string!(.sql_state_code)}},
                    {"key": "pg.detail",            "value": {"stringValue": string!(.detail)}},
                    {"key": "pg.hint",              "value": {"stringValue": string!(.hint)}},
                    {"key": "pg.internal_query",    "value": {"stringValue": string!(.internal_query)}},
                    {"key": "pg.internal_query_pos","value": {"stringValue": string!(.internal_query_pos)}},
                    {"key": "pg.context",           "value": {"stringValue": string!(.context)}},
                    {"key": "db.statement",         "value": {"stringValue": string!(.query)}},
                    {"key": "pg.query_pos",         "value": {"stringValue": string!(.query_pos)}},
                    {"key": "pg.location",          "value": {"stringValue": string!(.location)}},
                    {"key": "db.application",       "value": {"stringValue": string!(.application_name)}},
                    {"key": "db.backend_type",      "value": {"stringValue": string!(.backend_type)}},
                    {"key": "pg.leader_pid",        "value": {"stringValue": string!(.leader_pid)}},
                    {"key": "pg.query_id",          "value": {"stringValue": string!(.query_id)}}
                  ]
            
                  . = {
                    "resourceLogs": [{
                      "resource": {
                        "attributes": [
                          {"key": "service.name",  "value": {"stringValue": "postgresql"}},
                          {"key": "db.system",     "value": {"stringValue": "postgresql"}},
                          {"key": "db.namespace",  "value": {"stringValue": string!(.database_name)}},
                          {"key": "instance.uuid", "value": {"stringValue": string!(.instance_uuid)}}
                        ]
                      },
                      "scopeLogs": [{
                        "scope": {"name": "postgresql.csvlog"},
                        "logRecords": [{
                          "timeUnixNano":         to_string(ts_ns),
                          "observedTimeUnixNano": to_string(ts_ns),
                          "severityNumber":       sev_num,
                          "severityText":         sev_text,
                          "body":                 {"stringValue": string!(.pg_message)},
                          "attributes":           attrs
                        }]
                      }]
                    }]
                  }
            
              tag_postmaster:
                type: remap
                inputs: [postmaster]
                source: |
                  .instance_uuid = "${INSTANCE_UUID}"
                  .component = "postmaster"
                  raw = string!(.message)
                  sev = "INFO"
                  if match(raw, r'\\bPANIC:') {
                    sev = "PANIC"
                  } else if match(raw, r'\\bFATAL:') {
                    sev = "FATAL"
                  } else if match(raw, r'\\bERROR:') {
                    sev = "ERROR"
                  } else if match(raw, r'\\bWARNING:') {
                    sev = "WARNING"
                  } else if match(raw, r'\\bLOG:') {
                    sev = "LOG"
                  }
                  .severity_text = sev
                  .severity_number = if sev == "FATAL" || sev == "PANIC" {
                    21
                  } else if sev == "ERROR" {
                    17
                  } else if sev == "WARNING" {
                    13
                  } else {
                    9
                  }
                  .body = raw
                  .ts_ns = to_unix_timestamp(now(), unit: "nanoseconds")
            
              tag_slon:
                type: remap
                inputs: [slon]
                source: |
                  .instance_uuid = "${INSTANCE_UUID}"
                  .component = "slon"
                  raw = string!(.message)
                  parsed, err = parse_json(raw)
                  sev = "INFO"
                  body = raw
                  ts_ns = to_unix_timestamp(now(), unit: "nanoseconds")
                  if err == null && is_object(parsed) {
                    if parsed.msg != null {
                      body = to_string(parsed.msg) ?? raw
                    }
                    if parsed.level != null {
                      sev = to_string(parsed.level) ?? "INFO"
                    }
                    if parsed.ts != null {
                      ts_str = to_string(parsed.ts) ?? ""
                      parsed_ts, terr = parse_timestamp(ts_str, "%+")
                      if terr == null {
                        ts_ns = to_unix_timestamp(parsed_ts, unit: "nanoseconds")
                      }
                    }
                  }
                  .severity_text = sev
                  .severity_number = if sev == "SEVERE" {
                    17
                  } else if sev == "WARNING" {
                    13
                  } else if sev == "INFO" {
                    9
                  } else if sev == "CONFIG" || sev == "FINE" || sev == "FINER" || sev == "FINEST" {
                    5
                  } else {
                    0
                  }
                  .body = body
                  .ts_ns = ts_ns
            
              system_log_to_otlp:
                type: remap
                inputs: [tag_postmaster, tag_slon]
                source: |
                  # Coerce field reads to concrete types — VRL is strict, and a bare
                  # `.ts_ns` is untyped (E103 if later used in to_string()).
                  ts_ns = to_int(.ts_ns) ?? 0
                  sev_num = to_int(.severity_number) ?? 0
                  sev_text = string!(.severity_text)
                  body = string!(.body)
                  component = string!(.component)
                  instance_uuid = string!(.instance_uuid)
                  . = {
                    "resourceLogs": [{
                      "resource": {
                        "attributes": [
                          {"key": "service.name",  "value": {"stringValue": component}},
                          {"key": "instance.uuid", "value": {"stringValue": instance_uuid}}
                        ]
                      },
                      "scopeLogs": [{
                        "scope": {"name": component},
                        "logRecords": [{
                          "timeUnixNano":         to_string(ts_ns),
                          "observedTimeUnixNano": to_string(ts_ns),
                          "severityNumber":       sev_num,
                          "severityText":         sev_text,
                          "body":                 {"stringValue": body},
                          "attributes":           []
                        }]
                      }]
                    }]
                  }
            
            sinks:
              console_text:
                type: console
                inputs: [pg_parse, tag_postmaster, tag_slon]
                encoding:
                  codec: text
            
              # BEGIN otlp_remote
              otlp_remote:
                type: opentelemetry
                inputs: [pg_to_otlp, system_log_to_otlp]
                buffer:
                  type: disk
                  max_size: 268435488
                  when_full: block
                protocol:
                  type: http
                  tls:
                    verify_certificate: false
                  uri: ${OTLP_ENDPOINT}
                  method: post
                  encoding:
                    codec: otlp
                  framing:
                    method: bytes
                  request:
                    headers:
                      content-type: application/x-protobuf
              # END otlp_remote
            """;

    public static final String PATRONI = """
            # Vector pipeline (Patroni-mode slon container).
            #
            # Like STANDALONE but with the additional log sources that exist when slon
            # supervises Patroni: patroni.log (JSON after entrypoint.sh `log.type: json`),
            # entrypoint-out.log (plain text), and etcd.log (zap JSON, mounted into the
            # slon container from the etcd sidecar). No /tmp/server.log source: in Patroni
            # mode Patroni captures the postmaster stderr itself.
            
            data_dir: /tmp/postgres/vector/data
            
            sources:
              pg_csvlog:
                type: file
                include:
                  - /tmp/postgres/log/postgresql-*.csv
                read_from: beginning
                multiline:
                  start_pattern: '^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+ \\w+,'
                  mode: halt_before
                  condition_pattern: '^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+ \\w+,'
                  timeout_ms: 1000
            
              patroni:
                type: file
                include:
                  - /tmp/patroni.log
                read_from: beginning
            
              entrypoint:
                type: file
                include:
                  - /tmp/entrypoint-out.log
                read_from: beginning

              slon:
                type: file
                include:
                  - /tmp/slon.log
                read_from: beginning

              etcd:
                type: file
                include:
                  - /tmp/etcd-logs/etcd.log
                read_from: beginning

            transforms:
              tag_instance:
                type: remap
                inputs: [pg_csvlog]
                source: |
                  .instance_uuid = "${INSTANCE_UUID}"
            
              pg_parse:
                type: remap
                inputs: [tag_instance]
                source: |
                  raw = string!(.message)
                  cols, err = parse_csv(raw)
                  if err != null {
                    .parse_error = "csv parse: " + err
                  } else if length(cols) != 26 {
                    .parse_error = "unexpected csvlog column count: " + to_string(length(cols)) + " (expected 26 for PG17+)"
                  } else {
                    .log_time               = cols[0]
                    .user_name              = cols[1]
                    .database_name          = cols[2]
                    .process_id             = cols[3]
                    .connection_from        = cols[4]
                    .session_id             = cols[5]
                    .session_line_num       = cols[6]
                    .command_tag            = cols[7]
                    .session_start_time     = cols[8]
                    .virtual_transaction_id = cols[9]
                    .transaction_id         = cols[10]
                    .severity               = cols[11]
                    .sql_state_code         = cols[12]
                    .pg_message             = cols[13]
                    .detail                 = cols[14]
                    .hint                   = cols[15]
                    .internal_query         = cols[16]
                    .internal_query_pos     = cols[17]
                    .context                = cols[18]
                    .query                  = cols[19]
                    .query_pos              = cols[20]
                    .location               = cols[21]
                    .application_name       = cols[22]
                    .backend_type           = cols[23]
                    .leader_pid             = cols[24]
                    .query_id               = cols[25]
            
                    .message = .log_time + " [" + .process_id + "] " + .severity + ": " + .pg_message
                  }
            
              pg_to_otlp:
                type: remap
                inputs: [pg_parse]
                source: |
                  parsed_ts = parse_timestamp(string!(.log_time), "%Y-%m-%d %H:%M:%S%.f %Z") ?? now()
                  ts_ns = to_unix_timestamp(parsed_ts, unit: "nanoseconds")
            
                  sev_text = string!(.severity)
                  sev_num = if sev_text == "DEBUG1" || sev_text == "DEBUG2" || sev_text == "DEBUG3" || sev_text == "DEBUG4" || sev_text == "DEBUG5" {
                    5
                  } else if sev_text == "INFO" || sev_text == "LOG" {
                    9
                  } else if sev_text == "NOTICE" {
                    10
                  } else if sev_text == "WARNING" {
                    13
                  } else if sev_text == "ERROR" {
                    17
                  } else if sev_text == "FATAL" || sev_text == "PANIC" {
                    21
                  } else {
                    0
                  }
            
                  attrs = [
                    {"key": "db.user",              "value": {"stringValue": string!(.user_name)}},
                    {"key": "db.process_id",        "value": {"stringValue": string!(.process_id)}},
                    {"key": "client.address",       "value": {"stringValue": string!(.connection_from)}},
                    {"key": "db.session_id",        "value": {"stringValue": string!(.session_id)}},
                    {"key": "pg.session_line_num",  "value": {"stringValue": string!(.session_line_num)}},
                    {"key": "db.operation",         "value": {"stringValue": string!(.command_tag)}},
                    {"key": "pg.session_start_time","value": {"stringValue": string!(.session_start_time)}},
                    {"key": "pg.virtual_xid",       "value": {"stringValue": string!(.virtual_transaction_id)}},
                    {"key": "db.transaction_id",    "value": {"stringValue": string!(.transaction_id)}},
                    {"key": "db.sqlstate",          "value": {"stringValue": string!(.sql_state_code)}},
                    {"key": "pg.detail",            "value": {"stringValue": string!(.detail)}},
                    {"key": "pg.hint",              "value": {"stringValue": string!(.hint)}},
                    {"key": "pg.internal_query",    "value": {"stringValue": string!(.internal_query)}},
                    {"key": "pg.internal_query_pos","value": {"stringValue": string!(.internal_query_pos)}},
                    {"key": "pg.context",           "value": {"stringValue": string!(.context)}},
                    {"key": "db.statement",         "value": {"stringValue": string!(.query)}},
                    {"key": "pg.query_pos",         "value": {"stringValue": string!(.query_pos)}},
                    {"key": "pg.location",          "value": {"stringValue": string!(.location)}},
                    {"key": "db.application",       "value": {"stringValue": string!(.application_name)}},
                    {"key": "db.backend_type",      "value": {"stringValue": string!(.backend_type)}},
                    {"key": "pg.leader_pid",        "value": {"stringValue": string!(.leader_pid)}},
                    {"key": "pg.query_id",          "value": {"stringValue": string!(.query_id)}}
                  ]
            
                  . = {
                    "resourceLogs": [{
                      "resource": {
                        "attributes": [
                          {"key": "service.name",  "value": {"stringValue": "postgresql"}},
                          {"key": "db.system",     "value": {"stringValue": "postgresql"}},
                          {"key": "db.namespace",  "value": {"stringValue": string!(.database_name)}},
                          {"key": "instance.uuid", "value": {"stringValue": string!(.instance_uuid)}}
                        ]
                      },
                      "scopeLogs": [{
                        "scope": {"name": "postgresql.csvlog"},
                        "logRecords": [{
                          "timeUnixNano":         to_string(ts_ns),
                          "observedTimeUnixNano": to_string(ts_ns),
                          "severityNumber":       sev_num,
                          "severityText":         sev_text,
                          "body":                 {"stringValue": string!(.pg_message)},
                          "attributes":           attrs
                        }]
                      }]
                    }]
                  }
            
              tag_patroni:
                type: remap
                inputs: [patroni]
                source: |
                  .instance_uuid = "${INSTANCE_UUID}"
                  .component = "patroni"
                  raw = string!(.message)
                  parsed, err = parse_json(raw)
                  sev = "INFO"
                  body = raw
                  ts_ns = to_unix_timestamp(now(), unit: "nanoseconds")
                  if err == null && is_object(parsed) {
                    if parsed.message != null {
                      body = to_string(parsed.message) ?? raw
                    }
                    if parsed.levelname != null {
                      sev = to_string(parsed.levelname) ?? "INFO"
                    }
                    if parsed.asctime != null {
                      ts_str = to_string(parsed.asctime) ?? ""
                      parsed_ts, terr = parse_timestamp(ts_str, "%Y-%m-%d %H:%M:%S,%f")
                      if terr == null {
                        ts_ns = to_unix_timestamp(parsed_ts, unit: "nanoseconds")
                      }
                    }
                  }
                  .severity_text = sev
                  .severity_number = if sev == "CRITICAL" {
                    21
                  } else if sev == "ERROR" {
                    17
                  } else if sev == "WARNING" {
                    13
                  } else if sev == "INFO" {
                    9
                  } else if sev == "DEBUG" {
                    5
                  } else {
                    0
                  }
                  .body = body
                  .ts_ns = ts_ns
            
              tag_entrypoint:
                type: remap
                inputs: [entrypoint]
                source: |
                  .instance_uuid = "${INSTANCE_UUID}"
                  .component = "entrypoint"
                  .body = string!(.message)
                  .severity_text = "INFO"
                  .severity_number = 9
                  .ts_ns = to_unix_timestamp(now(), unit: "nanoseconds")
            
              tag_slon:
                type: remap
                inputs: [slon]
                source: |
                  .instance_uuid = "${INSTANCE_UUID}"
                  .component = "slon"
                  raw = string!(.message)
                  parsed, err = parse_json(raw)
                  sev = "INFO"
                  body = raw
                  ts_ns = to_unix_timestamp(now(), unit: "nanoseconds")
                  if err == null && is_object(parsed) {
                    if parsed.msg != null {
                      body = to_string(parsed.msg) ?? raw
                    }
                    if parsed.level != null {
                      sev = to_string(parsed.level) ?? "INFO"
                    }
                    if parsed.ts != null {
                      ts_str = to_string(parsed.ts) ?? ""
                      parsed_ts, terr = parse_timestamp(ts_str, "%+")
                      if terr == null {
                        ts_ns = to_unix_timestamp(parsed_ts, unit: "nanoseconds")
                      }
                    }
                  }
                  .severity_text = sev
                  .severity_number = if sev == "SEVERE" {
                    17
                  } else if sev == "WARNING" {
                    13
                  } else if sev == "INFO" {
                    9
                  } else if sev == "CONFIG" || sev == "FINE" || sev == "FINER" || sev == "FINEST" {
                    5
                  } else {
                    0
                  }
                  .body = body
                  .ts_ns = ts_ns
            
              tag_etcd:
                type: remap
                inputs: [etcd]
                source: |
                  .instance_uuid = "${INSTANCE_UUID}"
                  .component = "etcd"
                  raw = string!(.message)
                  parsed, err = parse_json(raw)
                  sev = "info"
                  body = raw
                  ts_ns = to_unix_timestamp(now(), unit: "nanoseconds")
                  if err == null && is_object(parsed) {
                    if parsed.msg != null {
                      body = to_string(parsed.msg) ?? raw
                    }
                    if parsed.level != null {
                      sev = to_string(parsed.level) ?? "info"
                    }
                    if parsed.ts != null {
                      ts_str = to_string(parsed.ts) ?? ""
                      parsed_ts, terr = parse_timestamp(ts_str, "%+")
                      if terr == null {
                        ts_ns = to_unix_timestamp(parsed_ts, unit: "nanoseconds")
                      }
                    }
                  }
                  .severity_text = sev
                  .severity_number = if sev == "fatal" || sev == "panic" || sev == "dpanic" {
                    21
                  } else if sev == "error" {
                    17
                  } else if sev == "warn" {
                    13
                  } else if sev == "info" {
                    9
                  } else if sev == "debug" {
                    5
                  } else {
                    0
                  }
                  .body = body
                  .ts_ns = ts_ns

              system_log_to_otlp:
                type: remap
                inputs: [tag_patroni, tag_entrypoint, tag_slon, tag_etcd]
                source: |
                  # Coerce field reads to concrete types — VRL is strict, and a bare
                  # `.ts_ns` is untyped (E103 if later used in to_string()).
                  ts_ns = to_int(.ts_ns) ?? 0
                  sev_num = to_int(.severity_number) ?? 0
                  sev_text = string!(.severity_text)
                  body = string!(.body)
                  component = string!(.component)
                  instance_uuid = string!(.instance_uuid)
                  . = {
                    "resourceLogs": [{
                      "resource": {
                        "attributes": [
                          {"key": "service.name",  "value": {"stringValue": component}},
                          {"key": "instance.uuid", "value": {"stringValue": instance_uuid}}
                        ]
                      },
                      "scopeLogs": [{
                        "scope": {"name": component},
                        "logRecords": [{
                          "timeUnixNano":         to_string(ts_ns),
                          "observedTimeUnixNano": to_string(ts_ns),
                          "severityNumber":       sev_num,
                          "severityText":         sev_text,
                          "body":                 {"stringValue": body},
                          "attributes":           []
                        }]
                      }]
                    }]
                  }
            
            sinks:
              console_text:
                type: console
                inputs: [pg_parse, tag_patroni, tag_entrypoint, tag_slon, tag_etcd]
                encoding:
                  codec: text
            
              # BEGIN otlp_remote
              otlp_remote:
                type: opentelemetry
                inputs: [pg_to_otlp, system_log_to_otlp]
                buffer:
                  type: disk
                  max_size: 268435488
                  when_full: block
                protocol:
                  type: http
                  tls:
                    verify_certificate: false
                  uri: ${OTLP_ENDPOINT}
                  method: post
                  encoding:
                    codec: otlp
                  framing:
                    method: bytes
                  request:
                    headers:
                      content-type: application/x-protobuf
              # END otlp_remote
            """;

}
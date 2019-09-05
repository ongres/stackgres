/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgbouncer.parameters;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class Blacklist {

  private static final List<String> bl;

  static {
    bl = ImmutableList.<String>builder()
        .add("logfile")
        .add("pidfile")
        .add("listen_addr")
        .add("listen_port")
        .add("unix_socket_dir")
        .add("unix_socket_mode")
        .add("unix_socket_group")
        .add("user")
        .add("auth_file")
        .add("auth_hba_file")
        .add("auth_type")
        .add("auth_query")
        .add("auth_user")
        .add("default_pool_size")
        .add("min_pool_size")
        .add("reserve_pool_size")
        .add("reserve_pool_timeout")
        .add("max_db_connections")
        .add("max_user_connections")
        .add("server_round_robin")
        .add("max_user_connections")
        .add("ignore_startup_parameters")
        .add("disable_pqexec")
        .add("application_name_add_host")
        .add("conffile")
        .add("service_name")
        .add("job_name")
        .add("stats_period")
        .add("syslog")
        .add("syslog_ident")
        .add("syslog_facility")
        .add("log_connections")
        .add("log_disconnections")
        .add("log_pooler_errors")
        .add("log_stats")
        .add("verbose")
        .add("admin_users")
        .add("stats_users")
        .add("server_check_delay")
        .add("server_check_query")
        .add("server_fast_close")
        .add("server_lifetime")
        .add("server_idle_timeout")
        .add("server_connect_timeout")
        .add("server_login_retry")
        .add("client_login_timeout")
        .add("autodb_idle_timeout")
        .add("dns_max_ttl")
        .add("dns_nxdomain_ttl")
        .add("dns_zone_check_period")
        .add("client_tls_sslmode")
        .add("client_tls_key_file")
        .add("client_tls_cert_file")
        .add("client_tls_ca_file")
        .add("client_tls_protocols")
        .add("client_tls_ciphers")
        .add("client_tls_ecdhcurve")
        .add("client_tls_dheparams")
        .add("server_tls_sslmode")
        .add("server_tls_ca_file")
        .add("server_tls_key_file")
        .add("server_tls_cert_file")
        .add("server_tls_protocols")
        .add("server_tls_ciphers")
        .add("query_timeout")
        .add("query_wait_timeout")
        .add("client_idle_timeout")
        .add("idle_transaction_timeout")
        .add("pkt_buf")
        .add("max_packet_size")
        .add("listen_backlog")
        .add("sbuf_loopcnt")
        .add("suspend_timeout")
        .add("tcp_defer_accept")
        .add("tcp_socket_buffer")
        .add("tcp_keepalive")
        .add("tcp_keepcnt")
        .add("tcp_keepidle")
        .add("tcp_keepintvl")
        .build();
  }

  private Blacklist() {}

  public static List<String> getBlacklistParameters() {
    return bl;
  }

}

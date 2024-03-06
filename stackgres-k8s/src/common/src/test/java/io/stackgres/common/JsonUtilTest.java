/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.patroni.PatroniConfig;
import org.junit.jupiter.api.Test;

class JsonUtilTest {

  @Test
  void testMergeJsonObjectsFilteringByModel1() throws Exception {
    ObjectNode value = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            va: test
            vb: false
            vc: 0
            vd: [1, 2, 3]
            """);
    ObjectNode otherValue = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            va: demo
            vb: true
            vc: 1
            vd: [4, 5, 6]
            vl: new
            """);
    var result = JsonUtil.mergeJsonObjectsFilteringByModel(
        value, otherValue, Model.class);
    assertEquals("""
            ---
            vl: "new"
            va: "test"
            vb: false
            vc: 0
            vd:
            - 1
            - 2
            - 3
            """,
            io.stackgres.testutil.JsonUtil.yamlMapper().writeValueAsString(result));
  }

  @Test
  void testMergeJsonObjectsFilteringByModel2() throws Exception {
    ObjectNode value = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            ve:
              ka: 1
              kb: 2
            """);
    ObjectNode otherValue = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            ve:
              ka: 3
              kd: 4
            """);
    var result = JsonUtil.mergeJsonObjectsFilteringByModel(
        value, otherValue, Model.class);
    assertEquals("""
            ---
            ve:
              ka: 1
              kb: 2
            """,
            io.stackgres.testutil.JsonUtil.yamlMapper().writeValueAsString(result));
  }

  @Test
  void testMergeJsonObjectsFilteringByModel3() throws Exception {
    ObjectNode value = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            vf:
              vi: a
              vj: b
              vk: c
            """);
    ObjectNode otherValue = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            vf:
              vi: j
              vj: k
              vk: l
              vm: m
            """);
    var result = JsonUtil.mergeJsonObjectsFilteringByModel(
        value, otherValue, Model.class);
    assertEquals("""
            ---
            vf:
              vm: "m"
              vi: "a"
              vj: "b"
              vk: "c"
            """,
            io.stackgres.testutil.JsonUtil.yamlMapper().writeValueAsString(result));
  }

  @Test
  void testMergeJsonObjectsFilteringByModel4() throws Exception {
    ObjectNode value = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            vg:
            - vi: d
              vj: e
              vk: f
            """);
    ObjectNode otherValue = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            vg:
            - vi: n
              vj: o
              vk: p
              vn: q
            """);
    var result = JsonUtil.mergeJsonObjectsFilteringByModel(
        value, otherValue, Model.class);
    assertEquals("""
            ---
            vg:
            - vi: "d"
              vj: "e"
              vk: "f"
            """,
            io.stackgres.testutil.JsonUtil.yamlMapper().writeValueAsString(result));
  }

  @Test
  void testMergeJsonObjectsFilteringByModel5() throws Exception {
    ObjectNode value = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            vh:
              kc:
                vi: g
                vj: h
                vk: i
            """);
    ObjectNode otherValue = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            vh:
              kc:
                vi: g
                vj: h
                vk: i
                vo: r
              ke:
                vi: t
                vp: u
            """);
    var result = JsonUtil.mergeJsonObjectsFilteringByModel(
        value, otherValue, Model.class);
    assertEquals("""
            ---
            vh:
              kc:
                vo: "r"
                vi: "g"
                vj: "h"
                vk: "i"
              ke:
                vp: "u"
            """,
            io.stackgres.testutil.JsonUtil.yamlMapper().writeValueAsString(result));
  }

  @Test
  void testMergeJsonObjectsFilteringByModelBase() throws Exception {
    ObjectNode value = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            va: test
            vb: false
            vc: 0
            vd: [1, 2, 3]
            ve:
              ka: 1
              kb: 2
            vf:
              vi: a
              vj: b
              vk: c
            vg:
            - vi: d
              vj: e
              vk: f
            vh:
              kc:
                vi: g
                vj: h
                vk: i
            """);
    ObjectNode otherValue = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            va: demo
            vb: true
            vc: 1
            vd: [4, 5, 6]
            ve:
              ka: 3
              kd: 4
            vf:
              vi: j
              vj: k
              vk: l
              vm: m
            vg:
            - vi: n
              vj: o
              vk: p
              vn: q
            vh:
              kc:
                vi: g
                vj: h
                vk: i
                vo: r
              ke:
                vi: t
                vp: u
            vl: new
            """);
    var result = JsonUtil.mergeJsonObjectsFilteringByModel(
        value, otherValue, Model.class);
    assertEquals("""
            ---
            ve:
              ka: 1
              kb: 2
            vf:
              vm: "m"
              vi: "a"
              vj: "b"
              vk: "c"
            vh:
              kc:
                vo: "r"
                vi: "g"
                vj: "h"
                vk: "i"
              ke:
                vp: "u"
            vl: "new"
            va: "test"
            vb: false
            vc: 0
            vd:
            - 1
            - 2
            - 3
            vg:
            - vi: "d"
              vj: "e"
              vk: "f"
            """,
            io.stackgres.testutil.JsonUtil.yamlMapper().writeValueAsString(result));
  }

  @Test
  void testMergeJsonObjectsFilteringByModelPatroniConfig1() throws Exception {
    ObjectNode value = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            postgresql:
              parameters:
                ssl: "on"
            """);
    ObjectNode otherValue = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            postgresql:
              parameters:
                shared_buffers: 32MB
            """);
    var result = JsonUtil.mergeJsonObjectsFilteringByModel(
        value, otherValue, PatroniConfig.class);
    assertEquals("""
            ---
            postgresql:
              parameters:
                ssl: "on"
            """,
            io.stackgres.testutil.JsonUtil.yamlMapper().writeValueAsString(result));
  }

  @Test
  void testMergeJsonObjectsFilteringByModelPatroniConfig2() throws Exception {
    ObjectNode value = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            postgresql:
              parameters:
                archive_command: /bin/true
                archive_mode: 'on'
                autovacuum_max_workers: '3'
                autovacuum_vacuum_cost_delay: 2ms
                autovacuum_work_mem: 512MB
                checkpoint_completion_target: '0.9'
                checkpoint_timeout: 30s
                default_statistics_target: '200'
                default_toast_compression: lz4
                enable_partitionwise_aggregate: 'on'
                enable_partitionwise_join: 'on'
                fsync: 'on'
                hot_standby: 'on'
                huge_pages: 'off'
                jit_inline_above_cost: '-1'
                lc_messages: C
                listen_addresses: localhost
                log_autovacuum_min_duration: 0ms
                log_checkpoints: 'on'
                log_connections: 'on'
                log_destination: stderr
                log_directory: log
                log_disconnections: 'on'
                log_filename: postgres-%M.log
                log_line_prefix: '%t [%p]: db=%d,user=%u,app=%a,client=%h '
                log_lock_waits: 'on'
                log_min_duration_statement: 1s
                log_rotation_age: 30min
                log_rotation_size: 0kB
                log_statement: none
                log_temp_files: 0kB
                log_truncate_on_rotation: 'on'
                logging_collector: 'off'
                maintenance_work_mem: 2GB
                max_locks_per_transaction: '128'
                max_pred_locks_per_transaction: '128'
                max_prepared_transactions: '32'
                max_replication_slots: '20'
                max_wal_senders: '20'
                max_wal_size: 2GB
                min_wal_size: 1GB
                password_encryption: scram-sha-256
                pg_stat_statements.track_utility: 'off'
                port: '5432'
                random_page_cost: '1.5'
                shared_buffers: 32MB
                shared_preload_libraries: pg_stat_statements, auto_explain
                ssl: 'on'
                ssl_cert_file: /etc/ssl/tls.crt
                ssl_key_file: /etc/ssl/tls.key
                superuser_reserved_connections: '8'
                track_activity_query_size: 4kB
                track_commit_timestamp: 'on'
                track_functions: pl
                track_io_timing: 'on'
                wal_compression: 'on'
                wal_keep_size: 1536MB
                wal_level: logical
                wal_log_hints: 'on'
                work_mem: 10MB
              use_slots: true
              use_pg_rewind: true
            ttl: 30
            loop_wait: 10
            retry_timeout: 10
            synchronous_mode: false
            synchronous_mode_strict: false
            check_timeline: true
            """);
    ObjectNode otherValue = (ObjectNode) io.stackgres.testutil.JsonUtil.yamlMapper()
        .readTree("""
            postgresql:
              parameters:
                archive_command: /bin/true
                archive_mode: 'on'
                autovacuum_max_workers: '3'
                autovacuum_vacuum_cost_delay: 2ms
                autovacuum_work_mem: 512MB
                checkpoint_completion_target: '0.9'
                checkpoint_timeout: 30s
                default_statistics_target: '200'
                default_toast_compression: lz4
                enable_partitionwise_aggregate: 'on'
                enable_partitionwise_join: 'on'
                fsync: 'on'
                hot_standby: 'on'
                huge_pages: 'off'
                jit_inline_above_cost: '-1'
                lc_messages: C
                listen_addresses: localhost
                log_autovacuum_min_duration: 0ms
                log_checkpoints: 'on'
                log_connections: 'on'
                log_destination: stderr
                log_directory: log
                log_disconnections: 'on'
                log_filename: postgres-%M.log
                log_line_prefix: '%t [%p]: db=%d,user=%u,app=%a,client=%h '
                log_lock_waits: 'on'
                log_min_duration_statement: 1s
                log_rotation_age: 30min
                log_rotation_size: 0kB
                log_statement: none
                log_temp_files: 0kB
                log_truncate_on_rotation: 'on'
                logging_collector: 'off'
                maintenance_work_mem: 2GB
                max_locks_per_transaction: '128'
                max_pred_locks_per_transaction: '128'
                max_prepared_transactions: '32'
                max_replication_slots: '20'
                max_wal_senders: '20'
                max_wal_size: 2GB
                min_wal_size: 1GB
                password_encryption: scram-sha-256
                pg_stat_statements.track_utility: 'off'
                port: '5432'
                random_page_cost: '1.5'
                shared_buffers: 32MB
                shared_preload_libraries: pg_stat_statements, auto_explain
                ssl: 'on'
                ssl_cert_file: /etc/ssl/tls.crt
                ssl_key_file: /etc/ssl/tls.key
                superuser_reserved_connections: '8'
                track_activity_query_size: 4kB
                track_commit_timestamp: 'on'
                track_functions: pl
                track_io_timing: 'on'
                wal_compression: 'on'
                wal_keep_size: 1536MB
                wal_level: logical
                wal_log_hints: 'on'
                work_mem: 10MB
              use_slots: true
              use_pg_rewind: true
            ttl: 30
            loop_wait: 10
            retry_timeout: 10
            synchronous_mode: false
            synchronous_mode_strict: false
            check_timeline: true
            """);
    var result = JsonUtil.mergeJsonObjectsFilteringByModel(
        value, otherValue, PatroniConfig.class);
    assertEquals("""
            ---
            postgresql:
              parameters:
                archive_command: "/bin/true"
                archive_mode: "on"
                autovacuum_max_workers: "3"
                autovacuum_vacuum_cost_delay: "2ms"
                autovacuum_work_mem: "512MB"
                checkpoint_completion_target: "0.9"
                checkpoint_timeout: "30s"
                default_statistics_target: "200"
                default_toast_compression: "lz4"
                enable_partitionwise_aggregate: "on"
                enable_partitionwise_join: "on"
                fsync: "on"
                hot_standby: "on"
                huge_pages: "off"
                jit_inline_above_cost: "-1"
                lc_messages: "C"
                listen_addresses: "localhost"
                log_autovacuum_min_duration: "0ms"
                log_checkpoints: "on"
                log_connections: "on"
                log_destination: "stderr"
                log_directory: "log"
                log_disconnections: "on"
                log_filename: "postgres-%M.log"
                log_line_prefix: "%t [%p]: db=%d,user=%u,app=%a,client=%h "
                log_lock_waits: "on"
                log_min_duration_statement: "1s"
                log_rotation_age: "30min"
                log_rotation_size: "0kB"
                log_statement: "none"
                log_temp_files: "0kB"
                log_truncate_on_rotation: "on"
                logging_collector: "off"
                maintenance_work_mem: "2GB"
                max_locks_per_transaction: "128"
                max_pred_locks_per_transaction: "128"
                max_prepared_transactions: "32"
                max_replication_slots: "20"
                max_wal_senders: "20"
                max_wal_size: "2GB"
                min_wal_size: "1GB"
                password_encryption: "scram-sha-256"
                pg_stat_statements.track_utility: "off"
                port: "5432"
                random_page_cost: "1.5"
                shared_buffers: "32MB"
                shared_preload_libraries: "pg_stat_statements, auto_explain"
                ssl: "on"
                ssl_cert_file: "/etc/ssl/tls.crt"
                ssl_key_file: "/etc/ssl/tls.key"
                superuser_reserved_connections: "8"
                track_activity_query_size: "4kB"
                track_commit_timestamp: "on"
                track_functions: "pl"
                track_io_timing: "on"
                wal_compression: "on"
                wal_keep_size: "1536MB"
                wal_level: "logical"
                wal_log_hints: "on"
                work_mem: "10MB"
              use_slots: true
              use_pg_rewind: true
            ttl: 30
            loop_wait: 10
            retry_timeout: 10
            synchronous_mode: false
            synchronous_mode_strict: false
            check_timeline: true
            """,
            io.stackgres.testutil.JsonUtil.yamlMapper().writeValueAsString(result));
  }

  public static class Model {
    private String va;
    private Boolean vb;
    private Integer vc;
    private List<String> vd;
    private Map<String, String> ve;
    private InnerModel vf;
    private List<InnerModel> vg;
    private Map<String, InnerModel> vh;
    
    public String getVa() {
      return va;
    }

    public void setVa(String va) {
      this.va = va;
    }

    public Boolean getVb() {
      return vb;
    }

    public void setVb(Boolean vb) {
      this.vb = vb;
    }

    public Integer getVc() {
      return vc;
    }

    public void setVc(Integer vc) {
      this.vc = vc;
    }

    public List<String> getVd() {
      return vd;
    }

    public void setVd(List<String> vd) {
      this.vd = vd;
    }

    public Map<String, String> getVe() {
      return ve;
    }

    public void setVe(Map<String, String> ve) {
      this.ve = ve;
    }

    public InnerModel getVf() {
      return vf;
    }

    public void setVf(InnerModel vf) {
      this.vf = vf;
    }

    public List<InnerModel> getVg() {
      return vg;
    }

    public void setVg(List<InnerModel> vg) {
      this.vg = vg;
    }

    public Map<String, InnerModel> getVh() {
      return vh;
    }

    public void setVh(Map<String, InnerModel> vh) {
      this.vh = vh;
    }
  }

  public static class InnerModel {
    private String vi;
    private String vj;
    private String vk;

    public String getVi() {
      return vi;
    }

    public void setVi(String vi) {
      this.vi = vi;
    }

    public String getVj() {
      return vj;
    }

    public void setVj(String vj) {
      this.vj = vj;
    }

    public String getVk() {
      return vk;
    }

    public void setVk(String vk) {
      this.vk = vk;
    }
  }

}

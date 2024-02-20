/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class JsonUtilTest {

  @Test
  void testMergeJsonObjectsFilteringByModel() throws Exception {
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
        value, otherValue, Model.class, io.stackgres.testutil.JsonUtil.jsonMapper());
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

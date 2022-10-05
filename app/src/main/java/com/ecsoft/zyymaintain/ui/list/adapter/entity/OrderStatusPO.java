package com.ecsoft.zyymaintain.ui.list.adapter.entity;

import java.io.Serializable;

public class OrderStatusPO implements Serializable {
    private Integer sid;
    private String  sname;
    private static final long serialVersionUID=1L;

    public OrderStatusPO() {
    }

    public OrderStatusPO(Integer sid, String sname) {
        this.sid = sid;
        this.sname = sname;
    }

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public String getSname() {
        return sname;
    }

    public void setSname(String sname) {
        this.sname = sname;
    }
}

package com.budimas.wms;

import org.json.JSONException;
import org.json.JSONObject;

public class WmsTransaction {
    public String id;
    public String process;
    public String referenceNo;
    public String palletCode;
    public String stockCode;
    public String productName;
    public String fromRack;
    public String toRack;
    public int qty;
    public String unit;
    public String userName;
    public String note;
    public long createdAt;

    public WmsTransaction() {
        createdAt = System.currentTimeMillis();
        unit = "PCS";
        fromRack = "-";
        toRack = "-";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("process", process);
        json.put("referenceNo", referenceNo);
        json.put("palletCode", palletCode);
        json.put("stockCode", stockCode);
        json.put("productName", productName);
        json.put("fromRack", fromRack);
        json.put("toRack", toRack);
        json.put("qty", qty);
        json.put("unit", unit);
        json.put("userName", userName);
        json.put("note", note);
        json.put("createdAt", createdAt);
        return json;
    }

    public static WmsTransaction fromJson(JSONObject json) {
        WmsTransaction tx = new WmsTransaction();
        tx.id = json.optString("id");
        tx.process = json.optString("process");
        tx.referenceNo = json.optString("referenceNo");
        tx.palletCode = json.optString("palletCode");
        tx.stockCode = json.optString("stockCode");
        tx.productName = json.optString("productName");
        tx.fromRack = json.optString("fromRack", "-");
        tx.toRack = json.optString("toRack", "-");
        tx.qty = json.optInt("qty", 0);
        tx.unit = json.optString("unit", "PCS");
        tx.userName = json.optString("userName");
        tx.note = json.optString("note");
        tx.createdAt = json.optLong("createdAt", System.currentTimeMillis());
        return tx;
    }
}

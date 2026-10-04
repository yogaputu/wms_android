package com.budimas.wms;

import org.json.JSONException;
import org.json.JSONObject;

public class IncomingRecord {
    public String id;
    public String poNumber;
    public String supplier;
    public String date;
    public String skuName;
    public String lotBatch;
    public String expDate;
    public int totalCartons;
    public int capacityPerPallet;
    public long createdAt;

    public IncomingRecord() {
        createdAt = System.currentTimeMillis();
        capacityPerPallet = 40;
    }

    public int palletCount() {
        if (capacityPerPallet <= 0 || totalCartons <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalCartons / (double) capacityPerPallet);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("poNumber", poNumber);
        json.put("supplier", supplier);
        json.put("date", date);
        json.put("skuName", skuName);
        json.put("lotBatch", lotBatch);
        json.put("expDate", expDate);
        json.put("totalCartons", totalCartons);
        json.put("capacityPerPallet", capacityPerPallet);
        json.put("createdAt", createdAt);
        return json;
    }

    public static IncomingRecord fromJson(JSONObject json) {
        IncomingRecord record = new IncomingRecord();
        record.id = json.optString("id");
        record.poNumber = json.optString("poNumber");
        record.supplier = json.optString("supplier");
        record.date = json.optString("date");
        record.skuName = json.optString("skuName");
        record.lotBatch = json.optString("lotBatch");
        record.expDate = json.optString("expDate");
        record.totalCartons = json.optInt("totalCartons", 0);
        record.capacityPerPallet = json.optInt("capacityPerPallet", 40);
        record.createdAt = json.optLong("createdAt", System.currentTimeMillis());
        return record;
    }
}

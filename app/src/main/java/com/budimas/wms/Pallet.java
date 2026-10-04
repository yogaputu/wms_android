package com.budimas.wms;

import org.json.JSONException;
import org.json.JSONObject;

public class Pallet {
    public static final String STATUS_WAITING = "Menunggu Putaway";
    public static final String STATUS_STORED = "Tersimpan";

    public String code;
    public String incomingId;
    public String skuName;
    public String lotBatch;
    public String expDate;
    public int cartonCount;
    public int stockOpnameQtyPcs;
    public String location;
    public String status;
    public long createdAt;
    public long storedAt;

    public Pallet() {
        createdAt = System.currentTimeMillis();
        status = STATUS_WAITING;
        location = "";
    }

    public String toQrPayload() {
        return "BUDIMAS-WMS|PALLET|" + code;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("incomingId", incomingId);
        json.put("skuName", skuName);
        json.put("lotBatch", lotBatch);
        json.put("expDate", expDate);
        json.put("cartonCount", cartonCount);
        json.put("location", location);
        json.put("status", status);
        json.put("createdAt", createdAt);
        json.put("storedAt", storedAt);
        return json;
    }

    public static Pallet fromJson(JSONObject json) {
        Pallet pallet = new Pallet();
        pallet.code = json.optString("code");
        pallet.incomingId = json.optString("incomingId");
        pallet.skuName = json.optString("skuName");
        pallet.lotBatch = json.optString("lotBatch");
        pallet.expDate = json.optString("expDate");
        pallet.cartonCount = json.optInt("cartonCount", 0);
        pallet.location = json.optString("location");
        pallet.status = json.optString("status", STATUS_WAITING);
        pallet.createdAt = json.optLong("createdAt", System.currentTimeMillis());
        pallet.storedAt = json.optLong("storedAt", 0);
        return pallet;
    }
}

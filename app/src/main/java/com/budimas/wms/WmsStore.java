package com.budimas.wms;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WmsStore {
    private static final String PREF_NAME = "budimas_wms_store";
    private static final String KEY_INCOMING = "incoming_records";
    private static final String KEY_PALLETS = "pallets";
    private static final String KEY_TRANSACTIONS = "transactions";
    private static final String KEY_NEXT_PALLET_SEQ = "next_pallet_seq";
    private static final String KEY_LATEST_INCOMING_ID = "latest_incoming_id";

    private final SharedPreferences prefs;

    public WmsStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public IncomingRecord saveIncoming(IncomingRecord record) {
        if (record.id == null || record.id.trim().isEmpty()) {
            record.id = "IN-" + record.createdAt;
        }
        List<IncomingRecord> records = getIncomingRecords();
        records.add(0, record);
        saveIncomingRecords(records);
        prefs.edit().putString(KEY_LATEST_INCOMING_ID, record.id).apply();
        return record;
    }

    public IncomingRecord getLatestIncoming() {
        String latestId = prefs.getString(KEY_LATEST_INCOMING_ID, "");
        for (IncomingRecord record : getIncomingRecords()) {
            if (record.id != null && record.id.equals(latestId)) {
                return record;
            }
        }
        List<IncomingRecord> records = getIncomingRecords();
        return records.isEmpty() ? null : records.get(0);
    }

    public List<IncomingRecord> getIncomingRecords() {
        List<IncomingRecord> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_INCOMING, "[]"));
            for (int i = 0; i < array.length(); i++) {
                result.add(IncomingRecord.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public List<Pallet> createPallets(IncomingRecord incoming) {
        return createPallets(incoming, "User");
    }

    public List<Pallet> createPallets(IncomingRecord incoming, String userName) {
        IncomingRecord savedIncoming = saveIncoming(incoming);
        List<Pallet> existing = getPallets();
        List<Pallet> created = new ArrayList<>();
        int remaining = savedIncoming.totalCartons;
        int capacity = Math.max(1, savedIncoming.capacityPerPallet);
        int nextSeq = prefs.getInt(KEY_NEXT_PALLET_SEQ, 1);

        while (remaining > 0) {
            Pallet pallet = new Pallet();
            pallet.code = String.format(Locale.US, "PLT-%06d", nextSeq++);
            pallet.incomingId = savedIncoming.id;
            pallet.skuName = savedIncoming.skuName;
            pallet.lotBatch = savedIncoming.lotBatch;
            pallet.expDate = savedIncoming.expDate;
            pallet.cartonCount = Math.min(capacity, remaining);
            remaining -= pallet.cartonCount;
            existing.add(0, pallet);
            created.add(pallet);
        }

        savePallets(existing);
        prefs.edit().putInt(KEY_NEXT_PALLET_SEQ, nextSeq).apply();
        for (Pallet pallet : created) {
            WmsTransaction tx = new WmsTransaction();
            tx.process = "Receiving";
            tx.referenceNo = savedIncoming.poNumber == null || savedIncoming.poNumber.trim().isEmpty()
                    ? savedIncoming.id
                    : savedIncoming.poNumber;
            tx.palletCode = pallet.code;
            tx.stockCode = "";
            tx.productName = pallet.skuName;
            tx.fromRack = "-";
            tx.toRack = "Pallet dibuat";
            tx.qty = pallet.cartonCount;
            tx.unit = "Karton";
            tx.userName = userName;
            tx.note = "Pallet dibuat dari receiving manual.";
            recordTransaction(tx);
        }
        return created;
    }

    public List<Pallet> getPallets() {
        List<Pallet> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_PALLETS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                result.add(Pallet.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public Pallet findPallet(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.US);
        for (Pallet pallet : getPallets()) {
            if (pallet.code != null && pallet.code.toUpperCase(Locale.US).equals(normalized)) {
                return pallet;
            }
        }
        return null;
    }

    public void updatePallet(Pallet updated) {
        List<Pallet> pallets = getPallets();
        for (int i = 0; i < pallets.size(); i++) {
            if (pallets.get(i).code.equals(updated.code)) {
                pallets.set(i, updated);
                savePallets(pallets);
                return;
            }
        }
        pallets.add(0, updated);
        savePallets(pallets);
    }

    public int countStored() {
        int count = 0;
        for (Pallet pallet : getPallets()) {
            if (Pallet.STATUS_STORED.equals(pallet.status)) {
                count++;
            }
        }
        return count;
    }

    public int countWaiting() {
        int count = 0;
        for (Pallet pallet : getPallets()) {
            if (!Pallet.STATUS_STORED.equals(pallet.status)) {
                count++;
            }
        }
        return count;
    }

    public int totalCartons() {
        int total = 0;
        for (Pallet pallet : getPallets()) {
            total += pallet.cartonCount;
        }
        return total;
    }

    public void recordTransaction(WmsTransaction transaction) {
        if (transaction == null) {
            return;
        }
        if (transaction.id == null || transaction.id.trim().isEmpty()) {
            transaction.id = "TX-" + transaction.createdAt + "-" + Math.abs((transaction.process + transaction.referenceNo + transaction.palletCode + transaction.stockCode).hashCode());
        }
        List<WmsTransaction> transactions = getTransactions();
        transactions.add(0, transaction);
        saveTransactions(transactions);
    }

    public List<WmsTransaction> getTransactions() {
        List<WmsTransaction> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_TRANSACTIONS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                result.add(WmsTransaction.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    private void saveIncomingRecords(List<IncomingRecord> records) {
        JSONArray array = new JSONArray();
        for (IncomingRecord record : records) {
            try {
                array.put(record.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_INCOMING, array.toString()).apply();
    }

    private void savePallets(List<Pallet> pallets) {
        JSONArray array = new JSONArray();
        for (Pallet pallet : pallets) {
            try {
                array.put(pallet.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_PALLETS, array.toString()).apply();
    }

    private void saveTransactions(List<WmsTransaction> transactions) {
        JSONArray array = new JSONArray();
        int limit = Math.min(transactions.size(), 300);
        for (int i = 0; i < limit; i++) {
            try {
                array.put(transactions.get(i).toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply();
    }
}

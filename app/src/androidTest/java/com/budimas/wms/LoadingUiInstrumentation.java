package com.budimas.wms;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit emulator-only UI smoke runner; all API calls replaced before navigation. */
public class LoadingUiInstrumentation extends Instrumentation {
    private MainActivity activity;
    private FakeApi api;
    private int checks;

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            activity = (MainActivity) startActivitySync(new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            api = new FakeApi(new AuthSession(activity));
            onUi(() -> {
                field("apiClient").set(activity, api);
                activity.getSharedPreferences("budimas_wms_ui", Context.MODE_PRIVATE).edit().putString("theme_mode", "Gelap").commit();
                call("showLoadingAssignmentForm");
            });
            awaitText("Jadwal Loading");
            onUi(() -> {
                check(!text("Tampilkan Barang Lolos Checker").isEnabled(), "Cannot submit incomplete selection");
                TextView choice = description("Armada:");
                check(Color.luminance(choice.getCurrentTextColor()) > 0.3, "Dark theme dropdown text must be readable");
            });
            screenshot("loading-dark");
            choose("Armada:", "UNIT-305", 305);
            choose("Driver:", "Driver 300", 300);
            onUi(() -> description("Tanggal pengiriman:").performClick());
            awaitClass(DatePicker.class);
            screenshot("loading-calendar");
            onUi(() -> {
                DatePicker calendar = first(DatePicker.class);
                calendar.updateDate(2026, 9, 2);
                text("Pilih").performClick();
            });
            awaitText("02/10/2026");
            screenshot("loading-selected");
            onUi(() -> {
                check(text("Tampilkan Barang Lolos Checker").isEnabled(), "All required fields enable submit");
                text("Tampilkan Barang Lolos Checker").performClick();
            });
            awaitText("Belum ada barang lolos checker");
            onUi(() -> {
                check(api.selection.getInt("id_armada") == 305, "Selected master vehicle ID sent");
                check(api.selection.getInt("id_driver") == 300, "Selected master driver ID sent");
                check("2026-10-02".equals(api.selection.getString("delivery_date")), "ISO calendar date sent");
                check(api.readyCalls == 1, "Only one read request; no loading mutations");
                text("Ganti Armada / Driver / Tanggal").performClick();
            });
            awaitText("Jadwal Loading");
            onUi(() -> {
                check(description("Armada:").getText().toString().contains("305"), "Retain previous vehicle");
                check(description("Driver:").getText().toString().contains("300"), "Retain previous driver");
                activity.getSharedPreferences("budimas_wms_ui", Context.MODE_PRIVATE).edit().putString("theme_mode", "Terang").commit();
                call("showLoadingAssignmentForm");
            });
            awaitText("Jadwal Loading");
            screenshot("loading-light");
            onUi(() -> {
                check(Color.luminance(description("Armada:").getCurrentTextColor()) < 0.1, "Light theme field text contrast");
                api.empty = true;
                call("showLoadingAssignmentForm");
            });
            awaitText("Master armada atau driver belum tersedia");
            onUi(() -> check(!text("Tampilkan Barang Lolos Checker").isEnabled(), "Empty masters cannot submit"));
            result.putString("stream", "PASS: " + checks + " native UI assertions; dark/light, search across 305 rows, calendar, retained selection, empty state; API fully mocked.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            try { screenshot("loading-ui-failure"); } catch (Exception ignored) { }
            result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void choose(String fieldName, String queryText, int expected) throws Exception {
        onUi(() -> description(fieldName).performClick());
        awaitClass(ListView.class);
        onUi(() -> {
            check(first(ListView.class).getAdapter().getCount() == 305, "Every master row available");
            first(EditText.class).setText("no-matching-record");
            check(first(ListView.class).getAdapter().getCount() == 0, "No-match search state");
            first(EditText.class).setText(queryText);
            ListView list = first(ListView.class);
            check(list.getAdapter().getCount() == 1, "Search finds the final rows without cap");
            JSONObject item = (JSONObject) list.getAdapter().getItem(0);
            check(item.getInt("id") == expected, "Search result maps to exact master ID");
        });
        screenshot(fieldName.startsWith("Armada") ? "loading-search-armada" : "loading-search-driver");
        onUi(() -> { ListView list = first(ListView.class); list.performItemClick(list.getChildAt(0), 0, list.getAdapter().getItemId(0)); });
    }

    private Field field(String name) throws Exception { Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f; }
    private void call(String name) throws Exception { Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity); }
    private void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); checks++; }
    private interface Work { void run() throws Exception; }
    private void onUi(Work work) throws Exception {
        Throwable[] error={null};runOnMainSync(() -> {try {work.run();} catch(Throwable e){error[0]=e;}});
        if(error[0]!=null) throw new Exception(error[0]);
        waitForIdleSync();
    }
    private List<View> views() {
        List<View> all=new ArrayList<>();
        for(View root:WindowInspector.getGlobalWindowViews()) append(root,all);
        return all;
    }
    private void append(View view,List<View> all) {all.add(view);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)append(group.getChildAt(i),all);}}
    private TextView text(String expected) {
        for(View view:views())if(view instanceof TextView && ((TextView)view).getText().toString().equalsIgnoreCase(expected))return (TextView)view;
        for(View view:views())if(view instanceof TextView && ((TextView)view).getText().toString().startsWith(expected))return (TextView)view;
        throw new AssertionError("Missing text: "+expected);
    }
    private TextView description(String prefix) {for(View view:views())if(view instanceof TextView && view.getContentDescription()!=null && view.getContentDescription().toString().startsWith(prefix))return (TextView)view;throw new AssertionError(prefix);}
    private <T extends View> T first(Class<T> type) {for(View view:views())if(type.isInstance(view))return type.cast(view);throw new AssertionError("Missing view: "+type.getSimpleName());}
    private void awaitText(String expected) throws Exception {await(() -> text(expected));}
    private void awaitClass(Class<? extends View> type) throws Exception {await(() -> first(type));}
    private void await(Work work) throws Exception {
        Exception last=null;
        for(int i=0;i<100;i++){try{onUi(work);return;}catch(Exception e){last=e;Thread.sleep(100);}}
        throw last;
    }
    private void screenshot(String name) throws Exception {
        waitForIdleSync();Thread.sleep(250);
        Bitmap bitmap=getUiAutomation().takeScreenshot();
        if(bitmap==null)throw new AssertionError("Screenshot unavailable");
        File output=new File(getTargetContext().getExternalFilesDir(null),name+".png");
        try(FileOutputStream stream=new FileOutputStream(output)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}bitmap.recycle();
    }
    private static class FakeApi extends BudimasApiClient {
        volatile JSONObject selection;volatile int readyCalls;volatile boolean empty;
        FakeApi(AuthSession session){super(session);}
        @Override public JSONObject getLoadingOptions() throws Exception {
            JSONArray vehicles=new JSONArray(),drivers=new JSONArray();
            if(!empty)for(int i=1;i<=305;i++){
                vehicles.put(new JSONObject().put("id",i).put("id_armada",i).put("kode","UNIT-"+i).put("no_pelat","B "+i+" UJI").put("nama","Armada "+i).put("nama_cabang","Solo").put("status_operasional","AVAILABLE"));
                drivers.put(new JSONObject().put("id",i).put("id_driver",i).put("nama","Driver "+i).put("nama_cabang","Solo"));
            }
            return new JSONObject().put("vehicles",vehicles).put("drivers",drivers);
        }
        @Override public JSONObject getWmsReadyToLoad(String search,JSONObject selected) throws Exception {selection=selected;readyCalls++;return new JSONObject().put("data",new JSONArray());}
    }
}

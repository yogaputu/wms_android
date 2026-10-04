package com.budimas.wms;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public class AuthSession {
    public static final String API_BASE_URL = BuildConfig.API_BASE_URL;

    private static final String PREF_NAME = "budimas_wms_auth";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_USER_JSON = "user_json";
    private static final String KEY_PERMISSIONS_JSON = "permissions_json";
    private static final String KEY_MENUS_JSON = "menus_json";

    private final SharedPreferences prefs;

    public AuthSession(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(JSONObject loginData) {
        String token = firstString(loginData, "access_token", "token", "accessToken");
        JSONObject user = firstObject(loginData, "user", "data_user", "detail_user");
        JSONArray permissions = firstArray(loginData, "permissions", "fitur");
        JSONArray menus = firstArray(loginData, "menus", "menu");

        if (user == null && loginData.has("id_user")) {
            user = loginData;
        }

        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_TOKEN_TYPE, firstString(loginData, "token_type", "tokenType"))
                .putString(KEY_USER_JSON, user == null ? "{}" : user.toString())
                .putString(KEY_PERMISSIONS_JSON, permissions == null ? "[]" : permissions.toString())
                .putString(KEY_MENUS_JSON, menus == null ? "[]" : menus.toString())
                .apply();
    }

    public void updateProfile(JSONObject profileData) {
        JSONObject user = firstObject(profileData, "user", "data_user", "detail_user");
        JSONArray permissions = firstArray(profileData, "permissions", "fitur");
        JSONArray menus = firstArray(profileData, "menus", "menu");

        if (user == null && profileData.has("id_user")) {
            user = profileData;
        }

        SharedPreferences.Editor editor = prefs.edit();
        if (user != null) {
            editor.putString(KEY_USER_JSON, user.toString());
        }
        if (permissions != null) {
            editor.putString(KEY_PERMISSIONS_JSON, permissions.toString());
        }
        if (menus != null) {
            editor.putString(KEY_MENUS_JSON, menus.toString());
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return !getToken().isEmpty();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getUserName() {
        JSONObject user = getUser();
        return firstString(user, "nama", "nama_user", "username", "email", "user_email");
    }

    public String getBranchName() {
        JSONObject user = getUser();
        JSONObject branch = user.optJSONObject("cabang");
        if (branch != null) {
            String name = firstString(branch, "nama", "nama_cabang", "name");
            if (!name.isEmpty()) {
                return name;
            }
        }
        return firstString(user, "nama_cabang", "cabang_nama", "branch_name");
    }

    public String getRoleLabel() {
        JSONObject user = getUser();
        JSONObject jabatan = user.optJSONObject("jabatan");
        if (jabatan != null) {
            String name = firstString(jabatan, "nama", "name");
            if (!name.isEmpty()) {
                return name;
            }
        }
        return firstString(user, "nama_jabatan", "role_code", "id_jabatan");
    }

    public JSONObject getUser() {
        try {
            return new JSONObject(prefs.getString(KEY_USER_JSON, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public static String firstString(JSONObject object, String... keys) {
        if (object == null) {
            return "";
        }
        for (String key : keys) {
            String value = object.optString(key, "");
            if (value != null && !"null".equalsIgnoreCase(value) && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    public static JSONObject firstObject(JSONObject object, String... keys) {
        if (object == null) {
            return null;
        }
        for (String key : keys) {
            JSONObject value = object.optJSONObject(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static JSONArray firstArray(JSONObject object, String... keys) {
        if (object == null) {
            return null;
        }
        for (String key : keys) {
            JSONArray value = object.optJSONArray(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}

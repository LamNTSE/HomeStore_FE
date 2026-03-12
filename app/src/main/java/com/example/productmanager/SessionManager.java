package com.example.productmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SessionManager {
    private static final String PREF_NAME = "homestore_session";
    private static final String KEY_TOKEN = "auth_token";

    private SessionManager() {}

    public static void saveToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, "");
    }

    public static boolean isLoggedIn(Context context) {
        return !getToken(context).isEmpty();
    }

    public static String getUserRole(Context context) {
        return extractRoleFromToken(getToken(context));
    }

    public static boolean isAdmin(Context context) {
        return "admin".equalsIgnoreCase(getUserRole(context));
    }

    private static String extractRoleFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return "";
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return "";
        }

        try {
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(payload);

            String[] roleKeys = new String[] {
                    "role",
                    "roles",
                    "http://schemas.microsoft.com/ws/2008/06/identity/claims/role"
            };

            for (String key : roleKeys) {
                String roleValue = json.optString(key, "");
                if (!roleValue.isEmpty()) {
                    return roleValue.toLowerCase(Locale.ROOT);
                }
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    public static int getUserId(Context context) {
        String token = getToken(context);
        if (token == null || token.trim().isEmpty()) return -1;

        String[] parts = token.split("\\.");
        if (parts.length < 2) return -1;

        try {
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(payload);

            // .NET uses "nameid" or the full claim URI for NameIdentifier
            String[] idKeys = new String[] {
                    "nameid",
                    "sub",
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"
            };
            for (String key : idKeys) {
                String val = json.optString(key, "");
                if (!val.isEmpty()) {
                    return Integer.parseInt(val);
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public static void clear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}

package com.gradient.mapbox.mapboxgradient;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Set;

/**
 * SharedPreferences Utilities
 */
public final class Preferences {

    //Saved Preferences
    public static final String SP_VOLUNTEERS_ADDED = "sp_volunteers_added";
    public static final String SP_CONTACTS_ADDED = "sp_contacts_added";

    private static Preferences mInstance = null;
    private SharedPreferences sharedPreferences;

    private Preferences(Context context) {
        sharedPreferences = context.getSharedPreferences("map_test_app", Activity.MODE_PRIVATE);
        sharedPreferences.edit();
    }

    public static void init(Context context) {
        mInstance = new Preferences(context);

    }

    public static Preferences getInstance() {
        if (mInstance == null) {
            throw new RuntimeException(
                    "Must run init(Application application) before an instance can be obtained");
        }
        return mInstance;
    }


    public boolean save(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        return editor.commit();
    }


    public String getString(String key) {
        return sharedPreferences.getString(key, "");
    }


    public boolean save(HashMap<String, String> valuesMap) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String value = "";
        for (String key : valuesMap.keySet()) {
            value = valuesMap.get(key);
            editor.putString(key, value);
        }
        return editor.commit();
    }

    public boolean save(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void clear(Context context) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public void remove(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }


    public boolean save(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public boolean save(String key, float value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, value);
        return editor.commit();
    }


    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    public boolean save(String key,  Set<String> value) {
        if (value == null) return false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(key, value);
        return editor.commit();
    }


    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        return sharedPreferences.getStringSet(key, defaultValue);
    }

}


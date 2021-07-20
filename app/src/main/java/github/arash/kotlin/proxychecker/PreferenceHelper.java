package github.arash.kotlin.proxychecker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceHelper {
    private static SharedPreferences pref;
    private static PreferenceHelper prefHelper;

    public PreferenceHelper(Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Boolean containsKey(String key) {
        return pref.contains(key);
    }

    public void clearPrefs() {
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.apply();
    }

    public static void initialize(Context appContext) {
        if (appContext == null) {
            throw new NullPointerException("Provided application context is null");
        }
        if (prefHelper == null) {
            synchronized (PreferenceHelper.class) {
                if (prefHelper == null) {
                    prefHelper = new PreferenceHelper(appContext);
                }
            }
        }
    }

    public static PreferenceHelper getInstance() {
        if (prefHelper == null) {
            throw new IllegalStateException(
                    "SharedPrefsManager is not initialized, call initialize(applicationContext) " +
                            "static method first");
        }
        return prefHelper;
    }

    public void setString(String key, String value) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void removeKey(String key) {
        pref.edit().remove(key).apply();
    }

    public String getString(String key, String defValue) {
        return pref.getString(key, defValue);
    }

    public Boolean getBoolean(String key) {
        return pref.getBoolean(key, false);
    }

    public void setBoolean(String key, Boolean isBoolean) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(key, isBoolean);
        editor.apply();

    }


    public void setInt(String key, int value) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public void loggedOut() {
        clearPrefs();
    }

    public int getInt(String key) {
        return pref.getInt(key, 0);
    }

    public void toPrev8(Activity activity, String key, Boolean isBoolean) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, isBoolean);
        editor.apply();
    }

    public boolean fromPrev8(Activity activity, String key) {
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, false);
    }
}
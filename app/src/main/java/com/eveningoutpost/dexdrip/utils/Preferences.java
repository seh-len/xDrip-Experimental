package com.eveningoutpost.dexdrip.utils;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.Services.XDripViewer;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.PebbleSync;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.xDripWidget;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nightscout.core.barcode.NSBarcodeConfig;

import net.tribe7.common.base.Joiner;

import java.net.URI;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class Preferences extends PreferenceActivity {
    private static final String TAG = "PREFS";
    private AllPrefsFragment preferenceFragment;


    private void refreshFragments() {
        preferenceFragment = new AllPrefsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                preferenceFragment).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (scanResult == null || scanResult.getContents() == null) {
            return;
        }
        if (scanResult.getFormatName().equals("QR_CODE")) {
            NSBarcodeConfig barcode = new NSBarcodeConfig(scanResult.getContents());
            if (barcode.hasMongoConfig()) {
                if (barcode.getMongoUri().isPresent()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("cloud_storage_mongodb_uri", barcode.getMongoUri().get());
                    editor.putString("cloud_storage_mongodb_collection", barcode.getMongoCollection().or("entries"));
                    editor.putString("cloud_storage_mongodb_device_status_collection", barcode.getMongoDeviceStatusCollection().or("devicestatus"));
                    editor.putBoolean("cloud_storage_mongodb_enable", true);
                    editor.apply();
                }
                if (barcode.hasApiConfig()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("cloud_storage_api_enable", true);
                    editor.putString("cloud_storage_api_base", Joiner.on(' ').join(barcode.getApiUris()));
                    editor.apply();
                } else {
                    prefs.edit().putBoolean("cloud_storage_api_enable", false).apply();
                }
            }
            if (barcode.hasApiConfig()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cloud_storage_api_enable", true);
                editor.putString("cloud_storage_api_base", Joiner.on(' ').join(barcode.getApiUris()));
                editor.apply();
            } else {
                prefs.edit().putBoolean("cloud_storage_api_enable", false).apply();
            }

            if (barcode.hasMqttConfig()) {
                if (barcode.getMqttUri().isPresent()) {
                    URI uri = URI.create(barcode.getMqttUri().or(""));
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":");
                        if (userInfo.length == 2) {
                            String endpoint = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
                            if (userInfo[0].length() > 0 && userInfo[1].length() > 0) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("cloud_storage_mqtt_endpoint", endpoint);
                                editor.putString("cloud_storage_mqtt_user", userInfo[0]);
                                editor.putString("cloud_storage_mqtt_password", userInfo[1]);
                                editor.putBoolean("cloud_storage_mqtt_enable", true);
                                editor.apply();
                            }
                        }
                    }
                }
            } else {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cloud_storage_mqtt_enable", false);
                editor.apply();
            }
        } else if (scanResult.getFormatName().equals("CODE_128")) {
            Log.d(TAG, "Setting serial number to: " + scanResult.getContents());
            prefs.edit().putString("share_key", scanResult.getContents()).apply();
        }
        refreshFragments();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceFragment = new AllPrefsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                preferenceFragment).commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.pref_general);

    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AllPrefsFragment.class.getName().equals(fragmentName)){ return true; }
        return false;
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };
    private static Preference.OnPreferenceChangeListener sBindNumericPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (isNumeric(stringValue)) {
                preference.setSummary(stringValue);
                return true;
            }
            return false;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }
    private static void bindPreferenceSummaryToValueAndEnsureNumeric(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindNumericPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    // This class is used by android, so it must stay public although this will compile when it is private.
    public static class AllPrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            addPreferencesFromResource(R.xml.pref_license);
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("highValue"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("lowValue"));
            bindPreferenceSummaryToValue(findPreference("units"));

            addPreferencesFromResource(R.xml.pref_notifications);
            bindPreferenceSummaryToValue(findPreference("bg_alert_profile"));
            bindPreferenceSummaryToValue(findPreference("calibration_notification_sound"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("calibration_snooze"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("bg_unclear_readings_minutes"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("disable_alerts_stale_data_minutes"));
            bindPreferenceSummaryToValue(findPreference("falling_bg_val"));
            bindPreferenceSummaryToValue(findPreference("rising_bg_val"));
            bindPreferenceSummaryToValue(findPreference("other_alerts_sound"));
            bindPreferenceSummaryToValueAndEnsureNumeric(findPreference("other_alerts_snooze"));

            addPreferencesFromResource(R.xml.pref_data_source);


            addPreferencesFromResource(R.xml.pref_data_sync);
            setupBarcodeConfigScanner();
            setupBarcodeShareScanner();
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_uri"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_mongodb_device_status_collection"));
            bindPreferenceSummaryToValue(findPreference("cloud_storage_api_base"));

            addPreferencesFromResource(R.xml.pref_advanced_settings);
            addPreferencesFromResource(R.xml.pref_community_help);

            bindTTSListener();
            bindBgMissedAlertsListener();
            final Preference collectionMethod = findPreference("dex_collection_method");
            final Preference displayBridgeBatt = findPreference("display_bridge_battery");
            final Preference runInForeground = findPreference("run_service_in_foreground");
            final Preference wifiRecievers = findPreference("wifi_recievers_addresses");
            final Preference xDripViewerNsAdresses = findPreference("xdrip_viewer_ns_addresses");
            final Preference predictiveBG = findPreference("predictive_bg");
            final Preference interpretRaw = findPreference("interpret_raw");

            final Preference shareKey = findPreference("share_key");
            shareKey.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    prefs.edit().remove("dexcom_share_session_id").apply();
                    return true;
                }
            });

            Preference.OnPreferenceChangeListener shareTokenResettingListener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    prefs.edit().remove("dexcom_share_session_id").apply();
                    return true;
                }
            };

            final Preference sharePassword = findPreference("dexcom_account_password");
            sharePassword.setOnPreferenceChangeListener(shareTokenResettingListener);
            final Preference shareAccountName = findPreference("dexcom_account_name");
            shareAccountName.setOnPreferenceChangeListener(shareTokenResettingListener);

            final Preference scanShare = findPreference("scan_share2_barcode");
            final EditTextPreference transmitterId = (EditTextPreference) findPreference("dex_txid");
            final Preference pebbleSync = findPreference("broadcast_to_pebble");
            final PreferenceCategory collectionCategory = (PreferenceCategory) findPreference("collection_category");
            final PreferenceCategory otherCategory = (PreferenceCategory) findPreference("other_category");
            final PreferenceScreen calibrationAlertsScreen = (PreferenceScreen) findPreference("calibration_alerts_screen");
            final PreferenceCategory alertsCategory = (PreferenceCategory) findPreference("alerts_category");
            final Preference disableAlertsStaleDataMinutes = findPreference("disable_alerts_stale_data_minutes");

            disableAlertsStaleDataMinutes.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!isNumeric(newValue.toString())) {
                        return false;
                    }
                    if ((Integer.parseInt(newValue.toString())) < 10 ) {
                        Toast.makeText(preference.getContext(),
                                "Value must be at least 10 minutes", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });
            Log.d(TAG, prefs.getString("dex_collection_method", "BluetoothWixel"));
            if(prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexcomShare") != 0) {
                collectionCategory.removePreference(shareKey);
                collectionCategory.removePreference(scanShare);
                otherCategory.removePreference(interpretRaw);
                alertsCategory.addPreference(calibrationAlertsScreen);
            } else {
                otherCategory.removePreference(predictiveBG);
                alertsCategory.removePreference(calibrationAlertsScreen);
                prefs.edit().putBoolean("calibration_notifications", false).apply();
            }


            if ((prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("WifiWixel") != 0)
                    && (prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("WifiBlueToothWixel") != 0)) {
                String receiversIpAddresses;
                receiversIpAddresses = prefs.getString("wifi_recievers_addresses", "");
                // only hide if non wifi wixel mode and value not previously set to cope with
                // dynamic mode changes. jamorham
                if (receiversIpAddresses == null || receiversIpAddresses.equals("")) {
                    collectionCategory.removePreference(wifiRecievers);
                }
            }

            if(prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexbridgeWixel") != 0) {
                collectionCategory.removePreference(transmitterId);
                collectionCategory.removePreference(displayBridgeBatt);
            }

            if(prefs.getString("dex_collection_method", "BluetoothWixel").compareTo("DexcomG5") == 0) {
                collectionCategory.addPreference(transmitterId);
            }
            pebbleSync.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Context context = preference.getContext();
                    if ((Boolean) newValue) {
                        context.startService(new Intent(context, PebbleSync.class));
                    } else {
                        context.stopService(new Intent(context, PebbleSync.class));
                    }
                    return true;
                }
            });

            bindWidgetUpdater();

            bindPreferenceSummaryToValue(collectionMethod);
            bindPreferenceSummaryToValue(shareKey);
            bindPreferenceSummaryToValue(wifiRecievers);
            bindPreferenceSummaryToValue(xDripViewerNsAdresses);
            bindPreferenceSummaryToValue(transmitterId);
            transmitterId.getEditText().setFilters(new InputFilter[]{new InputFilter.AllCaps()});
            collectionMethod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(((String) newValue).compareTo("DexcomShare") != 0) { // NOT USING SHARE
                        collectionCategory.removePreference(shareKey);
                        collectionCategory.removePreference(scanShare);
                        otherCategory.removePreference(interpretRaw);
                        otherCategory.addPreference(predictiveBG);
                        alertsCategory.addPreference(calibrationAlertsScreen);
                    } else {
                        collectionCategory.addPreference(shareKey);
                        collectionCategory.addPreference(scanShare);
                        otherCategory.addPreference(interpretRaw);
                        otherCategory.removePreference(predictiveBG);
                        alertsCategory.removePreference(calibrationAlertsScreen);
                        prefs.edit().putBoolean("calibration_notifications", false).apply();
                    }

                    if (((String) newValue).compareTo("BluetoothWixel") != 0
                            && ((String) newValue).compareTo("DexcomShare") != 0
                            && ((String) newValue).compareTo("DexbridgeWixel") != 0
                            && ((String) newValue).compareTo("WifiBlueToothWixel") != 0) {
                        collectionCategory.removePreference(runInForeground);
                    } else {
                        collectionCategory.addPreference(runInForeground);
                    }

                    // jamorham always show wifi receivers option if populated as we may switch modes dynamically
                    if((((String) newValue).compareTo("WifiWixel") != 0)
                            && (((String) newValue).compareTo("WifiBlueToothWixel") != 0)) {
                        String receiversIpAddresses;
                        receiversIpAddresses = prefs.getString("wifi_recievers_addresses", "");
                        if(receiversIpAddresses == null || receiversIpAddresses.equals("") ) {
                            collectionCategory.removePreference(wifiRecievers);
                        } else {
                            collectionCategory.addPreference(wifiRecievers);
                        }
                    } else {
                        collectionCategory.addPreference(wifiRecievers);
                    }

                    if(((String) newValue).compareTo("DexbridgeWixel") != 0) {
                        collectionCategory.removePreference(transmitterId);
                        collectionCategory.removePreference(displayBridgeBatt);
                    } else {
                        collectionCategory.addPreference(transmitterId);
                        collectionCategory.addPreference(displayBridgeBatt);
                    }

                    if(((String) newValue).compareTo("DexcomG5") == 0) {
                        collectionCategory.addPreference(transmitterId);
                    }

                    String stringValue = newValue.toString();
                    if (preference instanceof ListPreference) {
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(stringValue);
                        preference.setSummary(
                                index >= 0
                                        ? listPreference.getEntries()[index]
                                        : null);

                    } else if (preference instanceof RingtonePreference) {
                        if (TextUtils.isEmpty(stringValue)) {
                            preference.setSummary(R.string.pref_ringtone_silent);

                        } else {
                            Ringtone ringtone = RingtoneManager.getRingtone(
                                    preference.getContext(), Uri.parse(stringValue));
                            if (ringtone == null) {
                                preference.setSummary(null);
                            } else {
                                String name = ringtone.getTitle(preference.getContext());
                                preference.setSummary(name);
                            }
                        }
                    } else {
                        preference.setSummary(stringValue);
                    }
                    if(preference.getKey().equals("dex_collection_method")) {
                        CollectionServiceStarter.restartCollectionService(preference.getContext(), (String) newValue);
                    } else {
                        CollectionServiceStarter.restartCollectionService(preference.getContext());
                    }
                    return true;
                }
            });
            
            // Remove all the parts that are not needed in xDripViewer (doing it all in one place to avoid having many ifs)
            if(XDripViewer.isxDripViewerMode(getActivity())) {
                collectionCategory.removePreference(collectionMethod);
                collectionCategory.removePreference(shareKey);
                collectionCategory.removePreference(scanShare);
                collectionCategory.removePreference(wifiRecievers);
                collectionCategory.removePreference(transmitterId);
                collectionCategory.removePreference(displayBridgeBatt);
                
                final PreferenceCategory dataSyncCategory = (PreferenceCategory) findPreference("dataSync");
                final Preference autoConfigure = findPreference("auto_configure");
                final Preference cloudStorageMongo =  findPreference("cloud_storage_mongo");
                final Preference cloudStorageApi =  findPreference("cloud_storage_api");
                final Preference dexcomServerUploadScreen =  findPreference("dexcom_server_upload_screen");
                final Preference xDripViewerUploadMode =  findPreference("xDripViewer_upload_mode");
                
                dataSyncCategory.removePreference(autoConfigure);
                dataSyncCategory.removePreference(cloudStorageMongo);
                dataSyncCategory.removePreference(cloudStorageApi);
                dataSyncCategory.removePreference(dexcomServerUploadScreen);
                dataSyncCategory.removePreference(xDripViewerUploadMode);
                
            } else {
                collectionCategory.removePreference(xDripViewerNsAdresses);
            }
        }

        private void bindWidgetUpdater() {
            findPreference("widget_range_lines").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("extra_status_line").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("widget_status_line").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_calibration_long").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_calibration_short").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_avg").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_a1c_dcct").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_a1c_ifcc").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_in").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_high").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("status_line_low").setOnPreferenceChangeListener(new WidgetListener());
            findPreference("extra_status_line").setOnPreferenceChangeListener(new WidgetListener());
        }

        private void setupBarcodeConfigScanner() {
            findPreference("auto_configure").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AndroidBarcode(getActivity()).scan();
                    return true;
                }
            });
        }


        private void setupBarcodeShareScanner() {
            findPreference("scan_share2_barcode").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AndroidBarcode(getActivity()).scan();
                    return true;
                }
            });
        }

        private void bindTTSListener(){
            findPreference("bg_to_speech").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((Boolean)newValue) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        alertDialog.setTitle("Install Text-To-Speech Data?");
                        alertDialog.setMessage("Install Text-To-Speech Data?\n(After installation of languages you might have to press \"Restart Collector\" in System Status.)");
                        alertDialog.setCancelable(true);
                        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BgToSpeech.installTTSData(getActivity());
                            }
                        });
                        alertDialog.setNegativeButton(R.string.no, null);
                        AlertDialog alert = alertDialog.create();
                        alert.show();
                    }
                    return true;
                }
            });
        }

        private static Preference.OnPreferenceChangeListener sBgMissedAlertsHandler = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Context context = preference.getContext();
                context.startService(new Intent(context, MissedReadingService.class));
                return true;
            }
        };

        
        private void bindBgMissedAlertsListener(){
          findPreference("other_alerts_snooze").setOnPreferenceChangeListener(sBgMissedAlertsHandler);
        }

        private static class WidgetListener implements Preference.OnPreferenceChangeListener {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Context context = preference.getContext();
                if(AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, xDripWidget.class)).length > 0){
                    context.startService(new Intent(context, WidgetUpdateService.class));
                }
                return true;
            }
        }
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}


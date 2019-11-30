package io.szp.soundlocalization;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences preferences;

        private EditTextPreference cycleTimePreference;
        private EditTextPreference startFreq1Preference;
        private EditTextPreference endFreq1Preference;
        private EditTextPreference startFreq2Preference;
        private EditTextPreference endFreq2Preference;
        
        private SwitchPreferenceCompat twoDimensionEnabledPreference;
        private EditTextPreference startIntensityThresholdPreference;
        private EditTextPreference startIndexStdLimitPreference;
        private EditTextPreference endIntensityThresholdPreference;
        private EditTextPreference endIndexStdLimitPreference;
        private EditTextPreference bufferLengthPreference;
        private EditTextPreference fftLengthPreference;

        private SwitchPreferenceCompat useSecondSenderPreference;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            FragmentActivity activity = getActivity();
            if (activity == null)
                throw new AssertionError("Expected activity");
            preferences = PreferenceManager.getDefaultSharedPreferences(activity);
            preferences.registerOnSharedPreferenceChangeListener(this);

            cycleTimePreference = findPreference(getString(R.string.cycle_time_key));
            startFreq1Preference = findPreference(getString(R.string.start_freq1_key));
            endFreq1Preference = findPreference(getString(R.string.end_freq1_key));
            startFreq2Preference = findPreference(getString(R.string.start_freq2_key));
            endFreq2Preference = findPreference(getString(R.string.end_freq2_key));

            twoDimensionEnabledPreference = findPreference(
                    getString(R.string.two_dimension_enabled_key));
            startIntensityThresholdPreference = findPreference(
                    getString(R.string.start_intensity_threshold_key));
            startIndexStdLimitPreference = findPreference(
                    getString(R.string.start_index_std_limit_key));
            endIntensityThresholdPreference = findPreference(
                    getString(R.string.end_intensity_threshold_key));
            endIndexStdLimitPreference = findPreference(
                    getString(R.string.end_index_std_limit_key));
            bufferLengthPreference = findPreference(getString(R.string.buffer_length_key));
            fftLengthPreference = findPreference(getString(R.string.fft_length_key));

            useSecondSenderPreference = findPreference(getString(R.string.use_second_sender_key));

            cycleTimePreference.setOnBindEditTextListener(new FloatOnBindEditTextListener());
            startFreq1Preference.setOnBindEditTextListener(new FloatOnBindEditTextListener());
            endFreq1Preference.setOnBindEditTextListener(new FloatOnBindEditTextListener());
            startFreq2Preference.setOnBindEditTextListener(new FloatOnBindEditTextListener());
            endFreq2Preference.setOnBindEditTextListener(new FloatOnBindEditTextListener());

            startIntensityThresholdPreference.setOnBindEditTextListener(
                    new FloatOnBindEditTextListener());
            startIndexStdLimitPreference.setOnBindEditTextListener(
                    new FloatOnBindEditTextListener());
            endIntensityThresholdPreference.setOnBindEditTextListener(
                    new FloatOnBindEditTextListener());
            endIndexStdLimitPreference.setOnBindEditTextListener(
                    new FloatOnBindEditTextListener());
            bufferLengthPreference.setOnBindEditTextListener(new IntegerOnBindEditTextListener());
            fftLengthPreference.setOnBindEditTextListener(new IntegerOnBindEditTextListener());

            updateEnabled();
        }

        private static class IntegerOnBindEditTextListener
                implements EditTextPreference.OnBindEditTextListener {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        }

        private static class FloatOnBindEditTextListener
                implements EditTextPreference.OnBindEditTextListener {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
        }

        private void updateEnabled() {
            Resources res = getResources();
            boolean receiverEnabled = preferences.getBoolean(
                    getString(R.string.receiver_enabled_key),
                    res.getBoolean(R.bool.receiver_enabled_default));
            boolean senderEnabled = preferences.getBoolean(
                    getString(R.string.sender_enabled_key),
                    res.getBoolean(R.bool.sender_enabled_default));
            boolean receiverOrSenderEnabled = receiverEnabled || senderEnabled;
            cycleTimePreference.setEnabled(!receiverOrSenderEnabled);
            startFreq1Preference.setEnabled(!receiverOrSenderEnabled);
            endFreq1Preference.setEnabled(!receiverOrSenderEnabled);
            startFreq2Preference.setEnabled(!receiverOrSenderEnabled);
            endFreq2Preference.setEnabled(!receiverOrSenderEnabled);
            twoDimensionEnabledPreference.setEnabled(!receiverEnabled);
            startIntensityThresholdPreference.setEnabled(!receiverEnabled);
            startIndexStdLimitPreference.setEnabled(!receiverEnabled);
            endIntensityThresholdPreference.setEnabled(!receiverEnabled);
            endIndexStdLimitPreference.setEnabled(!receiverEnabled);
            bufferLengthPreference.setEnabled(!receiverEnabled);
            fftLengthPreference.setEnabled(!receiverEnabled);
            useSecondSenderPreference.setEnabled(!senderEnabled);
        }

        @Override
        public void onDestroy() {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (key.equals(getString(R.string.receiver_enabled_key))) {
                updateEnabled();
            } else if (key.equals(getString(R.string.sender_enabled_key))) {
                updateEnabled();
            } else if (key.equals(getString(R.string.cycle_time_key))) {
                cycleTimePreference.setText(preferences.getString(key,
                        getString(R.string.cycle_time_default)));
            } else if (key.equals(getString(R.string.start_freq1_key))) {
                startFreq1Preference.setText(preferences.getString(key,
                        getString(R.string.start_freq1_default)));
            } else if (key.equals(getString(R.string.end_freq1_key))) {
                endFreq1Preference.setText(preferences.getString(key,
                        getString(R.string.end_freq1_default)));
            } else if (key.equals(getString(R.string.start_freq2_key))) {
                startFreq2Preference.setText(preferences.getString(key,
                        getString(R.string.start_freq1_default)));
            } else if (key.equals(getString(R.string.end_freq2_key))) {
                endFreq2Preference.setText(preferences.getString(key,
                        getString(R.string.end_freq2_default)));
            } else if (key.equals(getString(R.string.start_intensity_threshold_key))) {
                startIntensityThresholdPreference.setText(preferences.getString(key,
                        getString(R.string.start_intensity_threshold_default)));
            } else if (key.equals(getString(R.string.start_index_std_limit_key))) {
                startIndexStdLimitPreference.setText(preferences.getString(key,
                        getString(R.string.start_index_std_limit_default)));
            } else if (key.equals(getString(R.string.end_intensity_threshold_key))) {
                endIntensityThresholdPreference.setText(preferences.getString(key,
                        getString(R.string.end_intensity_threshold_default)));
            } else if (key.equals(getString(R.string.end_index_std_limit_key))) {
                endIndexStdLimitPreference.setText(preferences.getString(key,
                        getString(R.string.end_index_std_limit_default)));
            } else if (key.equals(getString(R.string.buffer_length_key))) {
                bufferLengthPreference.setText(preferences.getString(key,
                        getString(R.string.buffer_length_default)));
            } else if (key.equals(getString(R.string.fft_length_key))) {
                fftLengthPreference.setText(preferences.getString(key,
                        getString(R.string.fft_length_default)));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return false;
    }
}
package gpsplus.rtkgps.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import gpsplus.rtkgps.R;
import gpsplus.rtkgps.databinding.ActivityStartupShutdownCommandsBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class StartupShutdownSettingsActivity extends AppCompatActivity {
    static final String TAG = StartupShutdownSettingsActivity.class.getSimpleName();

    public static final String ARG_SHARED_PREFS_NAME = "shared_prefs_name";

    public static final String SHARED_PREFS_KEY_SEND_COMMANDS_AT_STARTUP = "send_commands_at_startup";
    public static final String SHARED_PREFS_KEY_COMMANDS_AT_STARTUP = "commands_at_startup";
    public static final String SHARED_PREFS_KEY_SEND_COMMANDS_AT_SHUTDOWN = "send_commands_at_shutdown";
    public static final String SHARED_PREFS_KEY_COMMANDS_AT_SHUTDOWN = "commands_at_shutdown";

    static final String ASSETS_PATH_COMMANDS = "commands";

    private String mSharedPrefsName;

    private ActivityStartupShutdownCommandsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartupShutdownCommandsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        mSharedPrefsName = intent.getStringExtra(ARG_SHARED_PREFS_NAME);
        if (mSharedPrefsName == null) {
            throw new IllegalArgumentException("ARG_SHARED_PREFS_NAME not defined");
        }

        binding.sendCommandsAtStartup.setOnClickListener(this::onSendCommandsAtStartupClicked);
        binding.sendCommandsAtShutdown.setOnClickListener(this::onSendCommandsAtShutdownClicked);
        binding.loadStartupShutdownCommands.setOnClickListener(this::onLoadButtonClicked);
        binding.cancel.setOnClickListener(this::onCancelButtonClicked);
        binding.ok.setOnClickListener(this::onOkButtonClicked);

        loadSettings();
    }

    public void onSendCommandsAtStartupClicked(View v) {
        binding.commandsAtStartup.setEnabled(binding.sendCommandsAtStartup.isChecked());
    }

    public void onSendCommandsAtShutdownClicked(View v) {
        binding.commandsAtShutdown.setEnabled(binding.sendCommandsAtShutdown.isChecked());
    }

    public void onCancelButtonClicked(View v) {
        finish();
    }

    public void onLoadButtonClicked(View v) {
        showLoadCommandsDialog();
    }

    public void onOkButtonClicked(View v) {
        saveSettings();
        finish();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(mSharedPrefsName, MODE_PRIVATE);

        boolean sendStartup = prefs.getBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_STARTUP, false);
        boolean sendShutdown = prefs.getBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_SHUTDOWN, false);
        String startup = prefs.getString(SHARED_PREFS_KEY_COMMANDS_AT_STARTUP, "");
        String shutdown = prefs.getString(SHARED_PREFS_KEY_COMMANDS_AT_SHUTDOWN, "");

        binding.sendCommandsAtStartup.setChecked(sendStartup);
        binding.commandsAtStartup.setEnabled(sendStartup);
        binding.commandsAtStartup.setText(startup);

        binding.sendCommandsAtShutdown.setChecked(sendShutdown);
        binding.commandsAtShutdown.setEnabled(sendShutdown);
        binding.commandsAtShutdown.setText(shutdown);
    }

    private void saveSettings() {
        getSharedPreferences(mSharedPrefsName, MODE_PRIVATE)
                .edit()
                .putBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_STARTUP, binding.sendCommandsAtStartup.isChecked())
                .putString(SHARED_PREFS_KEY_COMMANDS_AT_STARTUP, binding.commandsAtStartup.getText().toString())
                .putBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_SHUTDOWN, binding.sendCommandsAtShutdown.isChecked())
                .putString(SHARED_PREFS_KEY_COMMANDS_AT_SHUTDOWN, binding.commandsAtShutdown.getText().toString())
                .apply();
    }

    private void showLoadCommandsDialog() {
        SelectCommandsFileDialog dialog = new SelectCommandsFileDialog();
        dialog.show(getSupportFragmentManager(), "commandsSelector");
    }

    void onLoadFileSelected(String path) {
        StringBuilder startup = new StringBuilder();
        StringBuilder shutdown = new StringBuilder();
        boolean isStartup = true;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(path)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("@")) {
                    isStartup = false;
                    continue;
                }
                if (isStartup) {
                    startup.append(line).append('\n');
                } else {
                    shutdown.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "onLoadFileSelected: " + e.getMessage());
        }

        binding.sendCommandsAtStartup.setChecked(startup.length() > 0);
        binding.commandsAtStartup.setEnabled(binding.sendCommandsAtStartup.isChecked());
        binding.commandsAtStartup.setText(startup.toString());

        binding.sendCommandsAtShutdown.setChecked(shutdown.length() > 0);
        binding.commandsAtShutdown.setEnabled(binding.sendCommandsAtShutdown.isChecked());
        binding.commandsAtShutdown.setText(shutdown.toString());
    }

    public static void setDefaultValue(Context ctx, String sharedPrefsName) {
        ctx.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_STARTUP, false)
                .putString(SHARED_PREFS_KEY_COMMANDS_AT_STARTUP, "")
                .putBoolean(SHARED_PREFS_KEY_SEND_COMMANDS_AT_SHUTDOWN, false)
                .putString(SHARED_PREFS_KEY_COMMANDS_AT_SHUTDOWN, "")
                .apply();
    }

    public static class SelectCommandsFileDialog extends DialogFragment {

        private CharSequence[] mFiles;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mFiles = getCommandsList();

            return new AlertDialog.Builder(requireActivity())
                    .setItems(mFiles, mOnClickListener)
                    .setTitle(R.string.load_startup_shutdown_dialog_title)
                    .setNegativeButton(android.R.string.cancel, (dialog, id) -> dismiss())
                    .create();
        }

        private CharSequence[] getCommandsList() {
            try {
                String[] files = requireActivity().getAssets().list(ASSETS_PATH_COMMANDS);
                assert files != null;
                Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
                return files;
            } catch (IOException e) {
                Log.e(TAG, "getCommandsList: " + e.getMessage());
            }
            return new String[0];
        }

        private final DialogInterface.OnClickListener mOnClickListener = (dialog, which) -> {
            ((StartupShutdownSettingsActivity) requireActivity())
                    .onLoadFileSelected(ASSETS_PATH_COMMANDS + "/" + mFiles[which]);
            dismiss();
        };
    }
}


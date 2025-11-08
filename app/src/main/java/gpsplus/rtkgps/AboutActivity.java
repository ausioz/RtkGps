package gpsplus.rtkgps;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import java.util.Locale;

import gpsplus.rtkgps.utils.ChangeLog;
import gpsplus.rtkgps.utils.Translated;

public class AboutActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.version)).setText(pi.versionName);
        } catch (NameNotFoundException nnfe) {
            Log.e("AboutActivity", "onCreate: " + nnfe.getMessage() );
        }

        TextView translationTextView = findViewById(R.id.translation_label);
        TextView translationLink = findViewById(R.id.translation_link);
        if (!Translated.contains(Locale.getDefault().getISO3Language()))
        {
            translationTextView.setText(getResources().getString(R.string.about_translation_title,Locale.getDefault().getDisplayLanguage(Locale.ENGLISH))+"\n"+
                    getResources().getString(R.string.about_translation_subtitle)+"\n"+
                    getResources().getString(R.string.about_translation_message) );
            translationLink.setText(
                    Html.fromHtml(getResources().getString(R.string.about_translation_link), Html.FROM_HTML_MODE_LEGACY)
            );
            translationLink.setMovementMethod(LinkMovementMethod.getInstance());
        }

    }

    public void onLegacyInfoButtonClicked(View v) {
        final DialogFragment dialog;
        dialog = new OpenSourceLicensesDialog();
        dialog.show(getSupportFragmentManager(), null);
    }

    public void onChangelogButtonClicked(View v) {
        ChangeLog cl = new ChangeLog(this);
        cl.getFullLogDialog().show();
    }

    public static class OpenSourceLicensesDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WebView webView = new WebView(requireActivity());
            webView.loadUrl("file:///android_asset/licenses.html");

            return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.about_licenses)
                .setView(webView)
                .setPositiveButton(android.R.string.ok,
                        (dialog, whichButton) -> dialog.dismiss()
                ).create();
        }

    }
}

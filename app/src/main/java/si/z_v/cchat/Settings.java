/*
 * Copyright (c) 2014 - Domen Ipavec
 */

package si.z_v.cchat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ipaulpro.afilechooser.utils.FileUtils;

/**
 * activity for settings
 */
public class Settings extends Activity {

    /**
     * file chooser request codes
     */
    private static final int USER_CERTIFICATE_CHOOSER = 34928;
    private static final int SERVER_CERTIFICATE_CHOOSER = 34929;
    private static final int TRUSTED_CERTIFICATE_CHOOSER = 34930;

    /**
     * Shared preferences for storing settings
     */
    private static final String PREFS = "CChatPrefsFile";
    private SharedPreferences settings;

    /**
     * View elements used in code
     */
    private Button uc_button;
    private TextView uc_text;
    private EditText server_edit;
    private EditText port_edit;
    private Button sc_button;
    private Button sc_reset;
    private TextView sc_text;
    private Button tc_button;
    private Button tc_reset;
    private TextView tc_text;

    /**
     * initialize activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // load settings
        settings = getSharedPreferences(PREFS, 0);

        // find view elements and initialize them
        uc_button = (Button)findViewById(R.id.button_user_certificate);
        uc_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getContentIntent = FileUtils.createGetContentIntent();
                Intent intent = Intent.createChooser(getContentIntent, getString(R.string.select_user_certificate));
                startActivityForResult(intent, USER_CERTIFICATE_CHOOSER);
            }
        });

        uc_text = (TextView)findViewById(R.id.text_user_certificate);
        uc_text.setText(settings.getString("user_certificate", ""));

        server_edit = (EditText)findViewById(R.id.edit_server);
        server_edit.setText(settings.getString("server", "cchat-server.z-v.si"));

        port_edit = (EditText)findViewById(R.id.edit_port);
        port_edit.setText(Integer.toString(settings.getInt("port", 7094)));

        sc_button = (Button)findViewById(R.id.button_server_certificate);
        sc_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getContentIntent = FileUtils.createGetContentIntent();
                Intent intent = Intent.createChooser(getContentIntent, getString(R.string.select_server_certificate));
                startActivityForResult(intent, SERVER_CERTIFICATE_CHOOSER);
            }
        });

        sc_reset = (Button)findViewById(R.id.reset_server_certificate);
        sc_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sc_text.setText("server.crt");
            }
        });

        sc_text = (TextView)findViewById(R.id.text_server_certificate);
        sc_text.setText(settings.getString("server_certificate", "server.crt"));

        tc_button = (Button)findViewById(R.id.button_trusted_certificate);
        tc_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getContentIntent = FileUtils.createGetContentIntent();
                Intent intent = Intent.createChooser(getContentIntent, getString(R.string.select_trusted_certificate));
                startActivityForResult(intent, TRUSTED_CERTIFICATE_CHOOSER);
            }
        });

        tc_reset = (Button)findViewById(R.id.reset_trusted_certificate);
        tc_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tc_text.setText("trusted.crt");
            }
        });

        tc_text = (TextView)findViewById(R.id.text_trusted_certificate);
        tc_text.setText(settings.getString("trusted_certificate", "trusted.crt"));

        // show user certificate chooser when called with intent from cchat oncreate
        if (getIntent().hasExtra("select_certificate")) {
            Intent getContentIntent = FileUtils.createGetContentIntent();
            Intent intent = Intent.createChooser(getContentIntent, getString(R.string.select_user_certificate));
            startActivityForResult(intent, USER_CERTIFICATE_CHOOSER);
        }
    }

    /**
     * store certificate chooser result in appropriate textview
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case USER_CERTIFICATE_CHOOSER:
                if (resultCode == RESULT_OK) {
                    uc_text.setText(FileUtils.getPath(this, data.getData()));
                }
                break;
            case SERVER_CERTIFICATE_CHOOSER:
                if (resultCode == RESULT_OK) {
                    sc_text.setText(FileUtils.getPath(this, data.getData()));
                }
                break;
            case TRUSTED_CERTIFICATE_CHOOSER:
                if (resultCode == RESULT_OK) {
                    tc_text.setText(FileUtils.getPath(this, data.getData()));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Save settings on pause
     */
    @Override
    protected void onPause() {
        SharedPreferences.Editor e = settings.edit();
        e.putString("user_certificate", uc_text.getText().toString());
        e.putString("server", server_edit.getText().toString());
        e.putInt("port", Integer.parseInt(port_edit.getText().toString()));
        e.putString("server_certificate", sc_text.getText().toString());
        e.putString("trusted_certificate", tc_text.getText().toString());
        e.commit();
        super.onPause();
    }
}

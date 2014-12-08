/*
 * Copyright (c) 2014 - Domen Ipavec
 */
package si.z_v.cchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;

/**
 * Main activity for connecting to server and user list
 */
public class CChat extends Activity {

    /**
     * Fragments for activity
     */
    UsersFragment uf;
    NoConnection nc;
    UserAdd ua;

    /**
     * Reference to self
     */
    CChat self = this;

    /**
     * Shared preferences for saving settings.
     */
    SharedPreferences settings;
    static final String PREFS = "CChatPrefsFile";

    /**
     * Password for user certificate.
     */
    String password = null;

    /**
     * keep track of fragments
     */
    int fragment;
    static final int NO_CONNECTION = 0;
    static final int USERS_LIST = 1;
    static final int USERS_ADD = 2;

    /**
     * track when activity is on top
     */
    public boolean focused = false;

    /**
     * Initialize activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init fragments
        uf = new UsersFragment();
        nc = new NoConnection();
        ua = new UserAdd();

        // ServerConnection needs our reference for messages
        ServerConnection.getInstance().setActivity(this);

        // load view
        setContentView(R.layout.activity_cchat);

        // load appropriate fragment
        if (savedInstanceState == null) {
            if (ServerConnection.getInstance().isInitialized()) {
                openHome();
            } else {
                openNotConnected();
            }
        } else {
            switch (savedInstanceState.getInt("fragment")) {
                case NO_CONNECTION:
                    openNotConnected();
                    break;
                case USERS_ADD:
                    openAdd();
                    break;
                case USERS_LIST:
                    openHome();
                    break;
            }
        }

        // load settings
        settings = getSharedPreferences(PREFS, 0);

        // cannot launch activity from onCreate, use delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // show select_user_certificate dialog if necessary
                if (!settings.contains("user_certificate")) {
                    Intent intent = new Intent(self, Settings.class);
                    intent.putExtra("select_certificate", true);
                    startActivity(intent);

                // otherwise connect to server
                } else if (!ServerConnection.getInstance().isInitialized()) {
                    connect();
                }
            }
        }, 0);
    }

    /**
     * save fragment
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("fragment", fragment);
    }

    /**
     * track focused
     */
    @Override
    public void onPause() {
        super.onPause();
        focused = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        focused = true;
    }

    /**
     * show error and warning messages
     */
    public Handler errorHandler = new Handler() {
        public void handleMessage(Message msg) {
            String e = msg.getData().getString("error");
            final boolean exit = msg.getData().getBoolean("exit");

            AlertDialog.Builder builder = new AlertDialog.Builder(self);
            builder.setMessage(e);
            if (exit) {
                // load not connected on error messages
                builder.setTitle(R.string.error);
                openNotConnected();
            } else {
                builder.setTitle(R.string.warning);
            }

            builder.setPositiveButton(R.string.ok, null);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    /**
     * load user list fragment when connected to server
     */
    public Handler connectedHanler = new Handler() {
        public void handleMessage(Message msg) {
            openHome();
        }
    };

    /**
     * load not connected on logout
     */
    public Handler disconnectedHandler = new Handler() {
        public void handleMessage(Message msg) {
            openNotConnected();
        }
    };

    /**
     * interface function to errorHandler
     * @param e message
     * @param exit is terminal, open not connected
     */
    public void showError(String e, boolean exit) {
        Message m = new Message();
        Bundle b = new Bundle();
        b.putString("error", e);
        b.putBoolean("exit", exit);
        m.setData(b);
        errorHandler.sendMessage(m);
    }

    /**
     * ask for user certificate password,
     * then connect to server
     */
    public void connect() {
        AlertDialog.Builder passwordPrompt = new AlertDialog.Builder(self);

        passwordPrompt.setTitle(R.string.password);
        passwordPrompt.setMessage(R.string.enter_password_user_certificate);

        final EditText input = new EditText(self);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordPrompt.setView(input);

        passwordPrompt.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                password = input.getText().toString();
                ServerConnection.getInstance().initialize();
            }
        });

        passwordPrompt.setNegativeButton(R.string.cancel, null);

        passwordPrompt.show();
    }

    /**
     * load user add fragment
     */
    public void openAdd() {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, ua)
                .commit();
        fragment = USERS_ADD;
    }

    /**
     * load users list fragment
     */
    public void openHome() {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, uf)
                .commit();
        fragment = USERS_LIST;
    }

    /**
     * load not connected fragment
     */
    public void openNotConnected() {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, nc)
                .commit();
        fragment = NO_CONNECTION;
    }

    /**
     * handle action bar item clicks
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                // load settings activity
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                return true;

            case R.id.action_add:
                openAdd();
                return true;

            case R.id.action_logout:
                ServerConnection.getInstance().logout();
                return true;

            case android.R.id.home:
                openHome();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * finish if not connected to server
     */
    @Override
    public void onStop() {
        super.onStop();
        if (!ServerConnection.getInstance().isInitialized()) {
            this.finish();
        }
    }
}

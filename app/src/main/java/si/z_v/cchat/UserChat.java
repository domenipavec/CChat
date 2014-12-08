/*
 * Copyright (c) 2014 - Domen Ipavec
 */

package si.z_v.cchat;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

/**
 * User chat activity
 */
public class UserChat extends Activity {

    /**
     * user name
     */
    public String user;

    /**
     * initialize activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_chat);

        // get user name from intent
        user = getIntent().getStringExtra("user");
        setTitle(user);

        // show chat fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new Chat())
                .commit();
    }

    /**
     * inflate options menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_chat, menu);
        return true;
    }
}

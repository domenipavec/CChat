/*
 * Copyright (c) 2014 - Domen Ipavec
 */

package si.z_v.cchat;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * NoConnection fragment for cchat activity
 */
public class NoConnection extends Fragment {
    /**
     * required empty public constructor
     */
    public NoConnection() {
    }

    /**
     * initialize view
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_no_connection, container, false);

        // add connect button listener
        Button b = (Button)v.findViewById(R.id.button_connect);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CChat)getActivity()).connect();
            }
        });

        return v;
    }

    /**
     * initialize menu, clear is needed to not duplicate items
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_cchat_noconnection, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

}

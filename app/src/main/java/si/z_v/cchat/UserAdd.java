/*
 * Copyright (c) 2014 - Domen Ipavec
 */

package si.z_v.cchat;

import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.ArrayList;


/**
 * user add fragment for cchat activity
 */
public class UserAdd extends Fragment {

    /**
     * adapter for results list
     */
    private ArrayAdapter<String> mAdapter;

    /**
     * required empty constructor
     */
    public UserAdd() {
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
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_user_add, container, false);

        // update action bar
        ActionBar ab = getActivity().getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.title_add);
        setHasOptionsMenu(true);

        final ServerConnection sc = ServerConnection.getInstance();

        // initalize search view
        final SearchView sv = (SearchView)v.findViewById(R.id.searchView);
        sv.setOnQueryTextListener(
            new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    sc.results.clear();
                    mAdapter.notifyDataSetChanged();
                    ServerConnection.getInstance().find(query);
                    return true;
                }
            }
        );

        // initialize adapter
        mAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, sc.results);

        // Set the adapter
        ListView lv = (ListView) v.findViewById(R.id.resultsView);
        lv.setAdapter(mAdapter);

        // add user to friends on result click
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ServerConnection.getInstance().add((String)parent.getItemAtPosition(position));
                sv.clearFocus();
                ((CChat)getActivity()).openHome();
            }
        });

        return v;
    }

    /**
     * initialize options menu, clear is needed because of fragments
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_cchat_add, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    /**
     * refresh view, called from serverconnection
     */
    public Handler resultChangedHandler = new Handler() {
        public void handleMessage(Message msg) {
            mAdapter.notifyDataSetChanged();
        }
    };
}

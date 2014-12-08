/*
 * Copyright (c) 2014 - Domen Ipavec
 */

package si.z_v.cchat;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Fragment for list of users
 */
public class UsersFragment extends Fragment {
    /**
     * The fragment's ListView
     */
    private ListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ArrayAdapter<String> mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UsersFragment() {
    }

    /**
     * initialize fragment
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users_list, container, false);

        final ServerConnection sc = ServerConnection.getInstance();

        // custom styling of items (gray offline, and bold connected)
        mAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, sc.friends_names) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView)super.getView(position, convertView, parent);
                if (!sc.friends_online.get(position)) {
                    textView.setTextColor(Color.LTGRAY);
                } else {
                    textView.setTextColor(Color.BLACK);
                }
                if (sc.chats.containsKey(sc.friends_names.get(position))) {
                    textView.setTypeface(null, Typeface.BOLD);
                } else {
                    textView.setTypeface(null, Typeface.NORMAL);
                }
                return textView;
            }
        };

        // initialize actionbar
        ActionBar ab = getActivity().getActionBar();
        ab.setTitle(R.string.title_cchat);
        ab.setDisplayHomeAsUpEnabled(false);
        setHasOptionsMenu(true);

        // Set the adapter
        mListView = (ListView) view.findViewById(R.id.usersList);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (sc.friends_online.get(position)) {
                    Intent intent = new Intent(getActivity(), UserChat.class);
                    intent.putExtra("user", sc.friends_names.get(position));
                    startActivity(intent);
                }
            }
        });

        // register list view for context menu
        this.registerForContextMenu(mListView);

        // create message for empty list
        TextView emptyView = new TextView(getActivity());
        emptyView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
        emptyView.setText(R.string.message_no_users);
        emptyView.setTextSize(20);
        emptyView.setVisibility(View.GONE);
        emptyView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        ((ViewGroup)mListView.getParent()).addView(emptyView);
        mListView.setEmptyView(emptyView);

        return view;
    }

    /**
     * initialize options menu, clear is needed because fragments
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_cchat, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    /**
     * users list has changed handler
     */
    public Handler usersChangedHandler = new Handler() {
        public void handleMessage(Message msg) {
            mAdapter.notifyDataSetChanged();
        }
    };

    /**
     * another user added us as a friend, show invite
     */
    public Handler inviteHandler = new Handler() {
        public void handleMessage(Message msg) {
            final String user = msg.getData().getString("user");

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(String.format(getString(R.string.invite_msg), user));
            builder.setTitle(R.string.invite);

            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ServerConnection.getInstance().add(user);
                }
            });

            builder.setNegativeButton(R.string.no, null);

            builder.create().show();
        }
    };

    /**
     * initialize context menu
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_user_context, menu);
    }

    /**
     * context menu callback
     * @param item
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_remove:
                ServerConnection.getInstance().remove(mAdapter.getItem(info.position));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}

/*
 * Copyright (c) 2014 - Domen Ipavec
 */

package si.z_v.cchat;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


/**
 * Chat fragment for UserChat
 */
public class Chat extends Fragment {

    /**
     * name of user we are talking to
     */
    String user;

    /**
     * adapter for messages' listview
     */
    private ArrayAdapter<String> mAdapter;

    /**
     * ChatConnection for this chat
     */
    private ChatConnection cc;

    /**
     * is fragment shown on top
     */
    public boolean focused = false;

    /**
     * send button
     */
    private Button b;

    /**
     * message text edit
     */
    private EditText et;

    /**
     * required empty public constructor
     */
    public Chat() {
    }

    /**
     * initialize fragment
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * initialize fragment view
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_chat, container, false);


        // get user name from activity
        user = ((UserChat)getActivity()).user;

        // get chat connection from server connection
        cc = ServerConnection.getInstance().initChat(this);

        // initialize adapter
        mAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, cc.lines) {

            /**
             * override getview to set different gravity for remote messages
             * @param position
             * @param convertView
             * @param parent
             * @return
             */
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView)super.getView(position, convertView, parent);
                if (cc.remote.get(position)) {
                    textView.setGravity(Gravity.RIGHT);
                } else {
                    textView.setGravity(Gravity.LEFT);
                }
                textView.setTextAppearance(getContext(), android.R.style.TextAppearance_DeviceDefault_Medium);
                return textView;
            }

            /**
             * all items disabled
             * @return
             */
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            /**
             * all items disabled
             * @param position
             * @return
             */
            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };

        // Set adapter to listview
        ListView lv = (ListView) v.findViewById(R.id.chatList);
        lv.setAdapter(mAdapter);

        // set edittext callback
        et = (EditText) v.findViewById(R.id.chatEdit);
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    send();
                    return true;
                }
                return false;
            }
        });

        // set button callback
        b = (Button) v.findViewById(R.id.chatSend);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("button", "click");
                send();
            }
        });

        // enable edit and button if connected to verified peer
        if (cc.verified) {
            b.setEnabled(true);
            et.setEnabled(true);
        }

        return v;
    }

    /**
     * send only non empty messages
     * clear text edit
     */
    public void send() {
        if (et.getText().length() > 0) {
            cc.send(et.getText().toString());
            et.setText("");
        }
    }

    /**
     * update focused
     */
    @Override
    public void onResume() {
        super.onResume();
        focused = true;
    }

    /**
     * update focused
     */
    @Override
    public void onPause() {
        super.onPause();
        focused = false;
    }

    /**
     * got new message
     */
    public Handler msgHandler = new Handler() {
        public void handleMessage(Message msg) {
            mAdapter.notifyDataSetChanged();
        }
    };

    /**
     * show untrusted peer error and exit
     */
    public Handler errorHandler = new Handler() {
        public void handleMessage(Message msg) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(R.string.error_untrusted_peer);
            builder.setTitle(R.string.error);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().finish();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    /**
     * peer went offline, inform and close
     */
    public Handler closeHandler = new Handler() {
        public void handleMessage(Message msg) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setMessage(R.string.user_offline);
            builder.setTitle(R.string.warning);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    /**
     * enable edit and button on successful connection
     */
    public Handler connectedHandler = new Handler() {
        public void handleMessage(Message msg) {
            b.setEnabled(true);
            et.setEnabled(true);
        }
    };



}

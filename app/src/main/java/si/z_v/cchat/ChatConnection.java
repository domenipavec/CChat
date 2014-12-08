/*
 * Copyright (c) 2014 - Domen Ipavec
 */

package si.z_v.cchat;

import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * Handle SSL connection to peer
 */
public class ChatConnection {
    /**
     * reference to chat fragment
     */
    private Chat chat = null;

    /**
     * name of peer
     */
    private String user;

    /**
     * SSLEngine for encryption
     */
    private SSLEngine sse = null;

    /**
     * messages
     */
    public ArrayList<String> lines = new ArrayList<String>();

    /**
     * are messages my or from peer
     */
    public ArrayList<Boolean> remote = new ArrayList<Boolean>();

    /**
     * is connection verified
     */
    public boolean verified = false;

    /**
     * init connection from chat fragment
     * connect to peer as client
     * @param c
     */
    public ChatConnection(Chat c) {
        setChat(c);
        user = c.user;

        new Thread(new InitClientThread()).start();
    }

    /**
     * init connection from serverconnection
     * connect to peer as server
     * @param u peer name
     * @param msg initial msg received
     */
    public ChatConnection(String u, String msg) {
        user = u;

        new Thread(new InitServerThread(msg)).start();
    }

    /**
     * update chat fragment
     * @param c
     */
    public void setChat(Chat c) {
        chat = c;
    }

    /**
     * initialize SSLContext from server connection key and trust managers
     * @return
     */
    public SSLContext getContext() {
        try {
            ServerConnection sc = ServerConnection.getInstance();

            SSLContext context = SSLContext.getInstance("TLSv1");
            context.init(sc.kmf.getKeyManagers(), sc.tmf.getTrustManagers(), new SecureRandom());
            return context;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * initialize ssl engine, put it in client mode and start handshake
     */
    class InitClientThread implements Runnable {
        public void run() {
            try {
                sse = getContext().createSSLEngine(user, 7094);

                sse.setNeedClientAuth(true);
                sse.setUseClientMode(true);
                sse.beginHandshake();

                new SSLWorker().run();
            } catch (SSLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * initialize ssl engine, put it in server mode and start handshake
     */
    class InitServerThread implements Runnable {
        String msg;
        public InitServerThread(String m) {
            msg = m;
        }

        public void run() {
            try {
                sse = getContext().createSSLEngine();

                sse.setNeedClientAuth(true);
                sse.setUseClientMode(false);
                sse.beginHandshake();

                new SSLWorker().run();

                SSLUnwrap(msg);
            } catch (SSLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * process SSLEngine until finished, not_handshaking or waiting for msg
     */
    class SSLWorker implements Runnable {
        public void run() {
            while (true) {
                switch (sse.getHandshakeStatus()) {
                    case FINISHED:
                        verifyPeer();
                        return;
                    case NEED_TASK:
                        sse.getDelegatedTask().run();
                        break;
                    case NEED_UNWRAP:
                        return;
                    case NEED_WRAP:
                        ServerConnection.getInstance().msg(user, SSLWrap(""));
                        break;
                    case NOT_HANDSHAKING:
                        verifyPeer();
                        return;
                }
            }
        }
    }

    /**
     * verify peer, called when handshake is finished
     */
    public void verifyPeer() {
        if (!verified) {
            try {
                // parse principal
                for (String pair : sse.getSession().getPeerPrincipal().toString().split(",| \\+ ")) {
                    String[] split = pair.split("=");
                    if (split[0].equals("CN") && split[1].equals(user)) {
                        verified = true;
                    }
                }
            } catch (SSLPeerUnverifiedException e) {
                e.printStackTrace();
            }
            if (!verified) {
                // inform user of unverified peer and close
                try {
                    sse.closeInbound();
                    sse.closeOutbound();
                } catch (SSLException e) {
                    e.printStackTrace();
                }
                if (isChatOpen()) {
                    chat.errorHandler.sendEmptyMessage(0);
                }
                ServerConnection.getInstance().chats.remove(user);
            } else {
                // enable sending messages
                if (isChatOpen()) {
                    chat.connectedHandler.sendEmptyMessage(0);
                }
            }
        }
    }

    /**
     * encrypt send and return base64 encoded output
     * @param send
     * @return
     */
    public String SSLWrap(String send) {
        try {
            ByteBuffer raw = ByteBuffer.wrap(send.getBytes(Charset.forName("UTF-8")));
            ByteBuffer encrypted = ByteBuffer.allocateDirect(sse.getSession().getPacketBufferSize());
            sse.wrap(raw, encrypted);
            return Base64.encodeToString(trimmedArray(encrypted), Base64.NO_WRAP);
        } catch (SSLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * process received msg
     * @param m
     */
    public void SSLUnwrap(String m) {
        // decode and decrypt
        byte[] decoded = Base64.decode(m, Base64.DEFAULT);

        ByteBuffer encrypted = ByteBuffer.allocateDirect(sse.getSession().getPacketBufferSize());
        encrypted.put(decoded);
        encrypted.flip();

        ByteBuffer raw = ByteBuffer.allocate(sse.getSession().getApplicationBufferSize());
        try {
            sse.unwrap(encrypted, raw);
        } catch (SSLException e) {
            e.printStackTrace();
        }

        // process further sslengine needs
        new Thread(new SSLWorker()).start();

        // check if we got message and publish it
        String msg = new String(trimmedArray(raw), Charset.forName("UTF-8"));
        if (!msg.equals("")) {
            lines.add(msg);
            remote.add(Boolean.TRUE);
            if (isChatOpen()) {
                chat.msgHandler.sendEmptyMessage(0);
            } else {
                // open chat window if necessary
                CChat cc = ServerConnection.getInstance().cchat;
                Intent intent = new Intent(cc, UserChat.class);
                intent.putExtra("user", user);
                cc.startActivity(intent);
            }
        }
    }

    /**
     * check if chat fragment exists and is on top
     * @return
     */
    public boolean isChatOpen() {
        return chat != null && chat.focused;
    }

    /**
     * return byte array from bytebuffer, but only as much as is filled
     * @param bb
     * @return
     */
    static byte[] trimmedArray(ByteBuffer bb)
    {
        return Arrays.copyOf(bb.array(), bb.position());
    }

    /**
     * send message
     * @param s
     */
    public void send(String s) {
        lines.add(s);
        remote.add(Boolean.FALSE);
        if (isChatOpen()) {
            chat.msgHandler.sendEmptyMessage(0);
        }
        ServerConnection.getInstance().msg(user, SSLWrap(s));
    }

    /**
     * close connection (e.g. when peer goes offline)
     */
    public void close() {
        try {
            sse.closeOutbound();
            sse.closeInbound();
        } catch (SSLException e) {
            e.printStackTrace();
        }
        if (isChatOpen()) {
            chat.closeHandler.sendEmptyMessage(0);
        }
    }
}

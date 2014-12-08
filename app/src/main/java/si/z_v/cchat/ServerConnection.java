/*
 * Copyright (c) 2014 - Domen Ipavec
 */

package si.z_v.cchat;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Singleton class for connection to the server
 */
public class ServerConnection {
    /**
     * singleton instance
     */
    private static ServerConnection instance = null;

    /**
     * SSLSocket to server
     */
    private Socket s = null;

    /**
     * lock disables writes while initialization is in progress
     */
    private Lock s_lock = new ReentrantLock();

    /**
     * whether we are connected
     */
    private boolean initialized = false;

    /**
     * cchat activity reference
     */
    public CChat cchat = null;

    /**
     * map of users and respective ChatConnection-s
     */
    public HashMap chats = new HashMap();

    /**
     * trust manager factory for peers
     */
    public TrustManagerFactory tmf;

    /**
     * trust manager factory for server
     */
    private TrustManagerFactory server_tmf;

    /**
     * key manager factory
     */
    public KeyManagerFactory kmf;

    /**
     * closing in progress, disable errors
     */
    private boolean closing = false;

    /**
     * friends list
     */
    public ArrayList<String> friends_names = new ArrayList<String>();
    public ArrayList<Boolean> friends_online = new ArrayList<Boolean>();


    /**
     * results string list
     */
    public ArrayList<String> results = new ArrayList<String>();

    /**
     * protected constructor to disable generation of new instances
     */
    protected ServerConnection() { }

    /**
     * return instance, create if does not exist
     * @return
     */
    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    /**
     * is connected?
     * @return
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * begin initialization
     */
    public void initialize() {
        if (!initialized) {
            new Thread(new InitThread(this)).start();
        }
    }

    /**
     * set cchat activity
     * @param cc
     */
    public void setActivity(CChat cc) {
        cchat = cc;
    }

    /**
     * check if cchat is active
     */
    public boolean isCChatOpen() {
        return cchat != null && cchat.focused;
    }

    /**
     * request user list from server
     */
    public void getUsers() {
        if (initialized) {
            new Thread(new SendThread(this, "list")).start();
        }
    }

    /**
     * find users in server
     * @param s
     */
    public void find(String s) {
        if (initialized) {
            new Thread(new SendThread(this, "find:" + s)).start();
        }
    }

    /**
     * add user as friend in server
     * @param s
     */
    public void add(String s) {
        if (initialized) {
            new Thread(new SendThread(this, "add:" + s)).start();
        }
    }

    /**
     * send msg to user
     * these are base64 encrypted ssl packets
     * @param user
     * @param msg
     */
    public void msg(String user, String msg) {
        if (initialized) {
            new Thread(new SendThread(this, "msg:" + user + ":" + msg)).start();
        }
    }

    /**
     * remove user as friend in server
     * @param s
     */
    public void remove(String s) {
        if (initialized) {
            new Thread(new SendThread(this, "remove:" + s)).start();
        }
    }

    /**
     * was disconnected from server or error connecting, reset and inform user
     * @param msg error message
     */
    public void disconnect(String msg) {
        // disable errors on logout
        if (closing) {
            closing = false;
        } else {
            if (isCChatOpen()) {
                cchat.showError(msg, true);
            }
        }

        // not connected
        initialized = false;

        // clear user connections
        chats.clear();

        // close ssl socket
        try {
            if (s != null) {
                s.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * get ChatConnection for chat fragment
     * @param c
     * @return
     */
    public ChatConnection initChat(Chat c) {
        if (initialized) {
            ChatConnection cc;
            if (chats.containsKey(c.user)) {
                cc = (ChatConnection)chats.get(c.user);
                cc.setChat(c);
            } else {
                cc = new ChatConnection(c);
                chats.put(c.user, cc);
            }
            return cc;
        }
        return null;
    }

    /**
     * cannot close socket from user thread, start close thread
     */
    public void logout() {
        new Thread(new CloseThread()).start();
    }

    /**
     * close socket and inform user
     */
    class CloseThread implements Runnable {
        public void run() {
            closing = true;
            if (isCChatOpen()) {
                cchat.disconnectedHandler.sendEmptyMessage(0);
            }
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * initialize connection to server and receive forever
     */
    class InitThread implements Runnable {
        /**
         * shorthand to server connection instance
         */
        private ServerConnection sc;

        /**
         * input reader
         */
        private BufferedReader in;

        /**
         * constructor
         * @param sc
         */
        public InitThread(ServerConnection sc) {
            this.sc = sc;
        }

        /**
         * do the work
         */
        public void run() {
            InputStream inputStream;

            // lock to disable writes while connecting
            sc.s_lock.lock();

            // load trusted certificate to trust manager (could be more than 1)
            String trust_cert = cchat.settings.getString("trusted_certificate", "trusted.crt");
            try {
                if (trust_cert.equals("trusted.crt")) {
                    inputStream = cchat.getResources().openRawResource(R.raw.trusted);
                } else {
                    inputStream = new FileInputStream(trust_cert);
                }
                KeyStore keyStoreCA = KeyStore.getInstance("BKS");
                keyStoreCA.load(null, null);
                int i = 0;
                for (Certificate certificate : CertificateFactory.getInstance("X.509").generateCertificates(inputStream)) {
                    keyStoreCA.setCertificateEntry("Server" + i, certificate);
                    i++;
                }
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStoreCA);
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                s_lock.unlock();
                disconnect(String.format(cchat.getString(R.string.error_trust_certificate), trust_cert));
                return;
            }

            // load server certificate to trust manager
            String server_cert = cchat.settings.getString("server_certificate", "server.crt");
            try {
                if (server_cert.equals("server.crt")) {
                    inputStream = cchat.getResources().openRawResource(R.raw.server);
                } else {
                    inputStream = new FileInputStream(server_cert);
                }
                KeyStore keyStoreCA = KeyStore.getInstance("BKS");
                keyStoreCA.load(null, null);
                keyStoreCA.setCertificateEntry("Server", CertificateFactory.getInstance("X.509").generateCertificate(inputStream));
                server_tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                server_tmf.init(keyStoreCA);
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                s_lock.unlock();
                disconnect(String.format(cchat.getString(R.string.error_server_certificate), server_cert));
                return;
            }

            // load user certificate to key manager
            try {
                inputStream = new FileInputStream(cchat.settings.getString("user_certificate", ""));
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(inputStream, cchat.password.toCharArray());
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, null);
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                s_lock.unlock();
                disconnect(String.format(cchat.getString(R.string.error_user_certificate), cchat.settings.getString("user_certificate", "")));
                return;
            }

            // get server info from settings
            String server_name = cchat.settings.getString("server", "cchat-server.z-v.si");
            int server_port = cchat.settings.getInt("port", 7094);

            // initialize tcp socket
            Socket sock;
            try {
                sock = new Socket();
                sock.setReuseAddress(true);
                sock.connect(new InetSocketAddress(server_name, server_port));
            } catch (Exception e) {
                e.printStackTrace();
                s_lock.unlock();
                disconnect(String.format(cchat.getString(R.string.error_server_connection), server_name));
                return;
            }

            // wrap socket in SSL
            try {
                SSLContext context = SSLContext.getInstance("TLSv1");
                context.init(kmf.getKeyManagers(), server_tmf.getTrustManagers(), new SecureRandom());
                sc.s = context.getSocketFactory().createSocket(sock, server_name, server_port, true);

                // init receiver
                this.in = new BufferedReader(new InputStreamReader(sc.s.getInputStream()));
            } catch (Exception e) {
                e.printStackTrace();
                s_lock.unlock();
                disconnect(cchat.getString(R.string.error_ssl_init));
                return;
            }

            // set as connected and inform user
            initialized = true;
            sc.s_lock.unlock();
            if (isCChatOpen()) {
                cchat.connectedHanler.sendEmptyMessage(0);
            }

            // load initial user list
            getUsers();

            // start receiving
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // read and parse
                    String read = in.readLine();
                    if (read == null) {
                        s.close();
                        throw new Exception("Connection closed.");
                    }
                    String[] split = read.split(":");

                    // parse list, close chat connections for offline users, and trigger view update
                    if (split[0].equals("list")) {
                        friends_names.clear();
                        friends_online.clear();
                        for (int i = 1; i < split.length; i+=2) {
                            String name = split[i];
                            Boolean o = split[i+1].equals("1");
                            friends_names.add(name);
                            friends_online.add(o);

                            if (!o) {
                                if (chats.containsKey(name)) {
                                    ChatConnection cc = (ChatConnection)chats.get(name);
                                    cc.close();
                                    chats.remove(name);
                                }
                            }
                        }
                        if (isCChatOpen()) {
                            cchat.uf.usersChangedHandler.sendEmptyMessage(0);
                        }

                    // parse find results and trigger view update
                    } else if (split[0].equals("find")) {
                        for (int i = 1; i < split.length; i++) {
                            results.add(split[i]);
                        }
                        if (isCChatOpen()) {
                            cchat.ua.resultChangedHandler.sendEmptyMessage(0);
                        }

                    // inform user of another user adding him as friend
                    } else if (split[0].equals("invite")) {
                        Message m = new Message();
                        Bundle b = new Bundle();
                        b.putString("user", split[1]);
                        m.setData(b);
                        if (isCChatOpen()) {
                            cchat.uf.inviteHandler.sendMessage(m);
                        }

                    // parse ssl message, initialize ChatConnection if necessary
                    } else if (split[0].equals("msg")) {
                        if (chats.containsKey(split[1])) {
                            ((ChatConnection) chats.get(split[1])).SSLUnwrap(split[2]);
                        } else {
                            if (friends_names.contains(split[1])) {
                                ChatConnection cc = new ChatConnection(split[1], split[2]);
                                chats.put(split[1], cc);
                            }
                        }

                    // respond to alive request
                    } else if (split[0].equals("alive")) {
                        new Thread(new SendThread(instance, "alive")).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    disconnect(cchat.getString(R.string.error_server_communication));
                    return;
                }
            }
        }
    }

    /**
     * send message s, on server connection using lock
     * appends newline to msg
     */
    class SendThread implements Runnable {
        private ServerConnection sc;
        private String s;

        public SendThread(ServerConnection sc, String s) {
            this.sc = sc;
            this.s = s + '\n';
        }

        public void run() {
            sc.s_lock.lock();
            try {
                OutputStream os = sc.s.getOutputStream();
                os.write(s.getBytes());
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sc.s_lock.unlock();
        }
    }
}

package com.example.socketdemoserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class TCPServerActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "TCPServerActivity";
    private Button mSendBtn;
    private TextView mShowTV;
    private EditText mInput;
    private boolean mIsServiceDestroyed = false;
    private static final int MESSAGE_RECEIVE_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_RECEIVE_NEW_MSG:
                    Log.d(TAG, "MESSAGE_RECEIVE_NEW_MSG"+(String) msg.obj);
                    mShowTV.setText(mShowTV.getText() + (String) msg.obj);
                    break;
                case MESSAGE_SOCKET_CONNECTED:
                    Log.d(TAG, "MESSAGE_SOCKET_CONNECTED");
                    break;
                default:
                    break;
            }
        }
    };

    private TcpServer tcpServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSendBtn = (Button)findViewById(R.id.send);
        mShowTV = (TextView)findViewById(R.id.msg);
        mInput = (EditText)findViewById(R.id.edit);

        mSendBtn.setOnClickListener(this);

        tcpServer = new TcpServer();
        new Thread(tcpServer).start();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View view) {
        if(view == mSendBtn) {
            final String msg = mInput.getText().toString();
            if(!TextUtils.isEmpty(msg)) {
                tcpServer.loopThread.iServer.test(msg);

                mInput.setText("");
                String time = formatDateTime(System.currentTimeMillis());
                final String showedMsg = "self " + time + ":" + msg +"\n";
                mShowTV.setText(mShowTV.getText() + showedMsg);
            }
        }
    }

    @Override
    public void onDestroy() {
        mIsServiceDestroyed = true;
        super.onDestroy();
    }

    private class TcpServer implements Runnable {
        LoopThread loopThread;

        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(8688);
                Log.d(TAG, "serverSocket:"+ serverSocket);
            } catch (IOException e) {
                Log.d(TAG, "IOException--e"+e.getMessage());
                e.printStackTrace();
                return;
            }
            while (!mIsServiceDestroyed) {
                try {
                    final Socket client = serverSocket.accept();
                    loopThread = new LoopThread(client);
                    Log.d(TAG, "client--");
                    new Thread(loopThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class LoopThread implements Runnable {
        private Socket client;
        private PrintWriter printWriter;

        public LoopThread(Socket client) {
            this.client = client;
        }

        private IServer iServer = new IServer() {
            @Override
            public void test(final String msg) {
                Log.d(TAG, "handler--loop-00");
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        Log.d(TAG, "handler--loop-22");
                        printWriter.println(msg);
                    }
                }.start();
            }
        };

        @SuppressLint("HandlerLeak")
        @Override
        public void run() {
            try {
                //responseClient(client);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
                printWriter = out;
                out.println("欢迎来到聊天室！");
                while (!mIsServiceDestroyed) {
                    Log.d(TAG, "str-0-");
                    String str = in.readLine();//不输入的时候，挂起，等待输入 ？
                    Log.d(TAG, "str-1-"+str);
                    if(str == null) {
                        break;
                    }
                    Log.d(TAG, "str-2-"+str);
                    String time = formatDateTime(System.currentTimeMillis());
                    final String showedMsg = "server " + time + ":" + str +"\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG, showedMsg).sendToTarget();

                    /*int i = new Random().nextInt(mDefinedMessages.length);
                    String msg = mDefinedMessages[i];
                    out.println(msg);*/
                }
                out.close();
                in.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void responseClient(Socket client) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
        out.println("欢迎来到聊天室！");
        while (!mIsServiceDestroyed) {
            Log.d(TAG, "str-0-");
            String str = in.readLine();//不输入的时候，挂起，等待输入 ？
            Log.d(TAG, "str-1-"+str);
            if(str == null) {
                break;
            }
            Log.d(TAG, "str-2-"+str);
            String time = formatDateTime(System.currentTimeMillis());
            final String showedMsg = "server " + time + ":" + str +"\n";
            mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG, showedMsg).sendToTarget();

            /*int i = new Random().nextInt(mDefinedMessages.length);
            String msg = mDefinedMessages[i];
            out.println(msg);*/
        }
        out.close();
        in.close();
        client.close();
    }

    @SuppressLint("SimpleDateFormat")
    private String formatDateTime(long time) {
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }
}

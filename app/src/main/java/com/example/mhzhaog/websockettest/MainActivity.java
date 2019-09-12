package com.example.mhzhaog.websockettest;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {

    private EditText etAddress;
    private Button btnConnect;
    private Button btnDisconnect;
    private TextView tvContent;
    private EditText etMessage;
    private Button btnSend;
    private WebSocket webSocket;

    private long sendTime = 0L;
    //发送心跳包
    private Handler handler = new Handler();
    // 每隔2秒发送一次心跳包，检测连接没有断开
    private static final long HEART_BEAT_RATE = 60 * 1000;

    // 发送心跳包
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {

                webSocket.send("I'm online，current time is " + Calendar.getInstance().get(Calendar.HOUR)+":"+ Calendar.getInstance().get(Calendar.MINUTE) );
                sendTime = System.currentTimeMillis();
            }
            handler.postDelayed(this, HEART_BEAT_RATE); //每隔一定的时间，对长连接进行一次心跳检测
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etAddress = findViewById(R.id.etAddress);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        tvContent = findViewById(R.id.tvContent);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        etAddress.setText("ws://echo.websocket.org");

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = etMessage.getText().toString();
                if(TextUtils.isEmpty(message)){
                    Toast.makeText(MainActivity.this, "message can't be empty!", Toast.LENGTH_SHORT).show();
                }else{
                    if(webSocket != null ){
                        webSocket.send(message);
                        etMessage.setText("");
                        hideKeyboard(v);
                    }else{
                        Toast.makeText(MainActivity.this, "socket is empty!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void disconnect() {
        if(webSocket != null ){
            //java.lang.IllegalArgumentException: Code must be in range [1000,5000): 1
            webSocket.close(1000,"connect close!");
        }
    }

    /**
     * 隐藏软键盘
     * @param view
     */
    public static void hideKeyboard(View view){
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

    private void connect() {
        String address = etAddress.getText().toString();
        if(TextUtils.isEmpty(address)){
            Toast.makeText(this, "address can't be empty", Toast.LENGTH_SHORT).show();
        }else{
            EchoWebSocketListener listener = new EchoWebSocketListener();
            Request request = new Request.Builder()
//                .url("ws://121.40.165.18:8800")
//                .url("ws://123.207.167.163:9010/ajaxchattest")
//                .url("ws://echo.websocket.org")
                    .url(address)
                    .build();
            OkHttpClient client = new OkHttpClient();
            // 刚连接就开启心跳检测
            handler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);
            webSocket = client.newWebSocket(request, listener);
            client.dispatcher().executorService();
        }
    }

    private final class EchoWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {

            webSocket.send("connect success!");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            output("onMessage: " + text);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("onMessage byteString: " + bytes);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(1000, null);
            output("onClosing: " + code + "/" + reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            output("onClosed: " + code + "/" + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("onFailure: " + t.getMessage());
            connect();
        }

        private void output(final String content) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvContent.setText(tvContent.getText().toString() + content + "\n");
                }
            });
        }
//---------------------
//        作者：xlh1191860939
//        来源：CSDN
//        原文：https://blog.csdn.net/xlh1191860939/article/details/75452342
//        版权声明：本文为博主原创文章，转载请附上博文链接！
    }

}

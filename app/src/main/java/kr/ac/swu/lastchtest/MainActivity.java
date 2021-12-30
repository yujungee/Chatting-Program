package kr.ac.swu.lastchtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener {

    // handler
    private static final int SERVER_TEXT_UPDATE = 100;
    private static final int CLIENT_TEXT_UPDATE = 200;

    private Button sendBtn;//버튼
    private TextView serverText;//서버채팅창
    private TextView clientText;//클라이언트 채팅창
    private EditText transClientText; // 메시지 보내는 editText;

    //서버세팅
    private String recvMessage; // 받아온 메시지
    private StringBuilder serverMsg = new StringBuilder();
    private StringBuilder clientMsgBuilder = new StringBuilder();

    //클라이언트 세팅
    private Socket clientSocket;
    private DataInputStream clientIn;
    private DataOutputStream clientOut;
    private String sendMessage; // 보낸 메시지

    //날짜 및 시간 형식 지정
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("(HH:mm:ss)");


    // Json으로 보낸 메시지, 받아온 메시지를 순서대로 저장할 것이다.
    JSONObject jObject1 = new JSONObject(); // 보낸 메시지
    JSONObject jObject2 = new JSONObject(); // 받아온 메시지
    // 순서 표시를 위한 숫자
    private int jsonNum1=0;
    private int JsonNum2=0;

    // 화면 업데이트를 위한 Handler
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msgg) {
            super.handleMessage(msgg);
            switch (msgg.what) {
                case CLIENT_TEXT_UPDATE: { // 클라이어트 화면
                    Date date = new Date();
                    String time1 = simpleDateFormat.format(date);
                    serverMsg.append(time1 + sendMessage + "\n");
                    serverText.setText(serverMsg.toString());
                }
                break;

                case SERVER_TEXT_UPDATE: { // 서버 화면
                    Date date = new Date();
                    String time2 = simpleDateFormat.format(date);
                    clientMsgBuilder.append(time2 + recvMessage + "\n");
                    clientText.setText(clientMsgBuilder.toString());
                }
                break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverText = (TextView) findViewById(R.id.client_text);
        clientText = (TextView) findViewById(R.id.server_text);
        transClientText = (EditText) findViewById(R.id.trans_client_text);
        sendBtn = (Button) findViewById(R.id.trans_client_button) ;
        sendBtn.setOnClickListener(this);

        joinServer(); // 서버 연결하는 함수
    }


    @Override
    public void onClick(View v) {
        sendMessage = transClientText.getText().toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientOut.writeUTF(sendMessage); // 쓰기
                    handler.sendEmptyMessage(CLIENT_TEXT_UPDATE); // 화면 업뎃
                    clientOut.flush(); // 비워주기
                    //Json
                    jObject1.put(Integer.toString(jsonNum1), sendMessage);
                    jsonNum1++;
                }catch (JSONException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                }
            }
        }).start();
        transClientText.setText("");

        // Json에 대화 순서대로 저장한 것 확인
//        System.out.println("job1" + jObject1);
//        System.out.println("job2" + jObject2);

    }

    // 서버 연결 함수
    public void joinServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    clientSocket = new Socket("192.168.25.61", 9976); // 연결
                    Log.v("joinServer", "클라이언트 : 서버 연결됨.");

                    clientOut = new DataOutputStream(clientSocket.getOutputStream()); // reader
                    clientIn = new DataInputStream(clientSocket.getInputStream()); // writer


                    while (clientIn != null) {
                        try {
                            recvMessage = clientIn.readUTF();
                            handler.sendEmptyMessage(SERVER_TEXT_UPDATE); // 화면 업뎃
                            //Json
                            jObject2.put( Integer.toString(JsonNum2), recvMessage);
                            JsonNum2++;
                        }catch (JSONException e) {
                            e.printStackTrace();
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            clientOut.close();
            clientIn.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


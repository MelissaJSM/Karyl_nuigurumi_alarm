// 주의! 블루투스 퍼미션이 제대로 작동하지 않는 경우 소스코드를 새로 추가해주길 바람! //

package com.example.karyl_ble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// 로딩값 스태틱으로 전부 공유 (넘기기가 잘 안되서 사용, 권장하지 않음.)
import static com.example.karyl_ble.DeviceListActivity.loading_value;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements BLeSerialPortService.Callback, View.OnClickListener, DataTransmissionListener {

    //SharedPreferences 활성화
    SharedPreferences ble_pref;
    SharedPreferences.Editor ble_editor;

    //설정, 알람용 버튼과 이미지 텍스트
    private ImageButton connect;
    private ImageView battery_img;
    private TextView battery_text;
    private ImageButton btn_setting;

    // 캬루 대화 및 외형 컨트롤 이미지
    private ImageView gif_karyl_blink;
    private ImageView speech_bubble;
    private ImageView gif_karyl_speak;
    private ImageView speech_text;

    //시간 표현용 이미지
    private ImageView img_hour_10;
    private ImageView img_hour_1;
    private ImageView img_min_10;
    private ImageView img_min_1;

    //알람 알림
    private ImageView img_alart;

    //설정 후 타이머 표시용
    private LinearLayout timer_layout;

    //블루투스용
    private BLeSerialPortService serialPort;
    private final int REQUEST_DEVICE = 3;
    private final int REQUEST_ENABLE_BT = 2;
    private int rindex = 0;

    //수신 저장용 가변 배열
    ArrayList<String> array_recieve = new ArrayList<String>();

    //이미지 로딩용
    ProgressDialog customProgressDialog;

    // 프래그먼트 라인
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private AlarmFragment AlarmFragment;
    private SettingFragment SettingFragment;


    //변수 라인//

    //변수 스태틱화
    static int loading_screen = 0; // 로딩할때 이미지 변환용

    //어드레스 주소 저장
    static String mac_address = null;


    int recieve_value = Constants.recieve_off;

    //수신 1,2라인 체크용
    int firstCheck = Constants.packet_no;
    int secondCheck = Constants.packet_no;

    //로딩 10초 카운트용
    int loading_count = 0;

    //로딩시 상황에따른 이미지 구현용
    int loading_window = Constants.window_off;

    //버튼 누른거에 따른 버튼 이미지 표시용
    int connect_alarm_value = Constants.connect_button;

    //자동 연결 변수
    int auto_connect = Constants.auto_connect_no;

    //시,분 저장용
    int save_hour10 = 0;
    int save_hour1 = 0;
    int save_min10 = 0;
    int save_min1 = 0;

    // 블루투스 연결 시도 핸들러
    Handler loading_Handler = new Handler();
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            serialPort = ((BLeSerialPortService.LocalBinder) rawBinder).getService()

                    //register the application context to service for callback
                    .setContext(getApplicationContext()).registerCallback(MainActivity.this);
        }

        public void onServiceDisconnected(ComponentName classname) {
            serialPort.unregisterCallback(MainActivity.this)
                    //Close the bluetooth gatt connection.
                    .close();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //프래그먼트 갱신
        fragmentManager = getSupportFragmentManager();
        AlarmFragment = new AlarmFragment();
        SettingFragment = new SettingFragment();

        // 변수와 id 연결 라인
        connect = (ImageButton) findViewById(R.id.connect);
        connect.setOnClickListener(this);


        btn_setting = (ImageButton) findViewById(R.id.btn_setting);
        btn_setting.setOnClickListener(this);

        battery_text = (TextView) findViewById(R.id.battery_text);
        battery_img = (ImageView) findViewById(R.id.battery_img);

        gif_karyl_blink = (ImageView) findViewById(R.id.gif_karyl_blink);
        speech_bubble = (ImageView) findViewById(R.id.speech_bubble);
        gif_karyl_speak = (ImageView) findViewById(R.id.gif_karyl_speak);
        speech_text = (ImageView) findViewById(R.id.speech_text);

        img_hour_10 = (ImageView) findViewById(R.id.img_hour_10);
        img_hour_1 = (ImageView) findViewById(R.id.img_hour_1);
        img_min_10 = (ImageView) findViewById(R.id.img_min_10);
        img_min_1 = (ImageView) findViewById(R.id.img_min_1);

        timer_layout = (LinearLayout) findViewById(R.id.timer_layout);

        img_alart = (ImageView) findViewById(R.id.alart_img);


        //블루투스 바인딩
        Intent bindIntent = new Intent(this, BLeSerialPortService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        //여기에 유저 프리패쳐로 값 불러오고
        ble_pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
        ble_editor = ble_pref.edit();

        mac_address = ble_pref.getString("mac_address", null);
        auto_connect = ble_pref.getInt("auto_connect", Constants.auto_connect_no);

        if (auto_connect == Constants.auto_connect_yes) {
            karyl_message(Constants.gif_karyl_connect_auto);
            move_DeviceListActivty();
        }

        karyl_blink();

    }

    //레주메
    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        // set the screen to portrait
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // if the bluetooth adatper is not support and enabled
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            finish();
        }

        // request to open the bluetooth adapter
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }


    @Override
    protected void onStop() {
        super.onStop();
        serialPort.stopSelf();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    //뒤로가기 버튼 눌렀을때
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.connect) { // id 매칭
            // the device can send data to
            if (connect_alarm_value == Constants.connect_button) { // 연결이안되서 나오는 버튼
                karyl_message(Constants.gif_connect_send);
                move_DeviceListActivty();
            }
            else if (connect_alarm_value == Constants.alarm_button) { // 연결이 되서 나오는 버튼
                karyl_message(Constants.gif_alarm_setting);
                transaction = fragmentManager.beginTransaction();
                transaction.setCustomAnimations(R.anim.wave, R.anim.wave);
                transaction.replace(R.id.main_background, AlarmFragment).commitAllowingStateLoss();
            }
            else if (connect_alarm_value == Constants.stop_button) { // 알람을 중지 시키는 버튼
                karyl_message(Constants.gif_alarm_stop_send);
                loading_screen = Constants.loading_screen_alarm_stop;
                ArrayList<Character> array_send = new ArrayList<Character>();
                array_send.add((char) Constants.send_start_packet);
                array_send.add((char) Constants.send_alarm_stop_packet);
                array_send.add((char) Character.forDigit(4, 10));
                array_send.add((char) Constants.send_end_packet);
                send_packet(array_send);
                loading_value = Constants.loading_on; // 로딩 패킷
                wait_packet(Constants.error_stop);
            }
            else if (connect_alarm_value == Constants.alart_button) { // 알람 울릴때 쓰는 버튼.
                karyl_message(Constants.gif_alarm_alart_stop_send);
                loading_screen = Constants.loading_screen_alart_alarm_end;
                ArrayList<Character> array_send = new ArrayList<Character>();
                array_send.add((char) Constants.send_start_packet);
                array_send.add((char) Constants.send_alarm_alart_end_packet);
                array_send.add((char) Character.forDigit(4, 10));
                array_send.add((char) Constants.send_end_packet);
                send_packet(array_send);
                loading_value = Constants.loading_on; // 로딩 패킷
                wait_packet(Constants.error_alart_end);
            }

        }

        else if (v.getId() == R.id.btn_setting) { // 세팅 메뉴 진입
            karyl_message(Constants.gif_setting_recieve);
            loading_value = Constants.loading_on; // 로딩 패킷
            loading_screen = Constants.loading_screen_setting_receive;
            wait_packet(Constants.error_setting);
            ArrayList<Character> array_send = new ArrayList<Character>();
            array_send.add((char) Constants.send_start_packet);
            array_send.add((char) Constants.send_setting_request_packet);
            array_send.add((char) Character.forDigit(4, 10));
            array_send.add((char) Constants.send_end_packet);
            send_packet(array_send);


        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_about) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //연결 성공시
    @Override
    public void onConnected(Context context) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connect = (ImageButton) findViewById(R.id.connect);
                connect.setImageResource(R.drawable.btn_alarm);
            }
        });
    }

    //연결 실패시
    @Override
    public void onConnectFailed(Context context) {
        //System.out.println("error conneect");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connect = (ImageButton) findViewById(R.id.connect);
                connect.setImageResource(R.drawable.btn_connect);
            }
        });
    }

    //연결이 끊여졌을시
    @Override
    public void onDisconnected(Context context) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connect_alarm_value = Constants.connect_button;
                connect = (ImageButton) findViewById(R.id.connect);
                connect.setImageResource(R.drawable.btn_connect);
            }
        });
    }

    //통신이 과도하게 지연될때
    @Override
    public void onCommunicationError(int status, String msg) {
        // get the send value bytes
        if (status > 0) {
        }// when the send process found error, for example the send thread  time out
        else {
            //System.out.println("통신이 장기간 길어지는걸 확인함.");
        }
    }

    //수신 받는곳
    @Override
    public void onReceive(Context context, BluetoothGattCharacteristic rx) {

        String msg;
        //아스키코드의 숫자영역만 필터해서 다르게 표시하기 위한 작업
        if (rx.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) >= 48 && rx.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) <= 57) {
            msg = Integer.toString(rx.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) - '0');
        }
        else {
            msg = "0x" + Integer.toHexString(rx.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            rindex = rindex + msg.length();
        }
        //System.out.println("받은 데이터의 정확한 값 : " + msg);

        //시작 값 필터.
        //시작값 들어온 이후에 끝 값 올때까지 계속 읽게 하기 위해 || 처리를 함.
        if (msg.contains(Constants.receive_start_packet) || recieve_value == Constants.recieve_on) {
            recieve_value = Constants.recieve_on; // 이후부터 끝까지 계속 값을 읽음
            array_recieve.add(msg);

            if (array_recieve.get(array_recieve.size() - 1).contains(Constants.receive_end_packet)) { //여긴 끝자리 체크
                if (array_recieve.size() == Integer.parseInt(array_recieve.get(array_recieve.size() - 2))) { // 여긴 배열크기 체크
                    //System.out.println("배열 정리로 진입");


                    if (array_recieve.get(1).contains(Constants.receive_first_start_packet)) { // 연결하고 인형에 저장된 값 불러오는곳 (첫번째 수신라인)
                        //System.out.println(" 현재 1차전송 값이 들어왔음.");

                        int battery_value = Integer.parseInt(array_recieve.get(2)) * 100 + Integer.parseInt(array_recieve.get(3)) * 10 + Integer.parseInt(array_recieve.get(4));
                        // 배터리 100자리 + 10자리 + 1자리.
                        battary_setting(battery_value);
                        firstCheck = Constants.packet_yes;
                        array_recieve.clear(); // 초기화 안하면 계속 쌓인다.
                    }
                    else if (firstCheck == Constants.packet_yes && array_recieve.get(1).contains(Constants.receive_second_start_packet)) {  // 연결하고 인형에 저장된 값 불러오는곳 (두번째 수신라인)
                        secondCheck = Constants.packet_yes;
                        //System.out.println(" 현재 2차전송 값이 들어왔음.");

                        if (firstCheck == Constants.packet_yes && secondCheck == Constants.packet_yes) { //둘 다 정상적으로 받아 왔을 경우
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    karyl_message(Constants.gif_connect_ok);
                                    btn_setting.setVisibility(View.VISIBLE);
                                    //여기에 지금 알람이 울리고있는지 확인하는 작업 필요.
                                    if (array_recieve.get(6).contains(Constants.receive_stay_alarm_packet)) { // 알람 버튼 설정 생성
                                        //System.out.println("현재 알람 아무것도 안함");
                                        connect_alarm_value = Constants.alarm_button;
                                    }
                                    else if (array_recieve.get(6).contains(Constants.receive_start_alarm_packet)) { // 알람 중지 버튼 생성
                                        connect_alarm_value = Constants.stop_button;
                                        connect.setImageResource(R.drawable.btn_alarm_stop);

                                        save_hour10 = Integer.parseInt(array_recieve.get(2));
                                        save_hour1 = Integer.parseInt(array_recieve.get(3));
                                        save_min10 = Integer.parseInt(array_recieve.get(4));
                                        save_min1 = Integer.parseInt(array_recieve.get(5));

                                        timer_img_set();

                                    }
                                    else if (array_recieve.get(6).contains(Constants.receive_alart_alarm_packet)) { // 알람 알림 버튼 생성
                                        img_alart.setVisibility(View.VISIBLE);
                                        connect_alarm_value = Constants.alart_button;
                                        connect.setImageResource(R.drawable.btn_alarm_alart); // 알람 멈춤 버튼으로 교체필요.
                                    }

                                    ArrayList<Character> array_send = new ArrayList<Character>();
                                    array_send.add((char) Constants.send_start_packet);
                                    array_send.add((char) Constants.send_connect_result_packet);
                                    array_send.add((char) firstCheck);
                                    array_send.add((char) secondCheck);
                                    array_send.add((char) Character.forDigit(6, 10));
                                    array_send.add((char) Constants.send_end_packet);
                                    send_packet(array_send);
                                    connect_end();

                                }
                            });

                            //System.out.println("연결 확인 완료 및 종료");
                        }


                    }
                    else if (array_recieve.get(1).contains(Constants.receive_battery_check_packet)) {
                        //System.out.println("배터리 체크 모드 진입");
                        int battery_value = Integer.parseInt(array_recieve.get(2)) * 100 + Integer.parseInt(array_recieve.get(3)) * 10 + Integer.parseInt(array_recieve.get(4));
                        battary_setting(battery_value);
                        array_recieve.clear();
                    }
                    else if (array_recieve.get(1).contains(Constants.receive_alarm_ok_packet)) {
                        //알람 세팅완료 후 표시기능
                        //System.out.println("알람 설정 완료 진입");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                timer_img_set();
                                karyl_message(Constants.gif_alarm_ok);
                                connect_alarm_value = Constants.stop_button;
                                connect.setImageResource(R.drawable.btn_alarm_stop);
                            }
                        });

                        connect_end();


                    }
                    else if (array_recieve.get(1).contains(Constants.receive_alarm_stop_ok_packet)) {
                        //알람 정지 이후 표시 기능
                        //System.out.println("알람 정지 메뉴 진입");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                timer_img_reset();
                                karyl_message(Constants.gif_alarm_stop_success);
                                connect_alarm_value = Constants.alarm_button;
                                connect.setImageResource(R.drawable.btn_alarm);
                            }
                        });
                        connect_end();

                    }
                    else if (array_recieve.get(1).contains(Constants.receive_setting_packet)) { // 세팅값 받아옴
                        //System.out.println("세팅값 받아옴");
                        int volume = (Integer.parseInt(array_recieve.get(2)) * 10) + Integer.parseInt(array_recieve.get(3));
                        String auto_alarm = array_recieve.get(4);
                        int music = Integer.parseInt(array_recieve.get(5));
                        int vib = Integer.parseInt(array_recieve.get(6));
                        Bundle Settingbundle = new Bundle(7); // 파라미터의 숫자는 전달하려는 값의 갯수
                        Settingbundle.putInt("volume", volume);
                        Settingbundle.putString("auto_alarm", auto_alarm);
                        Settingbundle.putInt("auto_connect", auto_connect);
                        Settingbundle.putInt("music", music);
                        Settingbundle.putInt("vib", vib);
                        SettingFragment.setArguments(Settingbundle);

                        connect_end();

                        //여기에 프래그먼트 이동할때 전달해야 할 값 처리필요.
                        transaction = fragmentManager.beginTransaction();
                        transaction.setCustomAnimations(R.anim.wave, R.anim.wave);
                        transaction.replace(R.id.main_background, SettingFragment).commitAllowingStateLoss();
                    }
                    else if (array_recieve.get(1).contains(Constants.receive_setting_ok_packet)) {
                        //알람 정지 이후 표시 기능
                        karyl_message(Constants.gif_setting_complete);
                        //System.out.println("세팅 확인 완료");
                        connect_end();

                    }
                    else if (array_recieve.get(1).contains(Constants.receive_alarm_alart_packet)) {
                        //여기에 울림 종료 버튼 추가랑
                        //울림버튼용 constant 추가랑
                        //알람 울렸다는 기능 추가.
                        //System.out.println("알람 울림 상태 진입");
                        connect_alarm_value = Constants.alart_button;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                img_alart.setVisibility(View.VISIBLE);
                                karyl_message(Constants.gif_alarm_alart);
                                connect.setImageResource(R.drawable.btn_alarm_alart);
                            }
                        });

                        array_recieve.clear();

                    }

                    else if (array_recieve.get(1).contains(Constants.receive_alarm_alart_end_packet) || array_recieve.get(1).contains(Constants.receive_alarm_shock_end_packet)) {
                        //여기엔 모든 ui를 초기화 하는 기능을 추가하면 돼.
                        //System.out.println("성공적으로 울리는 알람이 중지됨.");
                        connect_alarm_value = Constants.alarm_button;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (array_recieve.get(2).contains(Constants.receive_setting_auto_alarm_ok)) {
                                    // 반복중
                                    timer_img_repeat();
                                }
                                else if (array_recieve.get(2).contains(Constants.receive_setting_auto_alarm_no)) {
                                    timer_img_reset();
                                }
                                //여기에도 알람 이미지 인비지블 필요.
                                karyl_message(Constants.gif_alarm_alart_end_ok);
                                connect_end();
                            }
                        });
                    }

                    //debug
                    for (int i = 0; i < array_recieve.size(); i++) {
                        //System.out.println("처리된 배열 " + i + "값 : " + array_recieve.get(i));
                    }
                }
                else {
                    array_recieve.clear();
                }
                recieve_value = Constants.recieve_off;

            }


        }
    }

    //디바이스를 찾았을때
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        //System.out.println("디바이스를 찾았다.");
    }

    //디바이스의 정보
    @Override
    public void onDeviceInfoAvailable() {
        //System.out.println("디바이스 정보 : " + serialPort.getDeviceInfo());
    }

    //블루투스 체크
    @SuppressLint("MissingPermission")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    serialPort.connect(device);
                    showMessage(device.getName());
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                }
                else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private void showMessage(String msg) {
        try {
            Log.e(MainActivity.class.getSimpleName(), msg);
            //만약 여기서 에러가 발생했을경우 디바이스 이름이 없는 경우다.
        } catch (NullPointerException e) {
            Toast.makeText(this, "캬루가 주변에 없나봐요", Toast.LENGTH_SHORT).show();
        }
    }

    //설정한 패킷 보내는 곳
    private void send_packet(ArrayList<Character> array_send) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array_send.size(); i++) {
            sb.append(array_send.get(i));
        }

        String mergePacket = sb.toString();


        //System.out.println("mergePacket 값 : " + mergePacket);

        serialPort.send(mergePacket);


    }

    //패킷 전송을 시키고 10초동안 대기하는 애니메이션 생성하는 곳.
    private void wait_packet(int error_code) {
        loading_Handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (loading_value == Constants.loading_on) {
                    if (loading_window == Constants.window_off) {
                        //로딩창 객체 생성
                        customProgressDialog = new ProgressDialog(MainActivity.this);
                        //로딩창을 투명하게
                        customProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                        customProgressDialog.setCanceledOnTouchOutside(false);
                        customProgressDialog.show();
                        loading_window = Constants.window_on;
                    }
                    loading_count++;
                    if (loading_count > 1000) {
                        connect_failed(error_code);
                        connect_end();
                        //System.out.println("블루투스 연결 실패!");
                        loading_value = Constants.loading_off;
                        return;
                    }
                }
                loading_Handler.postDelayed(this, 10);
            }
        }, 0);

    }

    //10초 로딩화면 대기하다가 성공적으로 되었을경우 대기화면 삭제용
    private void connect_end() {
        customProgressDialog.dismiss(); // 로딩창 삭제
        loading_value = Constants.loading_off;
        loading_window = Constants.window_off;
        loading_count = 0;
        firstCheck = Constants.packet_no;
        secondCheck = Constants.packet_no;
        array_recieve.clear();
        loading_Handler.removeMessages(0);
        loading_Handler.removeCallbacksAndMessages(null);
    }

    //실시간 배터리 표시용
    private void battary_setting(int battery_value) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                battery_text.setText(Integer.toString(battery_value) + "%");
                if (battery_value >= 80 && battery_value <= 100) {
                    battery_img.setImageResource(R.drawable.img_battery5);
                }
                else if (battery_value >= 60 && battery_value <= 79) {
                    battery_img.setImageResource(R.drawable.img_battery4);
                }
                else if (battery_value >= 40 && battery_value <= 59) {
                    battery_img.setImageResource(R.drawable.img_battery3);
                }
                else if (battery_value >= 20 && battery_value <= 39) {
                    battery_img.setImageResource(R.drawable.img_battery2);
                }
                else if (battery_value >= 6 && battery_value <= 19) {
                    battery_img.setImageResource(R.drawable.img_battery1);
                }
                else if (battery_value >= 0 && battery_value <= 5) {
                    battery_img.setImageResource(R.drawable.img_battery0);
                }
            }
        });

    }

    //블루투스 연결로 넘기는 메소드
    private void move_DeviceListActivty() {
        loading_screen = Constants.loading_screen_connect;
        // if the device is not connectted
        Intent intent = new Intent(this, DeviceListActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("auto_connect", auto_connect); // 이 값을 intent 로 보내서 오토동작 명령어 시도 할 수 있음. (현재는 클릭으로 설정해놨지만, 키자마자 동작하게 설정 가능하니 이동 준비해라.)
        wait_packet(Constants.error_connect);
        startActivityForResult(intent, REQUEST_DEVICE);
    }

    //알람설정한 값을 인형에게 보내기 위한 메소드
    public void alarmTransmissionSet(int hour, int min) {
        //System.out.println("시간 : " + hour);
        //System.out.println("분 : " + min);
        //System.out.println(" 현재 2차전송 값이 들어왔음.");

        save_hour10 = hour / 10;
        save_hour1 = hour % 10;
        save_min10 = min / 10;
        save_min1 = min % 10;

        karyl_message(Constants.gif_alarm_send);

        loading_value = Constants.loading_on; // 로딩 패킷
        loading_screen = Constants.loading_screen_alarm_set;
        wait_packet(Constants.error_alarm);

        /* 시간 계산식
        설정된 시간이 현재 시간보다가 작을 경우
        (설정된시간) + 24 - 현재시간 = 알람시간.

        설정된 시간이 현재 시간보다가 클 경우
        설정된시간 - 현재시간. 계산끝.
        */

        //현재 시간을 구한 후 계산한다.
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy", Locale.KOREAN);
        Date nowDate = new Date();
        simpleDateFormat = new SimpleDateFormat("HH", Locale.KOREAN);
        int toDayHH = Integer.parseInt(simpleDateFormat.format(nowDate));
        simpleDateFormat = new SimpleDateFormat("mm", Locale.KOREAN);
        int toDaymin = Integer.parseInt(simpleDateFormat.format(nowDate));
        int timecount = 0;
        if (toDayHH > hour) {
            timecount = ((hour + 24) * 60 + min) - (toDayHH * 60 + toDaymin);
        }
        else {
            timecount = (hour * 60 + min) - (toDayHH * 60 + toDaymin);
        }

        //System.out.println("설정된 타임 카운트 : " + timecount);

        ArrayList<Character> array_send = new ArrayList<Character>();

        // 보내야 할 값이 2라인임.
        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                array_send.add((char) Constants.send_start_packet);
                array_send.add((char) Constants.send_timer_first_packet);
                array_send.add(Character.forDigit((hour / 10), 10));
                array_send.add(Character.forDigit((hour % 10), 10));
                array_send.add(Character.forDigit((min / 10), 10));
                array_send.add(Character.forDigit((min % 10), 10));
                array_send.add(Character.forDigit(8, 10));
                array_send.add((char) Constants.send_end_packet);
                send_packet(array_send);
            }
            else {
                array_send.clear();
                array_send.add((char) Constants.send_start_packet);
                array_send.add((char) Constants.send_timer_second_packet);
                array_send.add(Character.forDigit((timecount / 1000), 10));
                array_send.add(Character.forDigit((timecount % 1000 / 100), 10));
                array_send.add(Character.forDigit((timecount % 100 / 10), 10));
                array_send.add(Character.forDigit((timecount % 10), 10));
                array_send.add(Character.forDigit(8, 10));
                array_send.add((char) Constants.send_end_packet);
                send_packet(array_send);
            }
        }


    }

    //세팅메뉴에서 설정한 값을 인형에 보내는 메소드
    public void settingTransmissionSet(String auto_alarm, int volume, int setting_auto_connect, int music, int vib) {

        loading_value = Constants.loading_on; // 로딩 패킷
        loading_screen = Constants.loading_screen_setting_send;
        wait_packet(Constants.error_setting_send);

        karyl_message(Constants.gif_setting_send);

        // 보내버려
        ArrayList<Character> array_send = new ArrayList<Character>();

        array_send.add((char) Constants.send_start_packet);
        array_send.add((char) Constants.send_setting_packet);
        array_send.add(auto_alarm.charAt(0));
        array_send.add(Character.forDigit(volume / 10, 10));
        array_send.add(Character.forDigit(volume % 10, 10));
        array_send.add(Character.forDigit(music, 10));
        array_send.add(Character.forDigit(vib, 10)); // 아두이노가서 x5 해줘라.
        array_send.add(Character.forDigit(9, 10));
        array_send.add((char) Constants.send_end_packet);
        send_packet(array_send);

        //자동 연결 옵션
        if (setting_auto_connect == Constants.auto_connect_yes) {
            //유저 프리패쳐 넣어서 고정화.
            //무엇을? 2개 넣어야함. mac 주소 및 auto_connect
            ble_editor.putInt("auto_connect", setting_auto_connect);
            ble_editor.commit();
            ble_editor.putString("mac_address", mac_address);
            ble_editor.commit();
            auto_connect = Constants.auto_connect_yes;
        }
        else if (setting_auto_connect == Constants.auto_connect_no) {
            //유저 프리패쳐 넣어서 고정화.
            //무엇을? 1개 넣어야함. auto_connect
            ble_editor.putInt("auto_connect", setting_auto_connect);
            ble_editor.commit();
            auto_connect = Constants.auto_connect_no;
        }

    }

    //로딩화면 10초 넘어갔을때 에러안내 표시하는 메소드
    private void connect_failed(int error_code) {
        if (error_code == Constants.error_connect) {
            Toast.makeText(this, "캬루와의 연결에 실패했습니다.\n다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            karyl_message(Constants.gif_karyl_connect_failed);
        }
        else if (error_code == Constants.error_setting) {
            Toast.makeText(this, "세팅값 수신에 실패했습니다.\n다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            karyl_message(Constants.gif_setting_recieve_failed);
        }
        else if (error_code == Constants.error_alarm) {
            Toast.makeText(this, "알람값 전송에 실패했습니다.\n다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            timer_img_reset();
            karyl_message(Constants.gif_alarm_failed);
        }
        else if (error_code == Constants.error_stop) {
            Toast.makeText(this, "알람 정지에 실패했습니다.\n다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            karyl_message(Constants.gif_alarm_stop_failed);

        }
        else if (error_code == Constants.error_setting_send) {
            Toast.makeText(this, "세팅값 전송에 실패했습니다.\n다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            karyl_message(Constants.gif_setting_failed);
        }
        else if (error_code == Constants.error_alart_end) {
            Toast.makeText(this, "알람 중지를 실패했습니다.\n다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            karyl_message(Constants.gif_alarm_alart_end_failed);
        }
    }

    //캬루 눈 깜빡이는 메소드
    private void karyl_blink() {
        Glide.with(this).asGif().load(R.drawable.gif_karyl_blinking).into(gif_karyl_blink);
    }

    // 캬루 말풍선에 적혀나올 내용
    private void karyl_message(int message) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int message_type = 9999; // 9999인 이유? 0하니 겹쳐서
                for (int i = 0; i < 20; i++) {
                    if (i == message)
                        ;
                    message_type = message;
                }

                TypedArray typedArray_karyl_message;
                typedArray_karyl_message = getResources().obtainTypedArray(R.array.message_gif); //chara_select가 배열 번호가된다.

                Glide.with(MainActivity.this).asGif().load(R.drawable.gif_karyl_speak).into(gif_karyl_speak);
                Glide.with(MainActivity.this).asGif().load(typedArray_karyl_message.getResourceId(message_type, -1)).listener(new RequestListener<GifDrawable>() {

                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (resource != null) {
                            resource.setLoopCount(1);
                        }
                        speech_bubble.setVisibility(View.VISIBLE);
                        speech_text.setVisibility(View.VISIBLE);
                        gif_karyl_speak.setVisibility(View.VISIBLE);
                        resource.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                            @Override
                            public void onAnimationEnd(Drawable drawable) {
                                //in your case start new Activity
                                speech_bubble.setVisibility(View.INVISIBLE);
                                speech_text.setVisibility(View.INVISIBLE);
                                gif_karyl_speak.setVisibility(View.INVISIBLE);
                            }
                        });
                        return false;
                    }
                }).into(speech_text);
            }
        });
    }

    // 타이머 설정 후 이미지 설정
    private void timer_img_set() {
        TypedArray typedArray_karyl_timer_num;
        typedArray_karyl_timer_num = getResources().obtainTypedArray(R.array.png_num); //chara_select가 배열 번호가된다.
        timer_layout.setVisibility(View.VISIBLE);
        img_hour_10.setImageResource(typedArray_karyl_timer_num.getResourceId(save_hour10, -1));
        img_hour_1.setImageResource(typedArray_karyl_timer_num.getResourceId(save_hour1, -1));
        img_min_10.setImageResource(typedArray_karyl_timer_num.getResourceId(save_min10, -1));
        img_min_1.setImageResource(typedArray_karyl_timer_num.getResourceId(save_min1, -1));
    }

    //타이머 취소 후 이미지 설정
    private void timer_img_reset() {
        TypedArray typedArray_karyl_timer_num;
        typedArray_karyl_timer_num = getResources().obtainTypedArray(R.array.png_num); //chara_select가 배열 번호가된다.
        //System.out.println("리셋 진입 성공");
        save_hour1 = 0;
        save_hour10 = 0;
        save_min1 = 0;
        save_min10 = 0;
        timer_layout.setVisibility(View.INVISIBLE);
        img_alart.setVisibility(View.INVISIBLE);
        connect.setImageResource(R.drawable.btn_alarm);
        connect_alarm_value = Constants.alarm_button;
        img_hour_10.setImageResource(typedArray_karyl_timer_num.getResourceId(save_hour10, -1));
        img_hour_1.setImageResource(typedArray_karyl_timer_num.getResourceId(save_hour1, -1));
        img_min_10.setImageResource(typedArray_karyl_timer_num.getResourceId(save_min10, -1));
        img_min_1.setImageResource(typedArray_karyl_timer_num.getResourceId(save_min1, -1));
    }

    // 타이머가 종료되었으나 반복옵션이 켜져있는 상태의 이미지 설정
    private void timer_img_repeat() {
        //System.out.println("repeat 진입 성공");
        TypedArray typedArray_karyl_timer_num;
        typedArray_karyl_timer_num = getResources().obtainTypedArray(R.array.png_num); //chara_select가 배열 번호가된다.

        connect_alarm_value = Constants.stop_button;
        connect.setImageResource(R.drawable.btn_alarm_stop);
        img_alart.setVisibility(View.INVISIBLE);
        img_hour_10.setImageResource(typedArray_karyl_timer_num.getResourceId(save_hour10, -1));
        img_hour_1.setImageResource(typedArray_karyl_timer_num.getResourceId(save_hour1, -1));
        img_min_10.setImageResource(typedArray_karyl_timer_num.getResourceId(save_min10, -1));
        img_min_1.setImageResource(typedArray_karyl_timer_num.getResourceId(save_min1, -1));
    }


}

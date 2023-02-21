package com.example.karyl_ble;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import static com.example.karyl_ble.MainActivity.loading_screen;
import static com.example.karyl_ble.MainActivity.mac_address;
public class SettingFragment extends Fragment {

    MainActivity mainActivity; //(액티비티에서 이동하기) 주 가되는 메인액티비티 선언

    private DataTransmissionListener dataTransmissionSetListener; // 값 전송용 변수 선언

    String auto_alarm = null;
    int volume = 0;

    int vib = 0;

    int music = 0;

    int setting_auto_connect = Constants.auto_connect_no;

    int reset_ready = Constants.reset_off;

    Spinner spinner_music;

    //어땃쥐
    //화면이 붙을때 작동하는 메서드
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);


        mainActivity = (MainActivity) getActivity(); //(액티비티에서 이동하기) 현재 소속된 액티비티를 메인 액티비티로 한다.

        if (context instanceof DataTransmissionListener) {
            dataTransmissionSetListener = (DataTransmissionListener) context; // context 처리해서 값 불러오기
        } else {
            throw new RuntimeException(context.toString() + "must implement dataTransmissionSetListener");
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback); // 뒤로가기 버튼 작업
    }

    //뒤로가기 버튼 작업
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            FragmentManager manager = getActivity().getSupportFragmentManager();
            manager.beginTransaction().remove(SettingFragment.this).commit();
            manager.popBackStack();
        }
    };

    @Override
    public void onDetach() {
        super.onDetach();
        dataTransmissionSetListener = null; // null 로 반환처리
    }





    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View inflaterSetting = inflater.inflate(R.layout.fragment_setting, container, false); // inflater 때문에 선언을 추가해야함

        if (getArguments() != null) {
            //액티비티에서 프래그먼트로 값 받아오기
            Bundle advance_bundle = getArguments();

            if (advance_bundle != null) {
                get_setting_fragment_data(advance_bundle);
            }
        }


        // 이미지 id 연결용
        LinearLayout auto_connect_layout = inflaterSetting.findViewById(R.id.auto_connect_layout); // 자동연결
        LinearLayout connect_lock_layout = inflaterSetting.findViewById(R.id.connect_lock_layout); // 설정 표시 on/off

        TextView text_volume = inflaterSetting.findViewById(R.id.text_volume); // 볼륨값 텍스트
        TextView text_vib = inflaterSetting.findViewById(R.id.text_vib); // 충격센서 레벨 텍스트

        SeekBar sound_seekBar = inflaterSetting.findViewById(R.id.sound_seekbar); // 소리 시크바
        SeekBar vib_seekbar = inflaterSetting.findViewById(R.id.vib_seekbar); // 충격 감도 시크바

        //자동 알람 설정 버튼
        ImageButton btn_auto_alarm_yes = inflaterSetting.findViewById(R.id.btn_auto_alarm_yes);
        ImageButton btn_auto_alarm_no = inflaterSetting.findViewById(R.id.btn_auto_alarm_no);

        //설정 적용 버튼
        ImageButton ok_exit_button = inflaterSetting.findViewById(R.id.ok_exit_button);
        ImageButton cancel_exit_button = inflaterSetting.findViewById(R.id.cancel_exit_button);

        //자동 연결 설정 버튼
        ImageButton btn_auto_connect_yes = inflaterSetting.findViewById(R.id.btn_auto_connect_yes);
        ImageButton btn_auto_connect_no = inflaterSetting.findViewById(R.id.btn_auto_connect_no);

        //한번 연결해서 기억하고있는 연결 데이터 삭제용
        ImageButton reset_button = inflaterSetting.findViewById(R.id.reset_button);
        TextView reset_text = inflaterSetting.findViewById(R.id.reset_text);


        //스피너 기능을 통한 음악리스트 표시용
        spinner_music = inflaterSetting.findViewById(R.id.spinner_music);
        ArrayAdapter monthAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.karyl_music, R.layout.custom_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_music.setAdapter(monthAdapter); //어댑터에 연결해줍니다.
        spinner_music.setSelection(music); // 인형에 설정된 값을 기본값으로 설정
        spinner_music.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //System.out.println("클릭 한 값 확인용 : "+position);
                music = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //System.out.println("아무것도 안가져옴");
            }
        });


        //System.out.println("자동알람 설정 영역 : "+auto_alarm);
        //System.out.println("볼륨 설정 영역 : "+volume);

        //불러온 세팅값 검증영역
        if(auto_alarm.contains(Constants.receive_setting_auto_alarm_ok)){
            btn_auto_alarm_yes.setImageResource(R.drawable.button_p);
            btn_auto_alarm_no.setImageResource(R.drawable.button_non);
            auto_alarm="y";
        }
        else if(auto_alarm.contains(Constants.receive_setting_auto_alarm_no)) {
            btn_auto_alarm_no.setImageResource(R.drawable.button_p);
            btn_auto_alarm_yes.setImageResource(R.drawable.button_non);
            auto_alarm="n";
        }

        //자동연결 관련 초기세팅부분
        if(setting_auto_connect == Constants.auto_connect_yes){
            btn_auto_connect_yes.setImageResource(R.drawable.button_p);
            btn_auto_connect_no.setImageResource(R.drawable.button_non);
        }
        else if(setting_auto_connect == Constants.auto_connect_no){
            btn_auto_connect_no.setImageResource(R.drawable.button_p);
            btn_auto_connect_yes.setImageResource(R.drawable.button_non);
        }

        //mac 주소 관련 부분 (사실 이부분 필요없는데 혹시나해서)
        if(mac_address == null){
            auto_connect_layout.setVisibility(View.INVISIBLE);
            connect_lock_layout.setVisibility(View.VISIBLE);
            erase_connect();
            reset_text.setText("연결 되었던 기록이 없습니다.");
        }
        else{
            auto_connect_layout.setVisibility(View.VISIBLE);
            connect_lock_layout.setVisibility(View.INVISIBLE);
            reset_text.setText("연결 되었던 기록이 있습니다.");
        }

        //사운드 설정 부분
        sound_seekBar.setProgress(volume);
        text_volume.setText(Integer.toString(volume));
        sound_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //시크바를 조작하고 있는 중
                //System.out.println("조작 중 값 : " +sound_seekBar.getProgress());

                text_volume.setText(Integer.toString(sound_seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //시크바를 처음 터치
                //System.out.println("처음 터치 값 : " +sound_seekBar.getProgress());
                text_volume.setText(Integer.toString(sound_seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //시크바 터치가 끝났을 때
                //System.out.println("터치 끝 값 : " +sound_seekBar.getProgress());
                text_volume.setText(Integer.toString(sound_seekBar.getProgress()));
                volume = sound_seekBar.getProgress();
            }
        });


        //진동센서 감도 조절용
        vib_seekbar.setProgress(vib);
        text_vib.setText(Integer.toString(vib));
        vib_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //시크바를 조작하고 있는 중
                //System.out.println("조작 중 값 : " +vib_seekbar.getProgress());

                text_vib.setText(Integer.toString(vib_seekbar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //시크바를 처음 터치
                //System.out.println("처음 터치 값 : " +vib_seekbar.getProgress());
                text_vib.setText(Integer.toString(vib_seekbar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //시크바 터치가 끝났을 때
                //System.out.println("터치 끝 값 : " +vib_seekbar.getProgress());
                text_vib.setText(Integer.toString(vib_seekbar.getProgress()));
                vib = vib_seekbar.getProgress();
            }
        });


        //자동알람 설정 버튼
        btn_auto_alarm_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_auto_alarm_yes.setImageResource(R.drawable.button_p);
                btn_auto_alarm_no.setImageResource(R.drawable.button_non);
                auto_alarm = "y";
            }
        });
        btn_auto_alarm_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_auto_alarm_no.setImageResource(R.drawable.button_p);
                btn_auto_alarm_yes.setImageResource(R.drawable.button_non);
                auto_alarm = "n";
            }
        });


        //자동연결 설정 버튼
        btn_auto_connect_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_auto_connect_yes.setImageResource(R.drawable.button_p);
                btn_auto_connect_no.setImageResource(R.drawable.button_non);
                setting_auto_connect = Constants.auto_connect_yes;
            }
        });
        btn_auto_connect_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_auto_connect_no.setImageResource(R.drawable.button_p);
                btn_auto_connect_yes.setImageResource(R.drawable.button_non);
                setting_auto_connect = Constants.auto_connect_no;
            }
        });


        //연결되었던 기록 삭제 버튼
        reset_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset_ready = Constants.reset_on;
                auto_connect_layout.setVisibility(View.INVISIBLE);
                connect_lock_layout.setVisibility(View.VISIBLE);
                reset_text.setText("이제 확인을 누르시면 기록이 삭제됩니다.");
            }
        });


        //설정 적용 버튼
        ok_exit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loading_screen = Constants.loading_screen_setting_send;
                dataTransmissionSetListener.settingTransmissionSet(auto_alarm, volume, setting_auto_connect, music, vib);
                if(reset_ready == Constants.reset_on) {
                    erase_connect();
                }
                FragmentManager manager = getActivity().getSupportFragmentManager();
                manager.beginTransaction().remove(SettingFragment.this).commit();
                manager.popBackStack();
            }
        });
        cancel_exit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getActivity().getSupportFragmentManager();
                manager.beginTransaction().remove(SettingFragment.this).commit();
                manager.popBackStack();
            }
        });


        return inflaterSetting;
    }

    //메인화면에서 받아온 값 적용 메소트
    private void get_setting_fragment_data(Bundle advance_bundle) {
        auto_alarm = advance_bundle.getString("auto_alarm", "?"); // 프래그먼트 or 액티비티 이동 구분 변수
        volume = advance_bundle.getInt("volume", 0); // 프래그먼트 or 액티비티 이동 구분 변수
        setting_auto_connect = advance_bundle.getInt("auto_connect",Constants.auto_connect_no);
        music = advance_bundle.getInt("music", music);
        vib = advance_bundle.getInt("vib", vib);
    }

    //연결되었던 기록을 삭제하는 레이아웃
    private void erase_connect(){
        mac_address = null;
        setting_auto_connect = Constants.auto_connect_no;
    }

}
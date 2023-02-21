package com.example.karyl_ble;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TimePicker;


public class AlarmFragment extends Fragment {

    MainActivity mainActivity; //(액티비티에서 이동하기) 주 가되는 메인액티비티 선언

    private DataTransmissionListener dataTransmissionSetListener; // 값 전송용 변수 선언

    TimePicker time_picker; // 다이얼 타입의 시간 설정

    ImageButton set_button; // 설정 버튼

    ImageButton cancel_button; // 취소 버튼

    int setHour = 0; // 시
    int setMinute = 0; // 분

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
            manager.beginTransaction().remove(AlarmFragment.this).commit();
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
        View inflaterAlarm = inflater.inflate(R.layout.fragment_alarm, container, false); // inflater 때문에 선언을 추가해야함



        // xml 연결
        time_picker = inflaterAlarm.findViewById(R.id.time_picker);
        time_picker.setIs24HourView(true);

        set_button = inflaterAlarm.findViewById(R.id.set_button);
        cancel_button = inflaterAlarm.findViewById(R.id.cancel_button);


        //시간 다이얼 돌라가는 용도
        time_picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int hour, int minute) {
                // 오전 / 오후 를 확인하기 위한 if 문
                setHour = hour;
                setMinute = minute;

            }
        });

        //설정 버튼
        set_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                //System.out.println("설정된 시간 : "+setHour);
                //System.out.println("설정된 분 : "+setMinute);

                //값은 인터페이스를 통하여 메인 액티비티에서 적용하도록 설정.
                dataTransmissionSetListener.alarmTransmissionSet(setHour,setMinute);
                FragmentManager manager = getActivity().getSupportFragmentManager();
                manager.beginTransaction().remove(AlarmFragment.this).commit();
                manager.popBackStack();

            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                //System.out.println("설정 안했음");
                // 아무것도 안함.
                FragmentManager manager = getActivity().getSupportFragmentManager();
                manager.beginTransaction().remove(AlarmFragment.this).commit();
                manager.popBackStack();

            }
        });

        return inflaterAlarm;
    }
}
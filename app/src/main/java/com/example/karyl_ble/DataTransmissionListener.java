package com.example.karyl_ble;

public interface DataTransmissionListener {
    //void dataTransmissionSet(int character_select_num);

    //void fragmenttempData(int move_another_fragment, int character_select_num);

   void alarmTransmissionSet(int hour, int min);


   void settingTransmissionSet(String auto_alarm, int volume, int auto_connect, int music, int vib);
}

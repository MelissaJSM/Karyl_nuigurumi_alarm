package com.example.karyl_ble;

public final class Constants {

   //시작패킷부터 끝 패킷까지 왔을경우
   public static final int recieve_on = 1;
   public static final int recieve_off = 0;

   // 로딩 화면 전환용
   public static final int loading_off = 0;
   public static final int loading_on = 1;

   //로딩 화면 표시용
   public static final int window_off = 0;
   public static final int window_on = 1;

   //로딩화면 종류
   public static final int loading_screen_non = 99;
   public static final int loading_screen_connect = 0;
   public static final int loading_screen_alarm_set = 1;
   public static final int loading_screen_alarm_stop = 2;
   public static final int loading_screen_setting_receive = 3;
   public static final int loading_screen_alart_alarm_end = 4;
   public static final int loading_screen_setting_send = 5;


   // gif 종류
   public static final int gif_karyl_connect_auto = 0;
   public static final int gif_karyl_connect_failed = 1;
   public static final int gif_connect_ok = 2;
   public static final int gif_connect_send = 3;

   public static final int gif_setting_recieve = 4;
   public static final int gif_setting_send = 5;
   public static final int gif_setting_complete = 6;
   public static final int gif_setting_failed = 7;

   public static final int gif_alarm_setting = 8;
   public static final int gif_alarm_send = 9;
   public static final int gif_alarm_ok = 10;
   public static final int gif_alarm_failed = 11;

   public static final int gif_alarm_stop_failed = 12;
   public static final int gif_alarm_stop_success = 13;
   public static final int gif_alarm_stop_send = 14;
   public static final int gif_setting_recieve_failed = 15;



   public static final int gif_alarm_alart = 16;
   public static final int gif_alarm_alart_stop_send = 17;
   public static final int gif_alarm_alart_end_ok = 18;
   public static final int gif_alarm_alart_end_failed = 19;

   public static final int gif_welcome_morning = 20;
   public static final int gif_welcome_afternoon = 21;
   public static final int gif_welcome_dinner = 22;
   public static final int gif_welcome_dun = 23;

   public static final int gif_hit = 24;



   //자동연결
   public static final int auto_connect_no = 0;
   public static final int auto_connect_yes = 1;

   //리셋
   public static final int reset_off = 0;
   public static final int reset_on = 1;



   //연결버튼, 알람버튼 스위치 값
   public static final int connect_button = 0;
   public static final int alarm_button = 1;
   public static final int stop_button = 2;
   public static final int alart_button = 3;





   // 연결 실패시 에러코드 분류
   public static final int error_connect = 1;
   public static final int error_setting = 2;
   public static final int error_alarm = 3;
   public static final int error_stop = 4;
   public static final int error_alart_end = 5;
   public static final int error_setting_send = 6;

   //첫번째 두번째 패킷 체크값
   public static final int packet_no  = 0x66;
   public static final int packet_yes = 0x6f;

   //아스키 코드 16진수표 수신
   public static final String receive_start_packet = "0x41";
   public static final String receive_end_packet = "0x5a";
   public static final String receive_first_start_packet = "0x46";
   public static final String receive_second_start_packet = "0x47";
   public static final String receive_battery_check_packet = "0x42";
   public static final String receive_alarm_ok_packet = "0x54";
   public static final String receive_alarm_stop_ok_packet = "0x58";
   public static final String receive_setting_packet = "0x52";
   public static final String receive_setting_ok_packet = "0x53";

   public static final String receive_stay_alarm_packet = "0x62";
   public static final String receive_start_alarm_packet = "0x69";
   public static final String receive_alart_alarm_packet = "0x65";

   public static final String receive_alarm_alart_packet = "0x57";
   public static final String receive_alarm_alart_end_packet = "0x59";
   public static final String receive_alarm_shock_end_packet = "0x51";

   public static final String receive_setting_auto_alarm_ok = "0x79";
   public static final String receive_setting_auto_alarm_no = "0x6e";

   //아스키 코드 16진수표 송신
   public static final int send_start_packet = 0x41;
   public static final int send_end_packet = 0x5a;
   public static final int send_connect_result_packet = 0x46;
   public static final int send_timer_first_packet = 0x54;
   public static final int send_timer_second_packet = 0x55;
   public static final int send_setting_packet = 0x53;
   public static final int send_alarm_stop_packet = 0x58;
   public static final int send_alarm_alart_end_packet = 0x59;
   public static final int send_setting_request_packet = 0x52;



}

#if !( ARDUINO_ARCH_NRF52840 && TARGET_NAME == ARDUINO_NANO33BLE )
  #error This code is designed to run on nRF52-based Nano-33-BLE boards using mbed-RTOS platform! Please check your Tools->Board setting.
#endif

#define TIMER_INTERRUPT_DEBUG         1
#define _TIMERINTERRUPT_LOGLEVEL_     3

///////////////////////////////////////////////////////////////////
///////////////////////////디파인 영역//////////////////////////////
///////////////////////////////////////////////////////////////////

#define PACKET_START                  0x41 //a
#define PACKET_END                    0x5a //z  

#define PACKET_NORMAL_BATTERY_MODE    0x42 //b

#define PACKET_FIRST_CONNECT_MODE     0x46 //f
#define PACKET_SECOND_CONNECT_MODE    0x47 //g

#define PACKET_FIRST_TIME_MODE        0x54 //t
#define PACKET_SECOND_TIME_MODE       0x55 //u

#define PACKET_STOP_ALARM             0x58 //x

#define PACKET_SAVE_SETTING_SEND      0x52 //r

#define PACKET_SETTING_RECIEVE        0x53 //s

//현재 사용은 안하고있음
//#define PACKET_ERROR                  0x63 //c 

#define PACKET_NULL                   0x6e  //n

#define PACKET_FAIL                   0x66  //f
#define PACKET_OK                     0X6f  //o

//알람 울림 확인용
#define PACKET_STAY_ALARM		          0x62
#define PACKET_START_ALARM		        0x69
#define PACKET_ALART_ALARM		        0x65

#define PACKET_ALARM_ALART            0x57 //W
#define PACKET_ALARM_ALART_END        0x59 //Y

#define PACKET_ALARM_SHOCK_END        0x51

//송신 영역

#define SEND_NORMAL_BATTERY           0
#define SEND_FIRST_CONNECT            1
#define SEND_TIME_OK_FAIL             2
#define SEND_SAVE_SETTING             3
#define SEND_SETTING_RESULT           4
#define SEND_PAUSE_RESULT             5
#define SEND_ALARM_ALART              6
#define SEND_ALARM_ALART_END          7
#define SEND_ALARM_SHOCK_END          8
#define SEND_ERROR                    99



//이건 수신받은거
//볼륨은 int 값이라 없다.
#define PACKET_REPEAT_NO              0x6e //n
#define PACKET_REPEAT_YES             0X79 //y


//점점 증가하는 카운트 초기화
#define BATTERY_DELAY_RESET           0

//배터리 측정 기준값
#define BATTERY_TIME_DELAY_NORMAL     6000
#define BATTERY_TIME_DELAY_CONNECT    100       

//첫연결 체크
#define FIRST_CONNECT_NO              0
#define FIRST_CONNECT_YES             1

#define CHANGE_CHAR                   0x00
#define CHANGE_NUM                    '0'

#define STAY_ALARM                    0
#define START_ALARM                   1
#define ALART_ALARM                   2

//플레이 뮤직 define 은 음악 완성하면 설정하자.

#define SOUND_CONNECT                 1
#define SOUND_DISCONNECT              2
#define SOUND_SETTING_RECEIVE         3
#define SOUND_SETTING_SEND            4
#define SOUND_ALARM_SEND_OK           5
#define SOUND_HIT                     6
#define SOUND_LOW_BATTERY             7
#define SOUND_STOP                    8
#define SOUND_ALARM_ALART_STOP        9
#define SOUND_ALARM_SOUND1            10
#define SOUND_ALARM_SOUND2            11
#define SOUND_ALARM_SOUND3            12
#define SOUND_ALARM_SOUND3            13


#include <ArduinoBLE.h>
#include <DFMiniMp3.h>
#include "NRF52_MBED_TimerInterrupt.h"

///////////////////////////////////////////////////////////////////
////////////////////////변수 선언 영역//////////////////////////////
///////////////////////////////////////////////////////////////////

int batteryDelay = 10000; // 점점 증가해서 batteryTimeDelay까지 차오르면 동작하는 트리거를 위한 변수
int batteryTimeDelay = BATTERY_TIME_DELAY_NORMAL; // 배터리 시간 기준점 변수


int batteryLevel = 0; // 실제 배터리 퍼센트 저장하는 변수

int firstConnect = FIRST_CONNECT_NO; // 어플 실행 후 블루투스 초기 연결 / 연속 연결 구분용

//시간 패킷별로 분할
int hour10 = PACKET_NULL; // n
int hour1 = PACKET_NULL; // n
int min10 = PACKET_NULL; // n
int min1 = PACKET_NULL; // n

int timerValue = 0; // 안드로이드에서 요청한 타임 카운트 값

// 타이머 패킷을 안드로이드로 보낼때 패킷이 너무많아서 분할함. 
int timer1st = PACKET_FAIL; // f
int timer2nd = PACKET_FAIL; // f

//숫자와 문자 구분용
char charToNumber = CHANGE_CHAR;

int repeatAlarm = PACKET_REPEAT_NO; // n

uint16_t volume = 15; // 1~30

// 볼륨, 알람 반복값 정확하게 보내졌는지 체크용
int volume_check = PACKET_FAIL; // f
int repeat_check = PACKET_FAIL; // f

// 알람 중단값 정하게 보내졌는지 체크용
int pause_check = PACKET_FAIL; // f

int alarm_start_check = STAY_ALARM;

int sensor_cut = 100; // 기본이 100

int karyl_music = 10;

///////////////////////////////////////////////////////////////////
////////////////////////타이머 인터럽트 영역////////////////////////
///////////////////////////////////////////////////////////////////

#define TIMER1_INTERVAL_MS        60000   //1초 타이머 인터럽트

volatile uint32_t Timer1Count = 0; // 1초마다 점점 증가해서 timerInterruptvalue와 같아지면 알람 울림.

// Init NRF52 timer NRF_TIMER1
NRF52_MBED_Timer ITimer1(NRF_TIMER_3);

int lockOn = 0; // 타이머인터럽트 동작중지용

int timerInterruptvalue = 0; // 타이머 인터럽트용 남은시간 계산용 변수

//루프에서 타이머 시간 다되는지 검증용
void printResult()
{
  //Serial.print(F(", timerInterruptvalue = "));
  //Serial.print(timerInterruptvalue);
  //Serial.println();
  if(timerInterruptvalue<0){
    Timer1Count = 0;
    timerInterruptvalue = 0;
    playMusic(karyl_music); // 해당부분은 알람 재생임.
    timerEnd();
  }
  
}

void TimerHandler1()
{
  // Flag for checking to be sure ISR is working as //Serial.print is not OK here in ISR
  Timer1Count++;
  timerInterruptvalue = timerValue - Timer1Count;
}

void InterruptForPause()
{
  
  while (lockOn){
  }
}


///////////////////////////////////////////////////////////////////
//////////////////////////dfplayer 영역////////////////////////////
///////////////////////////////////////////////////////////////////

class Mp3Notify; 

typedef DFMiniMp3<HardwareSerial, Mp3Notify> DfMp3; 

DfMp3 dfmp3(Serial1);

class Mp3Notify
{
public:
  static void PrintlnSourceAction(DfMp3_PlaySources source, const char* action)
  {
    if (source & DfMp3_PlaySources_Sd) 
    {
        //Serial.print("SD Card, ");
    }
    if (source & DfMp3_PlaySources_Usb) 
    {
        //Serial.print("USB Disk, ");
    }
    if (source & DfMp3_PlaySources_Flash) 
    {
        //Serial.print("Flash, ");
    }
    //Serial.println(action);
  }
  static void OnError([[maybe_unused]] DfMp3& mp3, uint16_t errorCode)
  {
    // see DfMp3_Error for code meaning
    //Serial.println();
    //Serial.print("Com Error ");
    //Serial.println(errorCode);
  }
  static void OnPlayFinished([[maybe_unused]] DfMp3& mp3, [[maybe_unused]] DfMp3_PlaySources source, uint16_t track)
  {
    //Serial.print("Play finished for #");
    //Serial.println(track);  
  }
  static void OnPlaySourceOnline([[maybe_unused]] DfMp3& mp3, DfMp3_PlaySources source)
  {
    PrintlnSourceAction(source, "online");
  }
  static void OnPlaySourceInserted([[maybe_unused]] DfMp3& mp3, DfMp3_PlaySources source)
  {
    PrintlnSourceAction(source, "inserted");
  }
  static void OnPlaySourceRemoved([[maybe_unused]] DfMp3& mp3, DfMp3_PlaySources source)
  {
    PrintlnSourceAction(source, "removed");
  }
};


///////////////////////////////////////////////////////////////////
//////////////////////////블루투스 영역/////////////////////////////
///////////////////////////////////////////////////////////////////

// Device name
const char* nameOfPeripheral = "Karyl_BLE";
const char* uuidOfService = "0000fff0-0000-1000-8000-00805f9b34fb";
const char* uuidOfRxChar = "0000fff2-0000-1000-8000-00805f9b34fb";
const char* uuidOfTxChar = "0000fff1-0000-1000-8000-00805f9b34fb";

// BLE Service
BLEService microphoneService(uuidOfService);

// Setup the incoming data characteristic (RX).
const int WRITE_BUFFER_SIZE = 256;
bool WRITE_BUFFER_FIZED_LENGTH = false;

// RX / TX Characteristics
BLECharacteristic rxChar(uuidOfRxChar, BLEWriteWithoutResponse | BLEWrite, WRITE_BUFFER_SIZE, WRITE_BUFFER_FIZED_LENGTH);
BLEByteCharacteristic txChar(uuidOfTxChar, BLERead | BLENotify | BLEBroadcast);




///////////////////////////////////////////////////////////////////
////////////////////////////함수 영역///////////////////////////////
///////////////////////////////////////////////////////////////////

// 음악 재생 함수
void playMusic(int musicNum){
  dfmp3.playMp3FolderTrack(musicNum);
}

void volumeSet(){
  dfmp3.setVolume(15);
  volume = dfmp3.getVolume();
  //Serial.print("volume ");
  //Serial.println(volume);
}

void volumeGet(int volumeValue){
  dfmp3.setVolume(volumeValue);
  volume = dfmp3.getVolume();
  //Serial.print("volume ");
  //Serial.println(volume);
  volume_check = PACKET_OK;
}

void timerStop(){
  //Serial.println("alarm STOP");
  //이부분을 설정시작하자마자 쓸지가 문제인데.
  ITimer1.attachInterrupt(0.01, InterruptForPause);
  hour10 = PACKET_NULL; // n
  hour1 = PACKET_NULL; // n
  min10 = PACKET_NULL; // n
  min1 = PACKET_NULL; // n
  charToNumber = CHANGE_CHAR;
  alarm_start_check = STAY_ALARM;

  timerInterruptvalue = 0;
  timerValue = 0;
  Timer1Count = 0;

}

void repeatStop(){
  timerInterruptvalue = 0;
  Timer1Count = 0;
  timerValue = 1440; // 24시간을 분으로 변환
  alarm_start_check = PACKET_START_ALARM;
  ITimer1.attachInterruptInterval(TIMER1_INTERVAL_MS * 1000, TimerHandler1);
  alarm_start_check = START_ALARM;
}

void timerEnd(){
  //Serial.println("alarm alart!!");
  ITimer1.attachInterrupt(0.01, InterruptForPause);
  sendPacket(SEND_ALARM_ALART);
  timerValue = 0;
  //이부분을 설정시작하자마자 쓸지가 문제인데.
  alarm_start_check = ALART_ALARM;
}

//패킷 아두이노 -> 안드로이드 보내는 패킷
void sendPacket(int connectMode){
  batteryDelay = BATTERY_DELAY_RESET;
  switch(connectMode){
    case SEND_NORMAL_BATTERY : //  일반적인 배터리 전송 모드
      txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_NORMAL_BATTERY_MODE); // B
      delay(50);
      txChar.writeValue(batteryLevel/100 + '0');
      delay(50);
      txChar.writeValue(batteryLevel%100/10 + '0');
      delay(50);
      txChar.writeValue(batteryLevel%10 + '0');
      delay(50);
      txChar.writeValue('7');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
    break;

    case SEND_FIRST_CONNECT : // 처음 연결모드

      //1차 전송
      txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_FIRST_CONNECT_MODE); // F
      delay(50);
      txChar.writeValue(batteryLevel/100 + '0');
      delay(50);
      txChar.writeValue(batteryLevel%100/10 + '0');
      delay(50);
      txChar.writeValue(batteryLevel%10 + '0');
      delay(50);
      txChar.writeValue('7');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);

      //2차 전송
      txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_SECOND_CONNECT_MODE); // G
      delay(50);
      txChar.writeValue(hour10 + charToNumber); // chatToNumer
      delay(50);
      txChar.writeValue(hour1 + charToNumber);
      delay(50);
      txChar.writeValue(min10 + charToNumber);
      delay(50);
      txChar.writeValue(min1 + charToNumber);
      delay(50);

      //알람 울리고있는지 확인용.
      if(alarm_start_check == STAY_ALARM){ // 알람 대기
        txChar.writeValue(PACKET_STAY_ALARM);
        delay(50);
      }
      
      else if(alarm_start_check == START_ALARM){ // 알람 시작
        txChar.writeValue(PACKET_START_ALARM);
        delay(50);
      }

      else if(alarm_start_check == ALART_ALARM){ // 알람 울림
        txChar.writeValue(PACKET_ALART_ALARM);
        delay(50);
      } 

      txChar.writeValue('9');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
    break;

    //시간 설정관련
    case SEND_TIME_OK_FAIL : // 성공 / 실패 전송 패킷
      txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_FIRST_TIME_MODE); // T
      delay(50);
      txChar.writeValue(timer1st); // 해당 패킷은 실패 성공에 따른 o / f 처리가 됨.
      delay(50);
      txChar.writeValue(timer2nd); // 해당 패킷은 실패 성공에 따른 o / f 처리가 됨.
      delay(50);
      txChar.writeValue('6');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
      if(timer1st == PACKET_OK && timer2nd == PACKET_OK){
        alarm_start_check = START_ALARM;
        ITimer1.attachInterruptInterval(TIMER1_INTERVAL_MS * 1000, TimerHandler1);
        //동작 시작 사운드 넣어주면 좋아.
        playMusic(SOUND_ALARM_SEND_OK);
      }
      timer1st = PACKET_FAIL;
      timer2nd = PACKET_FAIL;
    break;

    case SEND_SAVE_SETTING : // 안드로이드에서 설정버튼 누르면 설정된 값 전송하는 부분
      txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_SAVE_SETTING_SEND); // R
      delay(50);
      txChar.writeValue(volume/10 + '0');
      delay(50);
      txChar.writeValue(volume%10  + '0');
      delay(50);
      txChar.writeValue(repeatAlarm); // y / n
      delay(50);

      txChar.writeValue((karyl_music-10) + '0');
      delay(50);
      txChar.writeValue((sensor_cut/50)  + '0');
      delay(50);

      txChar.writeValue('9');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
      playMusic(SOUND_SETTING_RECEIVE);
    break;

    case SEND_SETTING_RESULT : // 안드로이드로 설정 응답 패킷 전송.
      txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_SETTING_RECIEVE); // S
      delay(50);
      txChar.writeValue(volume_check); // o / f
      delay(50);
      txChar.writeValue(repeat_check); // o / f
      delay(50);
      txChar.writeValue('6');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
      playMusic(SOUND_SETTING_SEND);
      volume_check = PACKET_FAIL;
      repeat_check = PACKET_FAIL;

    break;

    case SEND_PAUSE_RESULT : //안드로이드로 도중 중단 응답 패킷 전송.
      txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_STOP_ALARM); // X
      delay(50);
      txChar.writeValue(pause_check); // o / f
      delay(50);
      txChar.writeValue('5');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
      playMusic(SOUND_STOP);
      pause_check = PACKET_FAIL;
      alarm_start_check == STAY_ALARM;
    break;

    case SEND_ALARM_ALART :
    txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_ALARM_ALART); // W
      delay(50);
      txChar.writeValue('4');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
    break;

    case SEND_ALARM_ALART_END :
    //Serial.println("WELCOM TO ALART_ALART_END");
    txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_ALARM_ALART_END); // Y
      delay(50);
      txChar.writeValue(repeatAlarm); // Y
      delay(50);
      txChar.writeValue('5');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
      playMusic(SOUND_ALARM_ALART_STOP);
    break;

    case SEND_ALARM_SHOCK_END : 
    //Serial.println("SHOCK ALART");
    txChar.writeValue(PACKET_START); // A
      delay(50);
      txChar.writeValue(PACKET_ALARM_SHOCK_END); // Q
      delay(50);
      txChar.writeValue(repeatAlarm); // y/n
      delay(50);
      txChar.writeValue('5');
      delay(50);
      txChar.writeValue(PACKET_END); // Z
      delay(50);
    break;
    

    case SEND_ERROR : // 성공적으로 실패했음 모드.
    txChar.writeValue(PACKET_START);
      delay(50);
      txChar.writeValue(0x63);
      delay(50);
      txChar.writeValue(PACKET_FAIL);
      delay(50);
      txChar.writeValue('5');
      delay(50);
      txChar.writeValue(PACKET_END);
      delay(50);
    break;
  }
}

//패킷 안드로이드 -> 아두이노로 오는 패킷 검사용
void checkPacket(int*recievePacket, int datalength){

  //패킷 검사
    //시작과 끝 패킷이 제대로 왔는가?
  if(recievePacket[0]== PACKET_START && recievePacket[datalength-1] == PACKET_END){ // A / Z
    // 실제로 온 패킷의 길이와 패킷상에 기록된 길이가 일치하는가?
    if(datalength == recievePacket[datalength-2]){

        //Serial.println("packet is unknown receive");

      //스위치문 : id 체크과정 -> 밸류값 처리과정
      switch(recievePacket[1]){

        case PACKET_FIRST_CONNECT_MODE : // 초기 연결 패킷 체크 과 // F
        //Serial.println("check connect 1st packet");
          if(recievePacket[2] == PACKET_OK && recievePacket[3] == PACKET_OK){ // 둘다 o
          batteryTimeDelay = BATTERY_TIME_DELAY_NORMAL;
          //Serial.println("good!");
          firstConnect = FIRST_CONNECT_NO;
          if(alarm_start_check != ALART_ALARM){
            playMusic(SOUND_CONNECT);
          }
          }// 초기 연결 모드를 종료시키고 배터리 용량 체크 딜레이를 원위치 시킴.
        break;
        
        
        case PACKET_FIRST_TIME_MODE : //시간 설정 영역 (1차 패킷 / 적용 시간 값)
          //시간 값 저장
          //Serial.println("check time packet");
          hour10 = recievePacket[2];
          hour1 = recievePacket[3];
          min10 = recievePacket[4];
          min1 = recievePacket[5];
          timer1st = PACKET_OK; // o
          //Serial.println("this timer packet");
          //Serial.println(recievePacket[2]);
          //Serial.println(recievePacket[3]);
          //Serial.println(recievePacket[4]);
          //Serial.println(recievePacket[5]);
          charToNumber = CHANGE_NUM;
        break;

        case PACKET_SECOND_TIME_MODE : //시간 설정 영역(2차 패킷 / 타이머 카운트 값)
          // 실제 사용할 타이머 값 저장
          //Serial.println("check count packet");
          timerValue = (recievePacket[2]*1000 + recievePacket[3]*100 + recievePacket[4]*10 + recievePacket[5]) - 1;
          timer2nd = PACKET_OK; // o
          //Serial.print("timerValue : ");
          //Serial.println(timerValue);
          sendPacket(SEND_TIME_OK_FAIL); // 2차 전송때 패킷 체크를 하도록 보내는게 좋더라 이말이지.
        break;

        case PACKET_STOP_ALARM : // 알람도중 스탑 패킷
        //Serial.println("check stop packet");
        if(timerValue!=0){
          timerStop();
          sendPacket(SEND_PAUSE_RESULT);
        } 
        break;

        case PACKET_ALARM_ALART_END : // 울리고 있는 알람 중지 패킷
        //Serial.println("check ALART stop packet");
        if(repeatAlarm == PACKET_REPEAT_YES){
          //반복중인경우
          //Serial.print("repeat alarm");
          repeatStop();
        }
        else if(repeatAlarm == PACKET_REPEAT_NO){
          timerStop();
        }
        sendPacket(SEND_ALARM_ALART_END);
        break;

        case PACKET_SAVE_SETTING_SEND : // 안드로이드에서 설정버튼 누르면 설정된 값 전송하는 부분
        //Serial.println("check setting packet");
          sendPacket(SEND_SAVE_SETTING);
        break;

        case PACKET_SETTING_RECIEVE : // 안드로이드에서 설정 완료된 데이터 값을 분석하는 부분.
          volume = recievePacket[3]*10 + recievePacket[4];
          volumeGet(volume);
          //반복 처리도 해야함. 
          repeatAlarm = recievePacket[2];
          if(repeatAlarm == PACKET_REPEAT_YES){
            //반복 알람 설정 처리.
            //Serial.println("alarm repeat ok");
            repeat_check = PACKET_OK;
          }
          else{
            //일반 알람 설정 처리.
            //Serial.println("alarm repeat no");
            repeat_check = PACKET_FAIL ;
          }

          karyl_music = recievePacket[5]+10;
          //Serial.print("karyl_music : ");
          //Serial.println(karyl_music);

          sensor_cut = recievePacket[6]*50;
          //Serial.print("sensor_cut : ");
          //Serial.println(sensor_cut);


          sendPacket(SEND_SETTING_RESULT);
        break;


        default : sendPacket(SEND_ERROR); // 실패함
      }// 스위치 종료

      // 올 패킷 배열 초기화 작업 해야함


    }// 길이 동일여부 체크 조ㅓㅇ료
  } //스타트 엔드 패킷체크 종
  else{
    // 이 부분에 패킷 잘못전송되서 처리해야할 패킷 보내는 용도임.
  }
}




/*
 *  MAIN
 */
void setup() {

  // Start serial.
  Serial.begin(9600);


///////////////////////////////////////////////////////////////////
//////////////////////////블루투스 영역/////////////////////////////
///////////////////////////////////////////////////////////////////

  // 시리얼 포트 디버그용도
  //while (!Serial);


  // Start BLE.
  startBLE();

  // Create BLE service and characteristics.
  BLE.setLocalName(nameOfPeripheral);
  BLE.setAdvertisedService(microphoneService);
  microphoneService.addCharacteristic(rxChar);
  microphoneService.addCharacteristic(txChar);
  BLE.addService(microphoneService);

  // Bluetooth LE connection handlers.
  BLE.setEventHandler(BLEConnected, onBLEConnected);
  BLE.setEventHandler(BLEDisconnected, onBLEDisconnected);
  
  // Event driven reads.
  rxChar.setEventHandler(BLEWritten, onRxCharValueUpdate);
  
  // Let's tell devices about us.
  BLE.advertise();
  
  // Print out full UUID and MAC address.
  //Serial.println("Peripheral advertising info: ");
  //Serial.print("Name: ");
  //Serial.println(nameOfPeripheral);
  //Serial.print("MAC: ");
  //Serial.println(BLE.address());
  //Serial.print("Service UUID: ");
  //Serial.println(microphoneService.uuid());
  //Serial.print("rxCharacteristic UUID: ");
  //Serial.println(uuidOfRxChar);
  //Serial.print("txCharacteristics UUID: ");
  //Serial.println(uuidOfTxChar);
  

  //Serial.println("Bluetooth device active, waiting for connections...");

///////////////////////////////////////////////////////////////////
//////////////////////////dfplayer 영역////////////////////////////
///////////////////////////////////////////////////////////////////

  //Serial.println("dfplayer initializing...");
  
  dfmp3.begin();


  
  uint16_t count = dfmp3.getTotalTrackCount(DfMp3_PlaySource_Sd);
  //Serial.print("files ");
  //Serial.println(count);
  
  //Serial.println("dfplayer starting...");



   volumeSet();

  
}

// 아니 이거 딜레이로 인한 문제 해결을 위해 적어놓으라는데 느려지기만 하던데????
void waitMilliseconds(uint16_t msWait)
{
  uint32_t start = millis();
  
  while ((millis() - start) < msWait)
  {
    // if you have loops with delays, its important to 
    // call dfmp3.loop() periodically so it allows for notifications 
    // to be handled without interrupts
    dfmp3.loop(); 
    delay(1);
  }
}


void loop()
{
  BLEDevice central = BLE.central();


  if(batteryDelay>=batteryTimeDelay){


    float vout = 0.0; 

    float vin = 0.0;

    float R1 = 30000.0;

    float R2 = 7500.0;

    int battery = 0; // A0 값 읽어올때 쓰는 변수

    int batteryvolt = 0;

    battery = analogRead(A1);

    batteryvolt = (((battery * 3.2)/1024.0) / (R2/(R1+R2)) *100);

    // 배터리가 3.2V 미만으로 떨어져서 과방전되면 죽을 수 있으니 미연에 방지하도록 하는 코드
    if(batteryvolt <= 320){
      ITimer1.attachInterrupt(0.01, InterruptForPause);
      exit(0);
    }

    batteryLevel = batteryvolt * 100 / 420 ;

    if(batteryLevel > 100){
      batteryLevel = 100;
    }

    //Serial.print("batteryLevel : ");
    //Serial.print(batteryLevel);
    //Serial.println("%");
   

    // Only send data if we are connected to a central device.
    if (central.connected()) {
      if(firstConnect ==FIRST_CONNECT_YES){ //처음 연결되서 기존에 설정된 값이 있는지 전송하는 모드
      sendPacket(1);
    }
    else if(firstConnect ==FIRST_CONNECT_NO){ // 일반 배터리  전송 모드
      if(batteryLevel<=20){
        playMusic(SOUND_LOW_BATTERY);
      }
      sendPacket(0);
    }
  }

    batteryDelay = BATTERY_DELAY_RESET; // 0으로 초기화
  }
  else{
    batteryDelay++;
  }

  if(alarm_start_check == START_ALARM){ // 알람 시작
   
   printResult();
  }

  if(alarm_start_check == ALART_ALARM){ // 알람 울림
    // 여기에 충격 센서 감지
    // 여기에 음악 재생 시작
     int sensor;
    sensor = analogRead(A2);

    // 버그 터졌는지 센서가 동작중이지않을때 700이 나오고 센서 동작하면 1023까지오르는 중임.
    if (sensor<sensor_cut){
    // 캬루 아파하는 보이스 추가
      playMusic(SOUND_HIT);
      //Serial.print("sensor : ");
      //Serial.println(sensor);
      //Serial.print("sensor_cut : ");
      //Serial.println(sensor_cut);
      if(repeatAlarm == PACKET_REPEAT_YES){
        //반복중인경우
        //Serial.print("repeat alarm");
        sendPacket(SEND_ALARM_SHOCK_END);
        repeatStop();
      }
      else if(repeatAlarm == PACKET_REPEAT_NO){
        //Serial.print("end alarm");
        sendPacket(SEND_ALARM_SHOCK_END);
        timerStop();
      }
    }
  }



  //타이머 인터럽트 테스트 영역
  //Timer1Count = 0;
  // 이 타이머 인터럽트는 내부에서 계속 돌고있으니 딜레이에 따른 피해는 없어야 정상이다.

  
  //for(int i=0; i<10; i++){
    ////Serial.print(TIMER1_INTERVAL_MS * 1000);
    
  //  delay(1000);
  //}
  //lockOn = 1;
  //ITimer1.attachInterrupt(0.01, InterruptForPause);
  //lockOn = 0;

    
  delay(10);
}


/*
 *  BLUETOOTH
 */
void startBLE() {
  if (!BLE.begin())
  {
    //Serial.println("starting BLE failed!");
    while (1);
  }
}

void onRxCharValueUpdate(BLEDevice central, BLECharacteristic characteristic) {
  // central wrote new value to characteristic, update LED
  //Serial.print("alpha: ");
  byte test[256];
  
  int dataLength = rxChar.readValue(test, 256);
  int recievePacket[dataLength];

  //디버그용
    for(int i = 0; i < dataLength; i++) {
      //Serial.println((char)test[i]);
  }

  //Serial.print("ascii: ");

  for(int i = 0; i < dataLength; i++) {
    ////Serial.print((char)test[i]);

    //바이트 값 int 로 변환
    if(test[i]<58){
      recievePacket[i] = test[i] -'0';
    }
    else{
      recievePacket[i] = test[i];
    }
    //Serial.println(recievePacket[i]);


    // 받아오는걸보아하니  for 문으로 하나씩 받는다.
    //애초에 byte 값으로 받아오네.. 이걸몰랐네
  }

  //Serial.println();
  //Serial.print("Value length = ");
  //Serial.println(rxChar.valueLength());
  //Serial.println();


checkPacket(recievePacket, dataLength);
}

void onBLEConnected(BLEDevice central) {
  //Serial.print("Connected event, central: ");
  //Serial.println(central.address());
  //블루투스 연결시 동작
  //연결시 타임 딜레이를 확 낮춰버릴예정
  batteryTimeDelay = BATTERY_TIME_DELAY_CONNECT;
  firstConnect = FIRST_CONNECT_YES;
  
}

void onBLEDisconnected(BLEDevice central) {
  //Serial.print("Disconnected event, central: ");
  //Serial.println(central.address());
  playMusic(SOUND_DISCONNECT);
  //블루투스 해제시 동
  
}
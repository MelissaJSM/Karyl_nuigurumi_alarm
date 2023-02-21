package com.example.karyl_ble;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import static com.example.karyl_ble.MainActivity.loading_screen;

public class ProgressDialog extends Dialog
{

    TypedArray typedArray_karyl; // 숫자 이미지 배열

    android.os.Handler loading_screen_Handler = new Handler();



    //변수 라인
    int karyl_count = 0; // 이미지 애니메이션 카운트
    int karyl_count_tx = 0; // 텍스트 애니메이션 카운트



    public ProgressDialog(Context context)
    {
        super(context);
        // 다이얼 로그 제목을 안보이게...
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.progress_loading);


        //로딩 이미지 배열 찾는 for문
        for(int loading_select = 0 ; loading_select<10 ; loading_select++){
            if(loading_screen==loading_select){
                ImageView iv_frame_loading = (ImageView)findViewById(R.id.iv_frame_loading);
                TextView texv_progress_message = (TextView)findViewById(R.id.tv_progress_message);
                typedArray_karyl = context.getResources().obtainTypedArray(R.array.png_karyl_00+loading_select); //chara_select가 배열 번호가된다.
                start_handler(loading_select, iv_frame_loading, texv_progress_message, context);

            }
        }






    }

    //30프레임의 애니메이션 구현용 핸들러
    private void start_handler(int loading_select, ImageView iv_frame_loading, TextView texv_progress_message, Context context){
        loading_screen_Handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //System.out.println("현재 로딩화면 동작중");
                if(karyl_count>29){ // 30프레임 체크용
                    karyl_count=0; // 리셋
                    karyl_count_tx = 0;
                }
                if(karyl_count%10 == 9 && karyl_count != 29){
                    karyl_count_tx++;
                }
                iv_frame_loading.setImageResource(typedArray_karyl.getResourceId(karyl_count, -1));
                texv_progress_message.setText(context.getResources().getString((R.string.loading_text_00 + loading_select*3) + karyl_count_tx));
                karyl_count++;

                loading_screen_Handler.postDelayed(this, 50);
            }
        }, 0);
    }

    //나가지는곳 구현
    @Override
    protected void onStop() {
        super.onStop();
        //System.out.println("데이터 상황 종료");
        loading_screen = Constants.loading_screen_non;
        loading_screen_Handler.removeMessages(0);
        loading_screen_Handler.removeCallbacksAndMessages(null);
        karyl_count_tx = 0;
        karyl_count = 0;
    }



}
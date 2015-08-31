package bmw.awa.awabmw;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.activeandroid.query.Select;
import com.loopj.android.image.SmartImageView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hirokinaganuma on 15/08/27.
 *
 * //必要であればmBoundServiceを使ってバインドしたServiceへの制御を行う
 *
 * MusicServiceClassにライフサイクルをまとめてあります。
 */
public class PlayerActivity extends Activity{//曲を選択した時のアクティビティ

    private MusicService mBindService;
    ImageButton playButton;
    SmartImageView jacketImage;
    TextView titleText, artistText;
    CircularSeekBar seekBar;
    Timer timer;
    Handler handler;
    TextView LeftSideText,RightSideText;//現在の再生位置を表示
    String maxLength,nowLength;//曲の現在時間を出すところ(全体時間と現在時間)
    AlphaAnimation feedin_btn;
    AlphaAnimation feedout_btn;

    //このReceiverはActivity起動時にしか使えない
    public BroadcastReceiver myReceiver = new BroadcastReceiver() {//Serviceからの受信機
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("onReceive", "@FromServiceBroadcastReceiver Called");

            if(intent.getAction()=="onCreate"){
                if(timer!=null){//これがないと再生せずに戻った時エラーでる
                    timer.cancel();
                    timer = null;
                }
                subOnCreate(true);//ランダムに次の曲へ
            }else if(intent.getAction()=="afterCreate"){
                afterBinding();
                centerButtonClicked();
            }else{//Activityが存在し曲が終了したときにここが呼ばれる
                nextTrack();
                Log.d("DEBUG TEST", "----------intent BY BROADCASTRECEIVER----------");
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {// Serviceとのインターフェースクラス
        //3 Service接続後、Binderが確立したタイミングで呼び出される。
        public void onServiceConnected(ComponentName className, IBinder service) {// Serviceとの接続確立時に呼び出される。
            mBindService = ((MusicService.LocalBinder)service).getService();//必要であればmBindServiceを使ってバインドしたServiceへの制御を行う
            afterBinding();
            centerButtonClicked();//この段階で再生開始
        }
        public void onServiceDisconnected(ComponentName className) {
            mBindService = null; // Serviceとの切断時に呼び出される。
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);


        //notificationからの呼び出し
        if(getIntent().getAction()=="ACTION_STOP_PLAY") {//playpauseが押されたとき
            Log.d("DEBUG TEST","----------onCreate Intent PLAY PAUSE----------");
            handler = new Handler();//Handlerを初期化
            Intent intent = new Intent(PlayerActivity.this, MusicService.class);//Serviceをバインドする
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);//エラー出たけど無視できそう(ServiceConnectionLeaked)
            SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);//現在再生されている曲を取得
            long keyId = data.getLong("key", 1);
            Item item = new Select().from(Item.class).where("Id = ?", keyId).executeSingle();
            Log.d("DEBUG TEST","Item Id at Activity#onCreate: ["+ keyId +"] from Notification");
            commonOnCreate(item);
            centerButtonClicked();
        }else if(getIntent().getAction()=="next"){
            Log.d("DEBUG TEST","----------onCreate Intent NEXT TRACK----------");
            nextTrack();
            centerButtonClicked();
        }else{//普通のActivity呼び出し
            Log.d("DEBUG TEST","----------onCreate Intent DAFAULT----------");
            subOnCreate(false);
        }
    }

    public void subOnCreate(boolean random){
        handler = new Handler();//Handlerを初期化

        Item item;
        if(random){///ItemをランダムにDBから取得する
            item = new Select().from(Item.class).orderBy("RANDOM()").executeSingle();
        }else{//onCreateか実行されるとき,DBのトップから
            Intent intent = new Intent(PlayerActivity.this,MusicService.class);//Serviceをバインドする
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);//エラー出たけど無視できそう(ServiceConnectionLeaked)
            item = new Select().from(Item.class).orderBy("id DESC").executeSingle();//
            IntentFilter filter=new IntentFilter("awa");
            registerReceiver(myReceiver, filter);//BroadCastReceiverセットする
        }
        Log.d("DEBUG TEST","Item Id at Activity#subOnCreate: ["+ item.getId() +"]");
        SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        editor.putLong("key",item.getId());
        Log.d("\"key\",item.getId()", "" + item.getId());
        editor.apply();

        commonOnCreate(item);
    }

    public void commonOnCreate(Item item){

        String trackName = item.track_name;
        String imageURI = item.artworkUrl100;
        String artistName =item.artistName;

        //DataBaseから取得した情報をもとにViewにセットしていく
        playButton = (ImageButton) findViewById(R.id.imageButton);//再生ボタンを関連付け
        jacketImage = (SmartImageView) findViewById(R.id.imageView);//Jacket画像を
        jacketImage.setImageUrl(imageURI);
        jacketImage.setScaleType(ImageView.ScaleType.FIT_CENTER);//画像を正方形で表示
        titleText = (TextView) findViewById(R.id.textView2);//
        titleText.setText(trackName);//トラック名をセット
        artistText = (TextView) findViewById(R.id.textView);
        artistText.setText(artistName);//アーティスト名をセット
        LeftSideText = (TextView)findViewById(R.id.textlefttime);
        RightSideText = (TextView)findViewById(R.id.textrighttime);

        feedin_btn = new AlphaAnimation( 0, 1 );//(0,1)フェードイン
        feedin_btn.setDuration(500);//表示時間を指定
        feedin_btn.setFillAfter(true);//もとに戻らない

        feedout_btn = new AlphaAnimation( 1, 0 );//フェードインアニメーションの準備する(1,0)フェードアウト
        feedout_btn.setDuration(500);//表示時間を指定
        feedout_btn.setFillAfter(true);//もとに戻らない

        mBindService.isActivityExist = true;
        Log.d("DEBUG TEST","commonOnCreate isActivity:"+mBindService.isActivityExist);
    }


    public void start(View v) {//再生&停止ボタンが押された時の処理
        centerButtonClicked();//再生ボタンが押された処理をサブルーチン化
    }

    public void centerButtonClicked(){
        if(mBindService!=null) {
            try {
                mBindService.startOrStop();//再生もしくは停止
                if(mBindService.getMediaPlayer().isPlaying()){
                    playButton.setBackground(getResources().getDrawable(R.drawable.play));
                    playButton.startAnimation(feedout_btn);
                }else{
                    playButton.setBackground(getResources().getDrawable(R.drawable.pause));
                    playButton.startAnimation(feedout_btn);
                }
                if (timer == null) { // timerの多重起動を防ぐ
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {// 現在の再生位置を取得する
                            try{
                                nowLength = mBindService.calcDuration(false);//現在の時刻を取得
                                handler.post(new Runnable() {// UIを操作するため、Handlerが必要
                                    @Override
                                    public void run() {
                                        RightSideText.setText(maxLength); // 現在の再生位置をセット
                                        LeftSideText.setText(nowLength);
                                        seekBar.invalidate();//これを加えることでプログレスバー線の色が変わってきた
                                        try{
                                            seekBar.setProgress(mBindService.getMediaPlayer().getCurrentPosition()); // SeekBarにも現在位置をセット
                                        }catch (RemoteException e) {
                                            e.printStackTrace();
                                        }catch (ArithmeticException e){
                                            e.printStackTrace();
                                        }catch (IllegalStateException e){
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }catch (RemoteException e) {
                                e.printStackTrace();
                            }catch (IllegalStateException e){
                                e.printStackTrace();
                            }
                        }
                    }, 0, 100); // 1000ミリ秒間隔で実行 timerTaskを実行
                }
            }catch (RemoteException e) {
                e.printStackTrace();
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
    }

    /*リピートボタンを実装する際はこのコメントアウトを外す
    public void repeat(View v) {
        if (mBindService != null) {
            try{
                mBindService.setLoopStateService();
            }catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    */

    public void seekBarPrepare() {//seekBarの再生準備 afterbinding
        seekBar = (CircularSeekBar) findViewById(R.id.seek_bar);
        seekBar.setProgress(0);
        try{
            seekBar.setMaxProgress(mBindService.getMediaPlayer().getDuration());
            maxLength = mBindService.calcDuration(true);//maxlengthに全体時間を表示
        }catch (RemoteException e) {
            e.printStackTrace();
        }
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.onTouchEvent(event);//superと同じような役割(これで、シークバーのトラックの処理が可能)
                if(mBindService!=null) {
                    try{
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            RightSideText.setText(maxLength); // 現在の再生位置をセット
                            LeftSideText.setText(nowLength);
                            int progress = seekBar.getProgress();
                            mBindService.getMediaPlayer().seekTo(progress);
                            mBindService.getMediaPlayer().start();
                            playButton.setBackground(getResources().getDrawable(R.drawable.play));
                            playButton.setAnimation(feedin_btn);
                            playButton.startAnimation(feedout_btn);
                        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            RightSideText.setText(maxLength); // 現在の再生位置をセット
                            LeftSideText.setText("...");
                            mBindService.getMediaPlayer().pause();
                            playButton.setBackground(getResources().getDrawable(R.drawable.pause));
                            playButton.setAnimation(feedin_btn);
                        }
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
    }

    public void afterBinding(){//
        /**Serviceと連携する場合呼び出しの順序が
         * 0.Activity#onCreate
         * 1.Service#onCreate が初回作成時に呼ばれる
         * 2.Service#onBind Service接続時に呼ばれる。ServiceとActivityを仲介するIBinderを返却する。
         * 3.Activity#onServiceConnected Service接続後、Binderが確立したタイミングで呼び出される。
         * この3のタイミングでafterBinding読んでいる
         * が終わって初めてService側の値とかをmBindService経由で取得できる
         */
        seekBarPrepare();//playerのDurationアクセスとかするのでafterbindingで
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(timer!=null){//これがないと再生せずに戻った時エラーでる
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBindService!=null){
            try{
                if(!mBindService.getMediaPlayer().isPlaying()){
                    mBindService.getMediaPlayer().stop();
                    mBindService.getMediaPlayer().release();
                }
            }catch (RemoteException e){
                e.printStackTrace();
            }
        }
        mBindService.isActivityExist = false;
        unregisterReceiver(myReceiver);
        Log.d("DEBUG TEST","onDestroy isActivity:"+mBindService.isActivityExist);
    }

    public void nextTrackClicked(View v){//次へボタンが押された時の処理は全てここに
        nextTrack();
    }

    public void nextTrack(){
        if(mBindService!=null) {
            try{
                if(timer!=null){//これがないと再生せずに戻った時エラーでる
                    timer.cancel();
                    timer = null;
                }
                subOnCreate(true);//ランダムに次の曲へ
                mBindService.nextTrackService();//Service#subOnCreateService(true)
                afterBinding();
                centerButtonClicked();
            }catch (RemoteException e){
                e.printStackTrace();
            }
        }
    }

}



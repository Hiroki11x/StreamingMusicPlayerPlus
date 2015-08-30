package bmw.awa.awabmw;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
 *
 *
 * //必要であればmBoundServiceを使ってバインドしたServiceへの制御を行う
 *
 *
 *
 */
public class PlayerActivity extends Activity{
    private MusicService mBindService;

    // Serviceとのインターフェースクラス
    private ServiceConnection mConnection = new ServiceConnection() {

        //3 Service接続後、Binderが確立したタイミングで呼び出される。
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Serviceとの接続確立時に呼び出される。
            // service引数には、Onbind()で返却したBinderが渡される
            mBindService = ((MusicService.LocalBinder)service).getService();
            afterBinding();
            //必要であればmBoundServiceを使ってバインドしたServiceへの制御を行う
        }

        public void onServiceDisconnected(ComponentName className) {
            // Serviceとの切断時に呼び出される。
            mBindService = null;
        }
    };


    //曲を選択した時のアクティビティ

    ImageButton playButton;
    SmartImageView jacketImage;
    TextView titleText, artistText;
    CircularSeekBar seekBar;


    Timer timer;
    Handler handler;
    TextView currentTimeText;//現在の再生位置を表示
    String maxLength,nowLength;//曲の現在時間を出すところ(全体時間と現在時間)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);


        handler = new Handler();

        //intent元から各種情報を取得
        /*
        String trackName = getIntent().getExtras().getString("track_name");
        String previewUrl = getIntent().getExtras().getString("preview_url");
        String imageURI = getIntent().getExtras().getString("artworkUrl100");
        String artistName = getIntent().getExtras().getString("artistName");
        String albumName = getIntent().getExtras().getString("collectionName");
        */

        //Serviceをバインドする
        Intent intent = new Intent(PlayerActivity.this,MusicService.class);
        /*
        intent.putExtra("track_name", trackName);//そのJSONObjectの名前をPlayerアクティビティに受け渡す
        intent.putExtra("preview_url", previewUrl);//そのJSONObjectURI(音楽を聴くためにもの)をPlayerアクティビティに受け渡す
        intent.putExtra("artworkUrl100", imageURI);//その画像のURL(音楽を聴くためにもの)をPlayerアクティビティに受け渡す
        intent.putExtra("artistName", artistName);//その画像アーティスト名をPlayerアクティビティに受け渡す
        intent.putExtra("collectionName", albumName);//その画像アーティスト名をPlayerアクティビティに受け渡す
        intent.setAction("play");
        */
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);//エラー出た


        Item item = new Select().from(Item.class).orderBy("id DESC").executeSingle();

        String trackName = item.track_name;
        String imageURI = item.artworkUrl100;
        String artistName =item.artistName;

        //intentから取得した情報をもとにViewにセットしていく
        playButton = (ImageButton) findViewById(R.id.imageButton);//再生ボタンを関連付け
        jacketImage = (SmartImageView) findViewById(R.id.imageView);//Jacket画像を
        jacketImage.setImageUrl(imageURI);
        jacketImage.setScaleType(ImageView.ScaleType.FIT_CENTER);//画像を正方形で表示
        titleText = (TextView) findViewById(R.id.textView2);
        titleText.setText(trackName);//トラック名をセット
        artistText = (TextView) findViewById(R.id.textView);
        artistText.setText(artistName);//アーティスト名をセット
        currentTimeText = (TextView) findViewById(R.id.textView3);
        //centerButtonClicked();//音楽の再生が行われるのでseekbarとかにも対応させる
    }

    public void start(View v) {//再生&停止ボタンが押された時の処理
        centerButtonClicked();
    }

    public void centerButtonClicked(){
        if(mBindService!=null){
            try{
                mBindService.startOrStop();
                if(mBindService.getMediaPlayer().isPlaying()){
                    playButton.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
                }else{
                    playButton.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));

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
                                        currentTimeText.setText(nowLength + " / " + maxLength); // 現在の再生位置をセット
                                        seekBar.invalidate();//これを加えることでプログレスバー線の色が変わってきた
                                        try{
                                            seekBar.setProgress(mBindService.getMediaPlayer().getCurrentPosition()); // SeekBarにも現在位置をセット
                                        }catch (RemoteException e) {
                                            e.printStackTrace();
                                        }catch (ArithmeticException e){
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 0, 1000); // 1000ミリ秒間隔で実行 timerTaskを実行
                }
            }catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void repeat(View v) {
        if (mBindService != null) {
            try{
                mBindService.setLoopStateService();
            }catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void seekBarPrepare() {//seekBarの再生準備 afterbinding
        seekBar = (CircularSeekBar) findViewById(R.id.seek_bar);
        seekBar.setProgress(0);
//        seekBar.setMaxProgress(mp.getDuration());
        try{
            seekBar.setMaxProgress(mBindService.getMediaPlayer().getDuration());
        }catch (RemoteException e) {
            e.printStackTrace();
        }

        seekBar.setSeekBarChangeListener(new CircularSeekBar.OnSeekChangeListener() {
            @Override
            public void onProgressChange(CircularSeekBar view, int newProgress) {
                Log.d("Welcome", "Progress:" + view.getProgress() + "/" + view.getMaxProgress());
            }
        });
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.onTouchEvent(event);//superと同じような役割(これで、シークバーのトラックの処理が可能)
                if(mBindService!=null) {
                    try{
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            currentTimeText.setText(nowLength + " / " + maxLength); // 現在の再生位置をセット
                            int progress = seekBar.getProgress();
                            mBindService.getMediaPlayer().seekTo(progress);
                            mBindService.getMediaPlayer().start();
                            Log.d("Call ACTION_UP", "");
                        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            currentTimeText.setText("loading...");
                            mBindService.getMediaPlayer().pause();
                        }
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });

    }

    public void afterBinding(){
        seekBarPrepare();
        try{
            maxLength = mBindService.calcDuration(true);//maxlengthに全体時間を表示
        }catch (RemoteException e){
            e.printStackTrace();
        }

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
                if(mBindService.getMediaPlayer().isPlaying()==false){
                    mBindService.getMediaPlayer().stop();
                    mBindService.getMediaPlayer().release();
                }
            }catch (RemoteException e){
                e.printStackTrace();
            }
        }
    }
}

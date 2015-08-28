package bmw.awa.awabmw;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.image.SmartImageView;

import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hirokinaganuma on 15/08/27.
 */
public class PlayerActivity extends Activity {
    //曲を選択した時のアクティビティ

    String previewUrl;
    MediaPlayer mp;
    ImageButton playButton;
    SmartImageView jacketImage;
    TextView titleText, artistText;
    CircularSeekBar seekBar;


    Timer timer;
    Handler handler;
    TextView currentTimeText;//現在の再生位置を表示
    int globalDuration; //曲の長さ
    String maxLength,nowLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);
        handler = new Handler();


        //intent元から各種情報を取得
        String trackName = getIntent().getExtras().getString("track_name");
        String previewUrl = getIntent().getExtras().getString("preview_url");
        String imageURI = getIntent().getExtras().getString("artworkUrl100");
        String artistName = getIntent().getExtras().getString("artistName");
        String albumName = getIntent().getExtras().getString("collectionName");

        playButton = (ImageButton) findViewById(R.id.imageButton);//再生ボタンを関連付け
        jacketImage = (SmartImageView) findViewById(R.id.imageView);
        jacketImage.setImageUrl(imageURI);
        jacketImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        titleText = (TextView) findViewById(R.id.textView2);
        titleText.setText(trackName);//トラック名をセット

        artistText = (TextView) findViewById(R.id.textView);
        artistText.setText(artistName);//アーティスト名をセット

        if (!TextUtils.isEmpty(previewUrl)) {//URIが空でなければ
            mediaplayerPrepare(previewUrl);//下にメソッドを定義している
        }
        seekBarPrepare();
        maxLength = calcDuration(true);
        currentTimeText = (TextView) findViewById(R.id.textView3);
    }

    public void start(View v) {//再生&停止ボタンが押された時の処理
        if (mp.isPlaying()) {//再生->停止の時
            mp.pause();
            playButton.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));

            if (timer != null) {
                timer.cancel(); // timerを止める
                timer = null; // もう一回再生おした時用
            }

        } else {//停止->再生の時
            mp.start();
            playButton.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
            //背景を書き換えないとボタンまで透過できない

            if (timer == null) { // timerの多重起動を防ぐ
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {// 現在の再生位置を取得する
                        nowLength = calcDuration(false);
                        // UIを操作するため、Handlerが必要
                        handler.post(new Runnable() {
                            int preProgress=0;
                            @Override
                            public void run() {
                                currentTimeText.setText(nowLength + " / " + maxLength); // 現在の再生位置をセット
                                seekBar.invalidate();//これを加えることでプログレスバー線の色が変わってきた
                                seekBar.setProgress(mp.getCurrentPosition()); // SeekBarにも現在位置をセット
                                preProgress=seekBar.getProgress();
                            }
                        });
                    }
                }, 0, 1000); // 1000ミリ秒間隔で実行 timerTaskを実行
            }
        }
    }

    public void mediaplayerPrepare(String args) {//mediaPlayerの再生準備
        mp = new MediaPlayer();
        try {
            mp.setDataSource(this, Uri.parse(args));
            // setAudioStreamTypeはprepare前に実行する必要がある
            mp.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mp.setLooping(false);
            // prepareの前後で使えるメソッドが異なる
            mp.prepare();
            mp.seekTo(0);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void seekBarPrepare() {//seekBarの再生準備
        seekBar = (CircularSeekBar) findViewById(R.id.seek_bar);
        seekBar.setProgress(0);
        seekBar.setMaxProgress(mp.getDuration());
        seekBar.setSeekBarChangeListener(new CircularSeekBar.OnSeekChangeListener() {
            @Override
            public void onProgressChange(CircularSeekBar view, int newProgress) {
                Log.d("Welcome", "Progress:" + view.getProgress() + "/" + view.getMaxProgress());
//                mp.seekTo(seekBar.getProgress());
            }
        });



        seekBar.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.onTouchEvent(event);//superと同じような役割
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // SeekBarにも現在位置をセット
                    int progress = seekBar.getProgress();
                    mp.seekTo(progress);
                    mp.start();
                    Log.d("Call ACTION_UP", "");
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mp.pause();
                }
                return true;
            }
        });

    }

    public String calcDuration(boolean isFirstCall) {
        int duration;
        if(isFirstCall){
            duration = mp.getDuration() / 1000;
        }else{
            duration = mp.getCurrentPosition() / 1000;
        }
        int minutes = duration / 60;
        int seconds = duration % 60;
        String m = String.format(Locale.JAPAN, "%02d", minutes);
        String s = String.format(Locale.JAPAN, "%02d", seconds);
        String temp = m + ":" + s; // maxLengthにセット
        return temp;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mp.release();//MediaPlayerのリソース開放
    }
}

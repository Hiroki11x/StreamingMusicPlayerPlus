package bmw.awa.awabmw;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.image.SmartImageView;

import java.io.IOException;

/**
 * Created by hirokinaganuma on 15/08/27.
 */
public class PlayerActivity extends Activity{
    //曲を選択した時のアクティビティ

    String previewUrl;
    MediaPlayer mp;
    ImageButton playButton;
    SmartImageView jacketImage;
    TextView titleText;
    CircularSeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);

        // ActionBarのタイトルを設定する
        String trackName = getIntent().getExtras().getString("track_name");
        getActionBar().setTitle(trackName);//アクションバーに、曲のタイトルを表示

        previewUrl = getIntent().getExtras().getString("preview_url");
        //intent元から、URIを取得

        if (!TextUtils.isEmpty(previewUrl)) {//URIが空でなければ
            mediaplayerPrepare();//下にメソッドを定義している
        }

        playButton = (ImageButton)findViewById(R.id.imageButton);//再生ボタン

        String imageURI = getIntent().getExtras().getString("artworkUrl100");
        jacketImage = (SmartImageView)findViewById(R.id.imageView);
        jacketImage.setImageUrl(imageURI);
        jacketImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        titleText = (TextView)findViewById(R.id.textView);
        titleText.setText(trackName);

        seekBar = (CircularSeekBar)findViewById(R.id.seek_bar);
        seekBar.setMaxProgress(100);
        seekBar.setProgress(0);
        seekBar.invalidate();
        seekBar.setSeekBarChangeListener(new CircularSeekBar.OnSeekChangeListener() {
            @Override
            public void onProgressChange(CircularSeekBar view, int newProgress) {}
        });
    }

    public void start(View v){//再生&停止ボタンが押された時の処理
        if(mp.isPlaying()){
            //再生->停止の時
            mp.pause();
            playButton.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));
        }else{
            //停止->再生の時
            mp.start();
            playButton.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
            //背景を書き換えないとボタンまで透過できない
        }
    }

    public void mediaplayerPrepare(){//mediaPlayeの再生準備
        mp = new MediaPlayer();
        try {
            mp.setDataSource(this, Uri.parse(previewUrl));
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

    @Override
    protected void onPause() {
        super.onPause();
        mp.release();//MediaPlayerのリソース開放
    }
}

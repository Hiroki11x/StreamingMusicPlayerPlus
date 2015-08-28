package bmw.awa.awabmw;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import java.io.IOException;

/**
 * Created by hirokinaganuma on 15/08/27.
 */
public class PlayerActivity extends Activity{
    //曲を選択した時のアクティビティ

    String previewUrl;
    MediaPlayer mp;

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
            /*
            VideoView videoView = (VideoView) findViewById(R.id.video_view);
            videoView.setMediaController(new MediaController(this)); // 再生ボタンとかをつける
            videoView.setVideoURI(Uri.parse(previewUrl)); // URLを設定する
            videoView.start(); // 再生する
            */
            mediaplayerPrepare();
        }
    }

    public void start(View v){//再生&停止ボタンが押された時の処理
        if(mp.isPlaying()){
            mp.pause();
        }else{
            mp.start();
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

}

package bmw.awa.awabmw;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.activeandroid.query.Select;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by hirokinaganuma on 15/08/29.
 *
 * ServiceConnectionによって取得するIBinderを介することで、Serviceへの制御を行うことが可能です。
 *
 * Activiyスタート時の挙動
 *
 * 1. Activity#onCreateでsubOnCreate(false)が呼ばれる
 * 2. Activity#subOnCreate()でbind(),DBからデータ取得,UIの関連付けを行う
 *
 * 3. Service#onCreateでsubOnCreateService(false)が呼ばれる
 * 4. Service#subOnCreateServiceでMediaPlayerの初期化、getIntentContents(),mediaplayerPrepare(),を行っている
 * 5. Service#getIntentContents()でDBからのデータ取得
 * 6. Service#mediaplayerPrepare()でMediaPlayerの準備,データのセットとか
 * 7. Service#onBind()特に何もしてない
 *
 * 8. Activity#onServiceConnected() ActivityからService制御するための変数ゲット、afterBinding()、centerButtonClicked()を実行
 * 9. Activity#afterBinding() seekBarPrepare(),
 *10. Activity#seekBarPrepare() Service#calcDuration実行 SeekBarの初期設定、リスナ設定、Service#mpからdurationを取得
 *11. Activity#centerButtonClicked() Service#startOrStop() ボタンの画像切り替え timer開始 progressBarが動き始める、時間も更新
 *
 *12. Service#startOrStop() MediaPlayer再生停止を行う generateNotification()
 *13. Service#generateNotification() で通知を出す
 *
 * Activity#nextTrackスタート時の挙動
 * すでにbindしているのでbinっする必要なし
 *
 * 1. Activity#subOnCreate(true)
 * 2. Service#subOnCreateService(true)//どちらもランダムで呼ぶと楽曲と再生画面が一致しなくなのでidをSharedPreferenceで管理
 *
 * 3. Activity#afterBinding()、
 * 4. Activity#centerButtonClicked()
 */

public class MusicService extends Service {

    static boolean isActivityExist=false;
    private static MediaPlayer mp;
    public static volatile boolean isNotif=false;
    private volatile String trackName_S = "";
    private volatile String previewUrl_S = "";
    private volatile String imageURI_S = "";
    private volatile String artistName_S = "";
    private volatile String albumName_S = "";
    private static long nowPlayingId;
//    private ProgressDialog dialog;

    // Serviceに接続するためのBinderクラスを実装する
    public class LocalBinder extends Binder {
        //Serviceの取得
        MusicService getService() {
            return MusicService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();// Binderの生成

    @Override//1  Serviceが初回作成時に呼ばれる。
    public void onCreate() {
        subOnCreateService(false);//falseとする時ランダムでなく最新のものを取得
    }

    public void subOnCreateService(boolean random){
        if (mp != null) {
            try{
                if (mp.isPlaying()) {mp.stop();}
                mp.release();
                mp = null;
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
        try {
            getIntentContents(random,isNotif);//falseとする時ランダムでなく最新のものを取得
            if (!TextUtils.isEmpty(previewUrl_S)) {
                mediaplayerPrepare(previewUrl_S);//エラー出た
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override//2  Service接続時に呼ばれる。ServiceとActivityを仲介するIBinderを返却する。
    public IBinder onBind(Intent intent) {// 戻り値として、Serviceクラスとのbinderを返す。
        return mBinder;
    }

    @Override//4 クライアントがServiceと切断されたタイミングで呼ばれる。
    public boolean onUnbind(Intent intent) {//onUnbindをreturn trueでoverrideすると次回バインド時にonRebildが呼ばれる
        // Service切断時に呼び出される
        stopForeground(true);//通知の削除
        return true;
    }

    @Override//5 Serviceが使用されなくなったタイミングで呼ばれる Serviceの持っているリソースをクリーンアップする。
    public void onDestroy() {
    }

    @Override
    public void onRebind(Intent intent) {// Unbind後に再接続する場合に呼ばれる
        Log.i("", "onRebind" + ": " + intent);
    }

    public void mediaplayerPrepare(String args) throws RemoteException {//mediaPlayerの再生準備
        mp = new MediaPlayer();
        try {
            mp.setDataSource(this, Uri.parse(args));//エラー出た
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);// setAudioStreamTypeはprepare前に実行する必要がある
            mp.setLooping(false);
            // prepareの前後で使えるメソッドが異なる
            mp.prepare();
            mp.seekTo(0);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                @Override
                public void onCompletion(MediaPlayer mp) {//楽曲再生終了時
                    // イベント受領時の処理を記述する
                    Log.d("DEBUG TEST", "onCompletion @Service");//ここの検知はOK
                    try{
                        SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
                        Item maxItem = new Select().from(Item.class).orderBy("id DESC").executeSingle();
                        if(maxItem.getId()<data.getLong("key",0)+1l){
                            mp.seekTo(0);
                            return;
                        }//
                        SharedPreferences.Editor editor = data.edit();
                        editor.putLong("key", data.getLong("key",0)+1l);//再生する曲をインクリメントで次へ
                        editor.apply();//Activity存在していないときはitemをSharedPreference保存

                        if(isActivityExist){//Activityが存在しているとき
                            Intent broadCastIntent = new Intent("awa");// intentを作成する。（SimpleService.ACTIONのブロードキャストとして配信させる）
                            sendBroadcast(broadCastIntent);
                            Log.d("DEBUG TEST", "onCompletion->BroadCast");
                        }else{//Activityが存在していないとき
                            subOnCreateService(true);
                            startOrStop();
                            Log.d("DEBUG TEST", "onCompletion->NextTrack");
                        }
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            });
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String calcDuration(boolean isFirstCall) throws RemoteException,IllegalStateException {
        int duration;
        if (isFirstCall) {
            duration = mp.getDuration() / 1000;
        } else {
            duration = mp.getCurrentPosition() / 1000;
        }
        int minutes = duration / 60;
        int seconds = duration % 60;
        String m = String.format(Locale.JAPAN, "%02d", minutes);
        String s = String.format(Locale.JAPAN, "%02d", seconds);
        String temp = m + ":" + s; // maxLengthにセット
        return temp;
    }

    //再生または停止を行う
    public void startOrStop() throws RemoteException,IllegalStateException {//Activity#centerButtonClickedから呼ばれる
        //再生ボタンの設定
        if (mp.isPlaying()) {//再生->停止の時
            mp.pause();
            stopForeground(true);
        } else {//停止->再生の時
            mp.start();
            generateNotification();//Notification開始
        }
    }

    public void nextTrackService() throws RemoteException{
        subOnCreateService(true);//trueとする時ランダムに取得SharedPreferenceから取得
    }

    public void getIntentContents(boolean random,boolean notif) throws RemoteException {
        Item item = null;//DataBaseからそれぞれの値を取得してくる
        SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
        nowPlayingId = data.getLong("key",0);
        item = new Select().from(Item.class).where("Id = ?", nowPlayingId).executeSingle();
        Log.d("DEBUG TEST", "Create and Save Item Id at Service#getIntentContents: [" + item.getId() + "]");
        trackName_S = item.track_name;
        previewUrl_S = item.previewUrl;
        imageURI_S = item.artworkUrl100;
        artistName_S = item.artistName;
        albumName_S = item.collectionName;
    }

    //targetAPtとかSuppressWarning入れないと落ちる。
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1| Build.VERSION_CODES.JELLY_BEAN_MR2|Build.VERSION_CODES.KITKAT|Build.VERSION_CODES.JELLY_BEAN|Build.VERSION_CODES.LOLLIPOP|Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressWarnings("deprecation")
    private void generateNotification() {
        //通知領域タップ時のPendingIntentを生成
        Intent actionIntent = new Intent(getApplicationContext(), PlayerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //独自レイアウトのremoveViewを実装
        RemoteViews mNotificationView = new RemoteViews(getPackageName(), R.layout.notification_statusbar);

        //Notificationの生成
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.mplus_logo);//ここに上側に表示する画像を
        builder.setContent(mNotificationView);//独自レイアウトをNotificationに設定
        builder.setTicker("M+ now Playing...");// 通知領域に初期表示時のメッセージを設定
        builder.setContentIntent(pi);//pendingIntentをセット
        builder.setDefaults(Notification.DEFAULT_LIGHTS);

        /**********これでも表示されない**********
         mNotificationView.setImageViewBitmap(R.id.imageicon, jacketImage_S);
         mNotificationView.setImageViewBitmap(R.id.imageicon, image);
         mNotificationView.setImageViewUri(R.id.imageicon, Uri.parse(imageURI_S));//これだと画像が表示されない
         */
        mNotificationView.setTextViewText(R.id.textTitle, trackName_S);// ステータスバーのレイアウトに設定されていタイトル名にタイトルを設定
        mNotificationView.setTextViewText(R.id.textArtist, artistName_S);// ステータスバーのレイアウトに設定されているアーティスト名にアーティストを設定

        Intent service = new Intent(MusicService.this,PlayerActivity.class);
        mNotificationView.setOnClickFillInIntent(R.id.imageicon, service);

        mNotificationView.setOnClickPendingIntent(R.id.btnPlay, createPendingIntent(R.id.btnPlay));// 再生、一時停止の際に呼ばれるIntent設定
        mNotificationView.setOnClickPendingIntent(R.id.btnNext, createPendingIntentNext(R.id.btnNext));// 次へボタンの際に呼ばれるIntentを設定

        NotificationManager manager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (builder.build() != null) {
            manager.notify(1,builder.build());
            Picasso.with(this).load(imageURI_S).into(mNotificationView, R.id.imageicon, 1, builder.build());//アイコン画像を刺した
        }
    }

    private PendingIntent createPendingIntent(int id) {//generateNotificationで呼ばれる
        Intent service = new Intent(MusicService.this,PlayerActivity.class);
        service.setAction("ACTION_STOP_PLAY");
        return PendingIntent.getActivity(MusicService.this, id, service, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createPendingIntentNext(int id) {//generateNotificationで呼ばれる
        Intent service = new Intent(MusicService.this,PlayerActivity.class);
        service.setAction("ACTION_NEXT");
        return PendingIntent.getActivity(MusicService.this, id, service, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public MediaPlayer getMediaPlayer() throws RemoteException {
        return mp;
    }
}
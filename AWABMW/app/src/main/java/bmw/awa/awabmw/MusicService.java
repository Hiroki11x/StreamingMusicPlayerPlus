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

    private static MediaPlayer mp;
    private int loopState = 0;//loopの選択肢
    private String trackName_S = "";
    private String previewUrl_S = "";
    private String imageURI_S = "";
    private String artistName_S = "";
    private String albumName_S = "";
//    /* ダイアログ(くるくるするやつ) */
//    private ProgressDialog dialog;


    // Serviceに接続するためのBinderクラスを実装する
    public class LocalBinder extends Binder {
        //Serviceの取得
        MusicService getService() {
            return MusicService.this;
        }
    }

    // Binderの生成
    private final IBinder mBinder = new LocalBinder();


    //1  Serviceが初回作成時に呼ばれる。
    @Override
    public void onCreate() {

        /*本当はUIロード状況表示して、AsyncTaskロードとかやりたい人生だった
        dialog = new ProgressDialog(PlayerActivity.class.get);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("登録中");
        dialog.show();

        PrepareTask task = new PrepareTask();
        task.execute();
        */
        subOnCreateService(false);//falseとする時ランダムでなく最新のものを取得
    }

    public void subOnCreateService(boolean random){
        if (mp != null) {
            if (mp.isPlaying()) {
                mp.stop();
            }
            mp.release();
            mp = null;
        }
        try {
            getIntentContents(random);//falseとする時ランダムでなく最新のものを取得
            if (!TextUtils.isEmpty(previewUrl_S)) {
                mediaplayerPrepare(previewUrl_S);//エラー出た
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }



    //2  Service接続時に呼ばれる。ServiceとActivityを仲介するIBinderを返却する。
    @Override
    public IBinder onBind(Intent intent) {
        // Service接続時に呼び出される
        // 戻り値として、Serviceクラスとのbinderを返す。

        /**
         * intent.setActionみたいなので、notificationから再生なのか停止なのかを見るためにsetしてあるものを受け取りたいが
         * intentをServiceはgetIntent()で受け取れないので、onBind()とかの引数で取得するしかない??
         *
         *

         if (intent != null) {
         getIntentContents(intent);
         String action = intent.getAction();
         if ("play".equals(action)) {
         if (mp == null || !mp.isPlaying()) {
         if(!TextUtils.isEmpty(previewUrl_S)){
         mediaplayerPrepare(previewUrl_S);//エラー出た
         mp.start();
         //startForeground(1, generateNotification());
         }
         }
         } else if ("pause".equals(action)) {
         if (mp != null && mp.isPlaying()) {
         mp.pause();
         }
         } else if ("stop".equals(action)) {
         if (mp != null) {
         mp.stop();
         }
         } else if ("next".equals(action)) {
         //
         } else if ("back".equals(action)) {
         //
         } else if ("playpause".equals(action)) {
         if (mp != null && mp.isPlaying()) {
         mp.pause();
         } else if (!TextUtils.isEmpty(previewUrl_S)) {
         mediaplayerPrepare(previewUrl_S);
         mp.start();
         }
         }

         }
         */

        return mBinder;
    }


    //4 クライアントがServiceと切断されたタイミングで呼ばれる。
    @Override
    public boolean onUnbind(Intent intent) {
        // Service切断時に呼び出される
        //onUnbindをreturn trueでoverrideすると次回バインド時にonRebildが呼ばれる
//        mp.pause();
//        mp.release();
        stopForeground(true);//通知の削除
        return true;
    }


    //5 Serviceが使用されなくなったタイミングで呼ばれる Serviceの持っているリソースをクリーンアップする。
    @Override
    public void onDestroy() {
//        super.onDestroy();
//        if(mp!=null){
//            mp.stop();
//            mp.release();
//            mp = null;
//        }
    }

    @Override
    public void onRebind(Intent intent) {
        // Unbind後に再接続する場合に呼ばれる
        Log.i("", "onRebind" + ": " + intent);
    }


    public void mediaplayerPrepare(String args) throws RemoteException {//mediaPlayerの再生準備
        mp = new MediaPlayer();
        try {
            mp.setDataSource(this, Uri.parse(args));//エラー出た
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


    public String calcDuration(boolean isFirstCall) throws RemoteException {

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
    public void startOrStop() throws RemoteException {//Activity#centerButtonClickedから呼ばれる
        //再生ボタンの設定
        if (mp.isPlaying()) {//再生->停止の時
            mp.pause();
            stopForeground(true);
        } else {//停止->再生の時
            mp.start();
            generateNotification();//Notification開始
//            startForeground(1, generateNotification());
        }
    }

    public void nextTrackService() throws RemoteException{
        subOnCreateService(true);//trueとする時ランダムに取得SharedPreferenceから取得
    }




    public void setLoopStateService() throws RemoteException {
        loopState = (loopState + 1) % 2;
        switch (loopState) {
            case 0:
                mp.setLooping(false);
                break;
            case 1:
                mp.setLooping(true);
                break;
        }
    }

    public void getIntentContents(boolean random) throws RemoteException {

        //DataBaseからそれぞれの値を取得してくる
        Item item = null;
        if(random){

            SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
            long keyId = data.getLong("key",1 );

            item = new Select().from(Item.class).where("Id = ?", keyId).executeSingle();


        }else{
            item = new Select().from(Item.class).orderBy("id DESC").executeSingle();
        }

        trackName_S = item.track_name;
        previewUrl_S = item.previewUrl;
        imageURI_S = item.artworkUrl100;
        artistName_S = item.artistName;
        albumName_S = item.collectionName;
    }

    //targetAPtとかSuppressWarning入れないと落ちる。
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1| Build.VERSION_CODES.JELLY_BEAN_MR2|Build.VERSION_CODES.KITKAT|Build.VERSION_CODES.JELLY_BEAN|Build.VERSION_CODES.LOLLIPOP|Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressWarnings("deprecation")
    private void  generateNotification() {

        //通知領域タップ時のPendingIntentを生成
        Intent actionIntent = new Intent(getApplicationContext(), PlayerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
                0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //独自レイアウトのremoveViewを実装
        RemoteViews mNotificationView = new RemoteViews(getPackageName(), R.layout.notification_statusbar);

        //Notificationの生成
        Notification.Builder builder = new Notification.Builder(
                getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_stat_media);
        //独自レイアウトをNotificationに設定
        builder.setContent(mNotificationView);
        // true常に通知領域に表示
        builder.setOngoing(true);
        // 通知領域に初期表示時のメッセージを設定
        builder.setTicker("M+ now Playing...");
        builder.setContentIntent(pi);
        builder.setDefaults(Notification.DEFAULT_LIGHTS);

        // ステータスバーにレイアウト設定されているイメージアイコンを設定
//        mNotificationView.setImageViewResource(R.id.imageicon,
//                R.drawable.ic_launcher);//第一引数はセット先のID

//        mNotificationView.setImageViewBitmap(R.id.imageicon, jacketImage_S);

        /**********これでも表示されない**********
         *
         *
         SmartImageView image = new SmartImageView(this);
         image.setImageURI(Uri.parse(imageURI_S));
         mNotificationView.setImageViewBitmap(R.id.imageicon, image.getDrawingCache());
         *
         *
         *
         ContentResolver cr = getContentResolver();
         Bitmap image =null;
         try{
         InputStream in = cr.openInputStream(Uri.parse(imageURI_S));
         image = BitmapFactory.decodeStream(in);
         in.close();
         }catch (FileNotFoundException e){
         e.printStackTrace();
         }catch (IOException e){
         e.printStackTrace();
         }
         mNotificationView.setImageViewBitmap(R.id.imageicon, image);
         */


        mNotificationView.setImageViewUri(R.id.imageicon, Uri.parse(imageURI_S));//これだと画像が表示されない

        // ステータスバーのレイアウトに設定されていタイトル名にタイトルを設定
        mNotificationView.setTextViewText(R.id.textTitle, trackName_S);
        // ステータスバーのレイアウトに設定されているアーティスト名にアーティストを設定
        mNotificationView.setTextViewText(R.id.textArtist, artistName_S);

        // イメージアイコンを押された時のintentを設定
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0, new Intent(MusicService.this, PlayerActivity.class), PendingIntent.FLAG_ONE_SHOT);
        //PendingIntent.getActivity(this, 0, new Intent(this, PlayerActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
        mNotificationView
                .setOnClickPendingIntent(R.id.imageicon, contentIntent);

        // 再生、一時停止の際に呼ばれるIntent設定
        mNotificationView.setOnClickPendingIntent(R.id.btnPlay, createPendingIntent("playpause"));

        // 次へボタンの際に呼ばれるIntentを設定
        mNotificationView.setOnClickPendingIntent(R.id.btnNext, createPendingIntent("next"));


        NotificationManager manager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (builder.build() != null) {
            manager.notify(1,builder.build());
        }

    }

    private PendingIntent createPendingIntent(String action) {//generateNotificationで呼ばれる
        Intent service = new Intent(this, MusicService.class);
//        service.setAction(action);
        return PendingIntent.getService(this, 0, service, 0);
    }


    public MediaPlayer getMediaPlayer() throws RemoteException {
        return mp;
    }

    /****
     class PrepareTask extends AsyncTask<Void, Void, Void> {


    @Override
    protected Void doInBackground(Void... params) {
    // DB登録等のUIに関与しない処理

    if (mp != null) {
    if (mp.isPlaying()) {
    mp.stop();
    }
    mp.release();
    mp = null;
    }

    try {
    getIntentContents();
    if (!TextUtils.isEmpty(previewUrl_S)) {
    mediaplayerPrepare(previewUrl_S);//エラー出た

    }
    } catch (RemoteException e) {
    e.printStackTrace();
    }
    dialog.dismiss();
    return params[0];
    return null;
    }


    @Override
    public Void onPostExecute(Void... params) {
    // くるくるを消去

    return params[0];
    }
    }
     */

}
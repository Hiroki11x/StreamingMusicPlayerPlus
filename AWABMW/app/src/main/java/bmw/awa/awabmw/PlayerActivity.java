package bmw.awa.awabmw;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.activeandroid.query.Select;
import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.loopj.android.image.SmartImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hirokinaganuma on 15/08/27.
 *
 * //必要であればmBoundServiceを使ってバインドしたServiceへの制御を行う
 *
 * MusicServiceClassにライフサイクルをまとめてあります。
 */
public class PlayerActivity extends Activity implements ObservableScrollViewCallbacks{//曲を選択した時のアクティビティ

    private MusicService mBindService;
    ImageButton playButton;
    SmartImageView jacketImage;
    TextView titleText, artistText;
    CircularSeekBar seekBar;
//    SeekBar intentSeekBar;
    Timer timer;
    Handler handler;
    TextView LeftSideText,RightSideText;//現在の再生位置を表示
    String maxLength,nowLength;//曲の現在時間を出すところ(全体時間と現在時間)
    AlphaAnimation feedin_btn;
    AlphaAnimation feedout_btn;
    ObservableListView observallistView;
    View HeaderView,MiddleView;
    TextView nowPlaying,centerBar,rightText;
    private ListAdapter mAdapter;
    private boolean flag=true;
    OnItemClickListener listener;


    //-----------------このReceiverはActivity起動時にしか使えない------------------------
    public BroadcastReceiver myReceiver = new BroadcastReceiver() {//Serviceからの受信機
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("onReceive", "@FromServiceBroadcastReceiver Called");
            nextTrack();
            Log.d("DEBUG TEST", "----------intent BY BROADCASTRECEIVER----------");
        }
    };//------------------------------------------------------------------------------

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

        mAdapter = new ListAdapter(this, R.layout.player_list_item);//list_itemは画像と文字があるリストを選択
        observallistView = (ObservableListView)findViewById(R.id.listView);
        observallistView.setScrollViewCallbacks(this);
        HeaderView = getLayoutInflater().inflate(R.layout.player_header,null,false);
        MiddleView = getLayoutInflater().inflate(R.layout.player_middle_header,null,false);

        observallistView.addHeaderView(HeaderView);
        observallistView.addHeaderView(MiddleView);
        observallistView.setAdapter(mAdapter);
        listener = new OnItemClickListener();
        observallistView.setOnItemClickListener(listener);

        //notificationからの呼び出し
        if(getIntent().getAction()=="ACTION_STOP_PLAY") {//playpauseが押されたとき
            Log.d("DEBUG TEST","----------onCreate Intent PLAY PAUSE ----------");
            subOnCreate(false);
            nextTrack();
        }else if(getIntent().getAction()=="ACTION_NEXT"){
            Log.d("DEBUG TEST", "----------onCreate Intent NEXT TRACK----------");
            subOnCreate(false);
            nextTrack();
        }else{//普通のActivity呼び出し
            Log.d("DEBUG TEST", "----------onCreate Intent DAFAULT----------");
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
        playButton = (ImageButton) HeaderView.findViewById(R.id.start);//再生ボタンを関連付け
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flag){
                    start();
                }
            }
        });
        jacketImage = (SmartImageView) HeaderView.findViewById(R.id.imageView);//Jacket画像を
        seekBar = (CircularSeekBar) HeaderView.findViewById(R.id.seek_bar);
        seekBar.setAlpha(0.9f);
        jacketImage.setImageUrl(imageURI);
        jacketImage.setScaleType(ImageView.ScaleType.FIT_CENTER);//画像を正方形で表示);
        nowPlaying = (TextView)MiddleView.findViewById(R.id.now_playing);
        centerBar = (TextView)MiddleView.findViewById(R.id.text_bar);
        rightText = (TextView)MiddleView.findViewById(R.id.text_right);
        titleText = (TextView) MiddleView.findViewById(R.id.track_text_view);//
        titleText.setText(trackName);//トラック名をセット
        artistText = (TextView) MiddleView.findViewById(R.id.artist_text_view);
        artistText.setText(artistName);//アーティスト名をセット
        tryGetMusic(artistName);
        LeftSideText = (TextView)HeaderView.findViewById(R.id.textlefttime);
        RightSideText = (TextView)HeaderView.findViewById(R.id.textrighttime);

        feedin_btn = new AlphaAnimation( 0, 1 );//(0,1)フェードイン
        feedin_btn.setDuration(500);//表示時間を指定
        feedin_btn.setFillAfter(true);//もとに戻らない

        feedout_btn = new AlphaAnimation( 1, 0 );//フェードインアニメーションの準備する(1,0)フェードアウト
        feedout_btn.setDuration(500);//表示時間を指定
        feedout_btn.setFillAfter(true);//もとに戻らない

        mBindService.isActivityExist = true;
        Log.d("DEBUG TEST", "commonOnCreate isActivity:" + mBindService.isActivityExist);
    }


    public void start() {//再生&停止ボタンが押された時の処理
        centerButtonClicked();//再生ボタンが押された処理をサブルーチン化
    }

    public void centerButtonClicked(){
        if(mBindService!=null) {
            try {
                mBindService.startOrStop();//再生もしくは停止
                if(mBindService.getMediaPlayer().isPlaying()){
                    playButton.setBackground(getResources().getDrawable(R.drawable.pause));
                    playButton.startAnimation(feedout_btn);
                    playButton.startAnimation(feedin_btn);
                }else{
                    playButton.setBackground(getResources().getDrawable(R.drawable.play));
                    playButton.startAnimation(feedout_btn);
                    playButton.startAnimation(feedin_btn);
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
                    }, 0, 500); // 1000ミリ秒間隔で実行 timerTaskを実行
                }
            }catch (RemoteException e) {
                e.printStackTrace();
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
    }

    public void seekBarPrepare() {//seekBarの再生準備 afterbinding
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
                if (mBindService != null && flag) {
                    try {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            RightSideText.setText(maxLength); // 現在の再生位置をセット
                            LeftSideText.setText(nowLength);
                            int progress = seekBar.getProgress();
                            mBindService.getMediaPlayer().seekTo(progress);
                            mBindService.getMediaPlayer().start();
                            playButton.setBackground(getResources().getDrawable(R.drawable.pause));
                            playButton.startAnimation(feedin_btn);

                        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            RightSideText.setText(maxLength); // 現在の再生位置をセット
                            LeftSideText.setText("...");
                            mBindService.getMediaPlayer().pause();

                            playButton.setBackground(getResources().getDrawable(R.drawable.play));
                            playButton.startAnimation(feedout_btn);


                        }
                    } catch (RemoteException e) {
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
        if(myReceiver!=null) {
            unregisterReceiver(myReceiver);
        }
        Log.d("DEBUG TEST", "onDestroy isActivity:" + mBindService.isActivityExist);
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

    public void onScrollChanged(int scroll_y, boolean var2, boolean var3){
        //scroll量
        //
        MiddleView.setTranslationY(8);
//        HeaderView.setTranslationY(scroll_y*1.2f);
        HeaderView.setTranslationY(scroll_y);
        WindowManager wm = getWindowManager();
        Display disp = wm.getDefaultDisplay();
        float percent = scroll_y/(float)disp.getWidth();
        jacketImage.setAlpha(1.5f - percent);
        jacketImage.setScaleX(1.0f + percent);
        jacketImage.setScaleY(1.0f + percent);
        if(scroll_y>380){
            nowPlaying.setText("now playing " +nowLength);
            centerBar.setText(" | ");
            rightText.setText(maxLength);
//            playButton.setVisibility(View.INVISIBLE);
            flag = false;
        } else {
            flag =true;
//            playButton.setVisibility(View.VISIBLE);
            playButton.setAlpha(1.0f-percent*4);
            seekBar.setAlpha(0.9f-percent*4);
            nowPlaying.setText("now playing");
            centerBar.setText("");
            rightText.setText("");
//            jacketImage.setAlpha(1.0f);
        }
    }


    public void onDownMotionEvent(){

    }

    public void onUpOrCancelMotionEvent(ScrollState var1){

    }

    private class ListAdapter extends ArrayAdapter<JSONObject> {//ListAdapterクラスを内部クラスとして定義

        public ListAdapter(Context context, int resource) {//コントラクタでsourceとかはセット
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // 再利用可能なViewがない場合は作る(ListViewが下まで行っちゃった時)
                convertView = getLayoutInflater().inflate(R.layout.player_list_item, null);
            }
            //ImageVieだと、URIから画像セットがイマイチうまくいかないのでSmartImageViewを使用
            SmartImageView imageView = (SmartImageView) convertView.findViewById(R.id.image_view);
            TextView trackTextView = (TextView) convertView.findViewById(R.id.track_text_view);
            TextView artistTextView = (TextView) convertView.findViewById(R.id.artist_text_view);

            // 表示する行番号のデータを取り出す
            JSONObject result = getItem(position);//positionにクリックされた要素の番号が渡されている

            //resultとしてJSONオブジェクトが渡されている状態
            imageView.setImageUrl(result.optString("artworkUrl100"));
            trackTextView.setText(result.optString("trackName"));
            artistTextView.setText(result.optString("artistName"));
            Log.d("", "call_getView" + System.currentTimeMillis());//getViewが呼ばれていたのか確認
            return convertView;//ListViewの1要素のViewを返す
        }
    }

    private class OnItemClickListener implements AdapterView.OnItemClickListener {//Item選択された時のリスナクラス
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {


            Log.d("DEBUG GENIUS",position+"");
            //選択したアイテムのオブジェクトをDBに登録
            JSONObject result = mAdapter.getItem(position);//JSONObject取得
            Item item = new Item();
            item.track_name=result.optString("trackName");
            item.previewUrl=result.optString("previewUrl");
            item.artworkUrl100=result.optString("artworkUrl100");
            item.artistName=result.optString("artistName");
            item.collectionName=result.optString("collectionName");
            item.registerTime=System.currentTimeMillis();
            item.save();

            Intent intent = new Intent(PlayerActivity.this, PlayerActivity.class);//PlayerActivityに明示的intent
            startActivity(intent);//intent開始
        }
    }

    public void tryGetMusic(String text){
        try {
            // url encode 例. スピッツ > %83X%83s%83b%83c みたいになるらしい
            text = URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e("", e.getMessage(), e);
            return ;
        }


        if (!TextUtils.isEmpty(text)) {//EditTextが空列でなければ
            // iTunes API から取得してくるのでURLを準備
            //このURLだけ検索ワードから色々ひっかけてくれる
            String urlString = "https://itunes.apple.com/search?term=" + text + "&country=JP&media=music&lang=ja_jp";

            new AsyncTask<String, Void, JSONObject>() {//AsyncTask実行
                //1番目はバックグラウンド処理を実行する時にUIスレッド（メインスレッド）から与える引数の型:String
                //2番目のProgressは進捗状況を表示するonProgressUpdateの引数の型:Void(今回は使わない)
                //最後のはバックグラウンド処理の後に受け取る型:JSONObject

                @Override
                protected JSONObject doInBackground(String... params) {//バックグラウンドで行う処理を記述する
                    HttpURLConnection conn;
                    try {
                        //params[0]にはurlStringが入るので、URLから接続を開始
                        URL url = new URL(params[0]);
                        conn = (HttpURLConnection) url.openConnection();

                    } catch (MalformedURLException e) {
                        Log.e("", e.getMessage(), e);
                        return null;
                    } catch (IOException e) {
                        Log.e("", e.getMessage(), e);
                        return null;
                    }

                    StringBuilder result = new StringBuilder();

                    //Java7以降のtry-catch-resoureceかな
                    //検索で引っかかったものが、文字の羅列でくるので、1行ずつresult(StringBuilder)にappend
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String s;
                        while ((s = reader.readLine()) != null) {
                            result.append(s);//resultはStringBuilderのこと
                        }
                    } catch (IOException e) {
                        Log.e("", e.getMessage(), e);
                        return null;
                    }

                    try {
                        return new JSONObject(result.toString());//result(StringBuilder)からJSONObjectを生成してreturn
                    } catch (JSONException e) {
                        Log.e("", e.getMessage(), e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(JSONObject jsonObject) {//doInBackground後の処理
                    Log.d("", jsonObject.toString());

                    mAdapter.clear();//ListVierに突っ込むAdapterを一度クリア

                    JSONArray results = jsonObject.optJSONArray("results");
                    //results(iTunes APIから取得できる楽曲情報全てをまとめた選択の意味)よりJSONObjectを取得
                    if (results != null) {
                        for (int i = 0; i <((results.length()>6)?6:results.length()); i++) {
                            mAdapter.add(results.optJSONObject(i));//JSONArrayのi番目の要素をAdapterに追加

                        }
                    }
                    Log.d("", "end mAdapter.add()" + System.currentTimeMillis());
                }
            }.execute(urlString);//AsyncTaskを実行
        }

    }
}



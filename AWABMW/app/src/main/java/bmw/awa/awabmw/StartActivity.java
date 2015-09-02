package bmw.awa.awabmw;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;

import com.loopj.android.image.SmartImageView;
import com.vstechlab.easyfonts.EasyFonts;

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

/**
 * Created by hirokinaganuma on 15/09/02.
 */
public class StartActivity extends Activity {


    private GridAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//        mAdapter = new ListAdapter(this, R.layout.player_header);//list_itemは画像と文字があるリストを選択
//        ListView listView = (ListView) findViewById(R.id.list_view);//activity_list内のlist_viewをListView を選択
//
//        //ここはonCreate()なので、getView以降は呼ばれていない
//        listView.setAdapter(mAdapter);//listViewにadapterをセット(単なる関連付けmAdapterにaddしたら、listViewに反映)
//        listView.setOnItemClickListener(new OnItemClickListener());//listViewリスナを設定

        // GridViewのインスタンスを生成
        GridView gridview = (GridView) findViewById(R.id.gridview);
        // BaseAdapter を継承したGridAdapterのインスタンスを生成
        // 子要素のレイアウトファイル grid_items.xml を main.xml に inflate するためにGridAdapterに引数として渡す
        adapter = new GridAdapter(this.getApplicationContext(), R.layout.grid_items);
        // gridViewにadapterをセット
        gridview.setAdapter(adapter);
        gridview.setOnItemClickListener(new OnItemClickListener());
        tryGetMusic("pop");

        final EditText editText = (EditText) findViewById(R.id.edit_text);//曲の検索画面
        editText.setTypeface(EasyFonts.robotoMedium(this));
        editText.setOnKeyListener(new OnKeyListener());//文字入力のEditTextにリスナ追加

    }


    private class OnKeyListener implements View.OnKeyListener {//EditTextのリスナも内部クラスで定義
        //することは主に検索ワードからJsonオブジェクトを持ってくるということ

        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {//Enterが押されて指が離れた時
                EditText editText = (EditText) view;//このリスナがセットされるのはEditTextなので,引数のViewにはEditTextがくるはず

                // キーボードを閉じる
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                String text = editText.getText().toString();//検索ワードをEditTextから取得
                if (!TextUtils.isEmpty(text)) {//EditTextが空列でなければ
                    Intent toMain = new Intent(StartActivity.this,MainActivity.class);
                    toMain.setAction("FROM_START");
                    toMain.putExtra("searchword", text);
                    startActivity(toMain);
                }
                return true;
            }
            return  false;
        }
    }

    private class OnItemClickListener implements AdapterView.OnItemClickListener {//Item選択された時のリスナクラス

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {


            //選択したアイテムのオブジェクトをDBに登録
            JSONObject result = adapter.getItem(position);//JSONObject取得
            Item item = new Item();
            item.track_name=result.optString("trackName");
            item.previewUrl=result.optString("previewUrl");
            item.artworkUrl100=result.optString("artworkUrl100");
            item.artistName=result.optString("artistName");
            item.collectionName=result.optString("collectionName");
            item.registerTime=System.currentTimeMillis();
            item.save();

            SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = data.edit();
            editor.putLong("key", item.getId());//再生する曲をインクリメントで次へ
            editor.apply();//Activity存在していないときはitemをSharedPreference保存

            for(int i =0;i<10;i++){
                JSONObject tempResult = adapter.getItem(i);//JSONObject取得
                Item tempItem = new Item();
                tempItem.track_name=tempResult.optString("trackName");
                tempItem.previewUrl=tempResult.optString("previewUrl");
                tempItem.artworkUrl100=tempResult.optString("artworkUrl100");
                tempItem.artistName=tempResult.optString("artistName");
                tempItem.collectionName=tempResult.optString("collectionName");
                tempItem.registerTime=System.currentTimeMillis();
                tempItem.save();
            }

            Intent intent = new Intent(StartActivity.this, PlayerActivity.class);//PlayerActivityに明示的intent
            startActivity(intent);//intent開始
        }
    }

    class GridAdapter extends ArrayAdapter<JSONObject> {
        private LayoutInflater inflater;
        private int layoutId;

        public GridAdapter(Context context,  int layoutId) {
            super(context, layoutId);
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.layoutId = layoutId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //このメソッドは、開発者が明示的に呼ぶものではなく、
            //position番目のデータがスクロールなどで
            //表示される時に、アプリに自動的に呼び出される。

            if (convertView == null) {
                // 再利用可能なViewがない場合は作る(ListViewが下まで行っちゃった時)
                convertView = getLayoutInflater().inflate(R.layout.grid_items, null);
            }
            //ImageVieだと、URIから画像セットがイマイチうまくいかないのでSmartImageViewを使用
            SmartImageView imageView = (SmartImageView) convertView.findViewById(R.id.imageview);

            // 表示する行番号のデータを取り出す
            JSONObject result = getItem(position);//positionにクリックされた要素の番号が渡されている

            //resultとしてJSONオブジェクトが渡されている状態
            imageView.setImageUrl(result.optString("artworkUrl100"));
            Log.d("", "call_getView" + System.currentTimeMillis());//getViewが呼ばれていたのか確認
            return convertView;//ListViewの1要素のViewを返す
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
            String urlString = "https://itunes.apple.com/search?term=" + text + "&country=JP&media=music&lang=en";

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

                    adapter.clear();//ListVierに突っ込むAdapterを一度クリア

                    JSONArray results = jsonObject.optJSONArray("results");
                    //results(iTunes APIから取得できる楽曲情報全てをまとめた選択の意味)よりJSONObjectを取得
                    if (results != null) {
                        for (int i = 0; i <results.length(); i++) {
                            adapter.add(results.optJSONObject(i));//JSONArrayのi番目の要素をAdapterに追加
                        }
                    }
                    Log.d("", "end mAdapter.add()" + System.currentTimeMillis());
                }
            }.execute(urlString);//AsyncTaskを実行
        }

    }

}

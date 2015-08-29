package bmw.awa.awabmw;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

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


public class MainActivity extends Activity {

//起動時のメイン画面

    private ListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mAdapter = new ListAdapter(this, R.layout.list_item);//list_itemは画像と文字があるリストを選択
        ListView listView = (ListView) findViewById(R.id.list_view);//activity_list内のlist_viewをListView を選択

        //ここはonCreate()なので、getView以降は呼ばれていない
        listView.setAdapter(mAdapter);//listViewにadapterをセット(単なる関連付けmAdapterにaddしたら、listViewに反映)
        listView.setOnItemClickListener(new OnItemClickListener());//listViewリスナを設定

        final EditText editText = (EditText) findViewById(R.id.edit_text);//曲の検索画面
        editText.setOnKeyListener(new OnKeyListener());//文字入力のEditTextにリスナ追加
    }

    private class ListAdapter extends ArrayAdapter<JSONObject> {//ListAdapterクラスを内部クラスとして定義

        public ListAdapter(Context context, int resource) {//コントラクタでsourceとかはセット
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //このメソッドは、開発者が明示的に呼ぶものではなく、
            //position番目のデータがスクロールなどで
            //表示される時に、アプリに自動的に呼び出される。

            if (convertView == null) {
                // 再利用可能なViewがない場合は作る(ListViewが下まで行っちゃった時)
                convertView = getLayoutInflater().inflate(R.layout.list_item, null);
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
            Log.d("","call_getView"+System.currentTimeMillis());//getViewが呼ばれていたのか確認
            return convertView;//ListViewの1要素のViewを返す
        }
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
                try {
                    // url encode 例. スピッツ > %83X%83s%83b%83c みたいになるらしい
                    text = URLEncoder.encode(text, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e("", e.getMessage(), e);
                    return true;
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
                        protected void onPostExecute(JSONObject jsonObject){//doInBackground後の処理
                            Log.d("", jsonObject.toString());

                            mAdapter.clear();//ListVierに突っ込むAdapterを一度クリア

                            JSONArray results = jsonObject.optJSONArray("results");
                            //results(iTunes APIから取得できる楽曲情報全てをまとめた選択の意味)よりJSONObjectを取得
                            if (results != null) {
                                for (int i = 0; i < results.length(); i++) {
                                    mAdapter.add(results.optJSONObject(i));//JSONArrayのi番目の要素をAdapterに追加

                                    //mAdapterは上で定義したListAdapterのこと
                                    //ListViewにsetAdapterしなくていいのかが疑問
                                    //debugしたところ、mAdapter.add呼ばれたのちにgetViewが呼ばれている模様
                                }
                            }
                            Log.d("","end mAdapter.add()"+System.currentTimeMillis());
                        }
                    }.execute(urlString);//AsyncTaskを実行
                }
                return true;
            }
            return false;
        }
    }

    private class OnItemClickListener implements AdapterView.OnItemClickListener {//Item選択された時のリスナクラス

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {



            /*
            intent.putExtra("track_name", result.optString("trackName"));//そのJSONObjectの名前をPlayerアクティビティに受け渡す
            intent.putExtra("preview_url", result.optString("previewUrl"));//そのJSONObjectURI(音楽を聴くためにもの)をPlayerアクティビティに受け渡す
            intent.putExtra("artworkUrl100", result.optString("artworkUrl100"));//その画像のURLをPlayerアクティビティに受け渡す
            intent.putExtra("artistName", result.optString("artistName"));//その画像アーティスト名をPlayerアクティビティに受け渡す
            intent.putExtra("collectionName", result.optString("collectionName"));//その画像アーティスト名をPlayerアクティビティに受け渡す
            */

//            Item _item = new Select().from(Item.class).orderBy("id DESC").executeSingle();
//            if(_item!=null){
//                _item.delete();
//            }
            JSONObject result = mAdapter.getItem(position);//JSONObject取得

            Item item = new Item();
            item.track_name=result.optString("trackName");
            item.previewUrl=result.optString("previewUrl");
            item.artworkUrl100=result.optString("artworkUrl100");
            item.artistName=result.optString("artistName");
            item.collectionName=result.optString("collectionName");
            item.registerTime=(int)System.currentTimeMillis();
            item.save();
            Log.d("***************", "***************************************");
            Log.d("***************", "***************************************");
            Log.d("item_saved","item.track_name: "+item.previewUrl);
            Log.d("***************","***************************************");
            Log.d("***************","***************************************");

            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);//PlayerActivityに明示的intent
            startActivity(intent);//intent開始
        }
    }


}

package bmw.awa.awabmw;

import android.app.Activity;
import android.app.ListActivity;
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
        listView.setAdapter(mAdapter);//listViewにadapterをセット
        listView.setOnItemClickListener(new OnItemClickListener());//listViewリスナを設定

        final EditText editText = (EditText) findViewById(R.id.edit_text);//曲の検索画面
        editText.setOnKeyListener(new OnKeyListener());
    }

    private class ListAdapter extends ArrayAdapter<JSONObject> {

        public ListAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // 再利用可能なViewがない場合は作る
                convertView = getLayoutInflater().inflate(R.layout.list_item, null);
            }

            SmartImageView imageView = (SmartImageView) convertView.findViewById(R.id.image_view);
            TextView trackTextView = (TextView) convertView.findViewById(R.id.track_text_view);
            TextView artistTextView = (TextView) convertView.findViewById(R.id.artist_text_view);

            // 表示する行番号のデータを取り出す
            JSONObject result = getItem(position);

            imageView.setImageUrl(result.optString("artworkUrl100"));
            trackTextView.setText(result.optString("trackName"));
            artistTextView.setText(result.optString("artistName"));

            return convertView;
        }
    }

    private class OnKeyListener implements View.OnKeyListener {

        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                EditText editText = (EditText) view;

                // キーボードを閉じる
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                String text = editText.getText().toString();
                try {
                    // url encode　例. スピッツ > %83X%83s%83b%83c
                    text = URLEncoder.encode(text, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e("", e.getMessage(), e);
                    return true;
                }
                if (!TextUtils.isEmpty(text)) {
                    String url =
                            "https://itunes.apple.com/search?term=" + text + "&country=JP&media=music&lang=ja_jp";

                    new AsyncTask<String, Void, JSONObject>() {

                        @Override
                        protected JSONObject doInBackground(String... params) {
                            HttpURLConnection conn;
                            try {
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
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                                String s;
                                while ((s = reader.readLine()) != null) {
                                    result.append(s);
                                }
                            } catch (IOException e) {
                                Log.e("", e.getMessage(), e);
                                return null;
                            }

                            try {
                                return new JSONObject(result.toString());
                            } catch (JSONException e) {
                                Log.e("", e.getMessage(), e);
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(JSONObject jsonObject) {
                            Log.d("", jsonObject.toString());

                            mAdapter.clear();

                            JSONArray results = jsonObject.optJSONArray("results");
                            if (results != null) {
                                for (int i = 0; i < results.length(); i++) {
                                    mAdapter.add(results.optJSONObject(i));
                                }
                            }
                        }
                    }.execute(url);
                }
                return true;
            }
            return false;
        }
    }

    private class OnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);

            JSONObject result = mAdapter.getItem(position);
            intent.putExtra("track_name", result.optString("trackName"));
            intent.putExtra("preview_url", result.optString("previewUrl"));

            startActivity(intent);
        }
    }
}

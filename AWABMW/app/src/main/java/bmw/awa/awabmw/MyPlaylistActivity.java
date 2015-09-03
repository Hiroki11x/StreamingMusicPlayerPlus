package bmw.awa.awabmw;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.activeandroid.query.Select;
import com.loopj.android.image.SmartImageView;
import com.vstechlab.easyfonts.EasyFonts;

import java.util.List;


public class MyPlaylistActivity extends ActionBarActivity {

    private ListAdapter mAdapter;
    OnItemClickListener listener;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_playlist);

        mAdapter = new ListAdapter(this, R.layout.player_list_item);//list_itemは画像と文字があるリストを選択
        listView.setAdapter(mAdapter);
        listener = new OnItemClickListener();
        listView.setOnItemClickListener(listener);

        getItems();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_playlist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private class OnItemClickListener implements AdapterView.OnItemClickListener {//Item選択された時のリスナクラス
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            //選択したアイテムのオブジェクトをDBに登録

            Item result = mAdapter.getItem(position);//Item
            SharedPreferences data = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = data.edit();
            editor.putLong("key", result.getId());//再生する曲をインクリメントで次へ
            editor.apply();//Activity存在していないときはitemをSharedPreference保存

            Intent intent = new Intent(MyPlaylistActivity.this,PlayerActivity.class);
            startActivity(intent);
        }
    }

    private class ListAdapter extends ArrayAdapter<Item> {//ListAdapterクラスを内部クラスとして定義
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
            Item result = getItem(position);//positionにクリックされた要素の番号が渡されている
            //resultとしてJSONオブジェクトが渡されている状態
            imageView.setImageUrl(result.artworkUrl100);
            trackTextView.setText(result.track_name);
            trackTextView.setTypeface(EasyFonts.robotoMedium(getApplication()));
            artistTextView.setText(result.artistName);
            artistTextView.setTypeface(EasyFonts.robotoMedium(getApplication()));
            return convertView;//ListViewの1要素のViewを返す
        }
    }

    public void getItems() {//ListViewに再生待ちの楽曲を突っ込む
        mAdapter.clear();//ListViewに突っ込むAdapterを一度クリア
        // 全件取得
        List<Item> items = new Select().from(Item.class).execute();
        for (Item i : items) {
            mAdapter.add(i);
        }
    }
}

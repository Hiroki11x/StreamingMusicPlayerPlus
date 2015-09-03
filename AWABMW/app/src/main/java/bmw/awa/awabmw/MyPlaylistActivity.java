package bmw.awa.awabmw;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.activeandroid.query.Select;
import com.loopj.android.image.SmartImageView;
import com.vstechlab.easyfonts.EasyFonts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MyPlaylistActivity extends ActionBarActivity {

    private ListAdapter mAdapter;
    OnItemClickListener listener;
    ListView listView;

    private DrawerLayout mDrawerLayout;
    private RecyclerView mRecyclerView;
    private DrawerAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private LinearLayoutManager mLayoutManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_playlist);

        listView = (ListView)findViewById(R.id.list_view);

        mAdapter = new ListAdapter(this, R.layout.player_list_item);//list_itemは画像と文字があるリストを選択
        listView.setAdapter(mAdapter);
        listener = new OnItemClickListener();
        listView.setOnItemClickListener(listener);

        getItems();

        /**
         * DrawerLayout
         */
        // ドロワーの開け閉めをActionBarDrawerToggleで監視
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // ドロワー開くボタン(三本線)を表示
        mDrawerToggle.setDrawerIndicatorEnabled(true);

//        mDrawerToggle.setHomeAsUpIndicator(menuIcon.setState(MaterialMenuDrawable.IconState.ARROW));

//        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
//            @Override
//            public void onDrawerSlide(View drawerView, float slideOffset) {
//                menuIcon.setTransformationOffset(
//                        MaterialMenuDrawable.AnimationState.BURGER_ARROW,
//                        isDrawerOpened ? 2 - slideOffset : slideOffset
//                );
//            }
//
//            @Override
//            public void onDrawerOpened(View drawerView) {
////                super.onDrawerOpened(drawerView);
//                isDrawerOpened = true;
//            }
//
//            @Override
//            public void onDrawerClosed(View drawerView) {
////                super.onDrawerClosed(drawerView);
//                isDrawerOpened = false;
//            }
//
//            @Override
//            public void onDrawerStateChanged(int newState) {
////                super.onDrawerStateChanged(newState);
//                if (newState == DrawerLayout.STATE_IDLE) {
//                    if (isDrawerOpened) menuIcon.setState(MaterialMenuDrawable.IconState.ARROW);
//                    else menuIcon.setState(MaterialMenuDrawable.IconState.ARROW);
//                }
//            }
//        });

        /**
         * RecyclerView
         */
        mRecyclerView = (RecyclerView) findViewById(R.id.drawer_view);

        // RecyclerView内のItemサイズが固定の場合に設定すると、パフォーマンス最適化
        mRecyclerView.setHasFixedSize(true);

        // レイアウトの選択
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // XML
        TypedArray drawerMenuList = getResources().obtainTypedArray(R.array.drawer_menu_list);
        int menuLength = drawerMenuList.length();

        // RecyclerView.Adapter に渡すデータ
        final ArrayList<HashMap<String, Object>> drawerMenuArr = new ArrayList<>();

        for (int i = 0; i < menuLength; i++) {
            TypedArray itemArr = getResources().obtainTypedArray(drawerMenuList.getResourceId(i, 0));
            int itemLength = itemArr.length();
            HashMap<String, Object> content = new HashMap<>();
            drawerMenuArr.add(content);
            for (int j = 0; j < itemLength; j++) {
                TypedArray contentArr = getResources().obtainTypedArray(itemArr.getResourceId(j, 0));

                // key-value
                if (contentArr.getString(0).contains("icon")) {
                    content.put(contentArr.getString(0), contentArr.getDrawable(1));
                } else {
                    content.put(contentArr.getString(0), contentArr.getString(1));
                }
                contentArr.recycle();
            }
            itemArr.recycle();
        }

        // XMLを読込んで表示する
        mDrawerAdapter = new DrawerAdapter(drawerMenuArr);

        mRecyclerView.setAdapter(mDrawerAdapter);

        // GestureDetectorを使って、onSingleTapUpを検知
        final GestureDetector mGestureDetector = new GestureDetector(getApplicationContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return true;
                    }
                });

        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
                if (mGestureDetector.onTouchEvent(e)) {

                    // onSingleTapUpの時に、タッチしているViewを取得
                    View childView = view.findChildViewUnder(e.getX(), e.getY());
                    int potision = mRecyclerView.getChildPosition(childView);

                    // タッチしているViewのデータを取得
                    HashMap<String, Object> data = drawerMenuArr.get(potision);

                    // Menu アイテムのみ
                    if (data.get("text").toString().equals("My History")) {
                        // ドロワー閉じる
                        mDrawerLayout.closeDrawers();
                        startActivity(new Intent(getApplicationContext(), MyPlaylistActivity.class));

                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean b) {

            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            }
        });
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

            startActivity(new Intent(getApplicationContext(), PlayerActivity.class));
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

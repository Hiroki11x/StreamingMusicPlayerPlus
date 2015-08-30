package bmw.awa.awabmw;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.image.SmartImageView;

import butterknife.Bind;
import butterknife.ButterKnife;


public class RecommendationActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    @Bind(R.id.tabs)
    TabLayout mTabLayout;
    @Bind(R.id.txt_title)
    TextView txtTitle;
    @Bind(R.id.img_jacket)
    SmartImageView imgJacket;
    @Bind(R.id.view_pager)
    ViewPager viewPager;

    String[] factor = new String[]{"album", "artist", "tempo", "tempo2", "tempo3"};
    int[] iconId = {R.drawable.album, R.drawable.artist, R.drawable.tempo, R.drawable.tempo, R.drawable.tempo};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        ButterKnife.bind(this);

        imgJacket.setImageDrawable(getResources().getDrawable(R.drawable.jacket));
        imgJacket.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        mTabLayout.addTab(mTabLayout.newTab());
        mTabLayout.addTab(mTabLayout.newTab());
        mTabLayout.addTab(mTabLayout.newTab());
        mTabLayout.addTab(mTabLayout.newTab());
        mTabLayout.addTab(mTabLayout.newTab());
//        mTabLayout.addTab(mTabLayout.newTab());

        mTabLayout.setOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        tab.setIcon(R.drawable.ic_launcher);
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        int lp = 0;

                        for (lp = 0; lp < mTabLayout.getTabCount(); lp++) {
                            if (tab.getPosition() == lp) {
                                tab.setIcon(getResources().getDrawable(iconId[lp]));
                            }
                        }
//                        Object tag = tab.getTag();
//                        if (tag.equals("artist")) {
//                            tab.setIcon(R.drawable.artist);
//                        } else if (tag.equals("album")) {
//                            tab.setIcon(R.drawable.album);
//                        } else if (tag.equals("tempo")) {
//                            tab.setIcon(R.drawable.tempo);
//                        }
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {

                    }
                }

        );

        mTabLayout.setSelectedTabIndicatorColor(Color.WHITE);

        txtTitle.setSelected(true);

        FragmentPagerAdapter pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return TestFragment.newInstance(position + 1);
            }

            @Override
            public int getCount() {
                return 5;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return null;
            }
        };

        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(this);

        mTabLayout.setupWithViewPager(viewPager);

        mTabLayout.getTabAt(0).setTag("artist").setIcon(R.drawable.artist);
        mTabLayout.getTabAt(1).setTag("album").setIcon(R.drawable.album);
        mTabLayout.getTabAt(2).setTag("tempo").setIcon(R.drawable.tempo);
        mTabLayout.getTabAt(3).setTag("tempo2").setIcon(R.drawable.tempo);
        mTabLayout.getTabAt(4).setTag("tempo3").setIcon(R.drawable.tempo);

        int x = 0;
        for (x = 0; x < mTabLayout.getTabCount(); x++) {
            mTabLayout.getTabAt(x).setTag(factor[x]).setIcon(iconId[x]);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recommendation, menu);
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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public static class TestFragment extends Fragment {

        public TestFragment() {
        }

        public static TestFragment newInstance(int page) {
            Bundle args = new Bundle();
            args.putInt("page", page);
            TestFragment fragment = new TestFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int page = getArguments().getInt("page", 0);
            View view = inflater.inflate(R.layout.fragment_test, container, false);
            ((TextView) view.findViewById(R.id.page_text)).setText("Page " + page);
            return view;
        }
    }
}

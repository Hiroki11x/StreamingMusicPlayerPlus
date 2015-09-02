package bmw.awa.awabmw;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.image.SmartImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by satsukies on 15/08/31.
 */
public class ExpandListAdapter extends BaseExpandableListAdapter {

    private List<Element> elements;
    private Activity activity;
    private LayoutInflater inflater;
    private int page;

    public ExpandListAdapter(Activity act, List<Element> elements, int page) {
        this.activity = act;
        this.elements = new ArrayList<>(elements);
        this.inflater = activity.getLayoutInflater();
        this.page = page;
    }

    @Override
    public Object getChild(int arg0, int arg1) {
        return elements.get(arg0).getChildren().get(arg1);
    }

    @Override
    public long getChildId(int arg0, int arg1) {
        return arg1;
    }

    @Override
    public View getChildView(final int arg0, final int arg1, boolean arg2, View view, ViewGroup arg4) {
        Element element = ((elements.get(arg0)).getChildren()).get(arg1);

        /**
         * 子アイテムの描画
         */
        TextView textView;
        TextView textView2;
        SmartImageView smartImageView;
        switch (page) {
            case 1:
                view = inflater.inflate(R.layout.expandable_child, null);
                textView = (TextView) view.findViewById(R.id.child_title);
                textView.setText(element.getTrackName());
                smartImageView = (SmartImageView) view.findViewById(R.id.child_jacket);
                smartImageView.setImageUrl(element.getArtworkUrl100());
                break;
            case 2:
                view = inflater.inflate(R.layout.expandable_artist_child, null);
                textView = (TextView) view.findViewById(R.id.child_title);
                textView.setText(element.getTrackName());
                textView2 = (TextView)view.findViewById(R.id.number);
                textView2.setText("" + arg1);
                break;
            case 3:
                view = inflater.inflate(R.layout.expandable_album_child, null);
                textView = (TextView) view.findViewById(R.id.child_title);
                textView.setText(element.getTrackName());
                smartImageView = (SmartImageView) view.findViewById(R.id.child_jacket);
                smartImageView.setImageUrl(element.getArtworkUrl100());
                textView2 = (TextView)view.findViewById(R.id.number);
                textView2.setText("" + arg1);
                break;
            default:
                view = inflater.inflate(R.layout.expandable_child, null);
                textView = (TextView) view.findViewById(R.id.child_title);
                textView.setText(element.getTrackName());
                smartImageView = (SmartImageView) view.findViewById(R.id.child_jacket);
                smartImageView.setImageUrl(element.getArtworkUrl100());
                break;
        }

        ((ImageView)view.findViewById(R.id.child_add)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Element e = ((elements.get(arg0)).getChildren()).get(arg1);
                Item selected = new Item(e.getTrackName(), e.getpreviewUrl(), e.getArtworkUrl100(), e.getArtistName(), e.getCollectionName(), e.getRegisterTime());
                selected.save();
                ((ImageView)v).setBackground(activity.getResources().getDrawable(R.drawable.add_list));
            }
        });

        return view;
    }

    @Override
    public int getChildrenCount(int arg0) {
        return elements.get(arg0).getChildren().size();
    }

    @Override
    public Object getGroup(int arg0) {
        return elements.get(arg0);
    }

    @Override
    public int getGroupCount() {
        return elements.size();
    }

    @Override
    public long getGroupId(int arg0) {
        return arg0;
    }

    @Override
    public View getGroupView(final int arg0, boolean isExpanded, View view, ViewGroup parent) {
        Element element = elements.get(arg0);
        try {
            if (element.isParent()) {
                switch (page) {
                    case 1:
                        //tempo
                        view = inflater.inflate(R.layout.expandable_parent, null);
                        break;
                    case 2:
                        //artist
                        view = inflater.inflate(R.layout.expandable_artist_parent, null);
                        ((SmartImageView)view.findViewById(R.id.parent_jacket)).setImageUrl(element.getArtworkUrl100());
                        ((TextView)view.findViewById(R.id.parent_title)).setText(element.getArtistName());
                        break;
                    case 3:
                        //album
                        view = inflater.inflate(R.layout.expandable_album_parent, null);
                        break;
                    default:
                        view = inflater.inflate(R.layout.expandable_parent, null);
                        break;
                }
//            TextView textView = (TextView) view.findViewById(R.id.parent_title);
//            textView.setText(element.getCollectionName());
//            ImageView indicator = (ImageView) view.findViewById(R.id.parent_indicator);
//            indicator.setImageResource(isExpanded ? R.drawable.ic_launcher : android.R.drawable.ic_media_play);

                if (isExpanded) {
                    view.findViewById(R.id.artist_expand).setRotation(0);
                } else {
                    view.findViewById(R.id.artist_expand).setRotation(270);
                }
            } else {
                /**
                 * 子アイテムの描画
                 */
                TextView textView;
                TextView textView2;
                SmartImageView smartImageView;
                switch (page) {
                    case 1:
                        view = inflater.inflate(R.layout.expandable_child, null);
                        textView = (TextView) view.findViewById(R.id.child_title);
                        textView.setText(element.getTrackName());
                        smartImageView = (SmartImageView) view.findViewById(R.id.child_jacket);
                        smartImageView.setImageUrl(element.getArtworkUrl100());
                        break;
                    case 2:
                        view = inflater.inflate(R.layout.expandable_artist_child, null);
                        textView = (TextView) view.findViewById(R.id.child_title);
                        textView.setText(element.getTrackName());
                        textView2 = (TextView)view.findViewById(R.id.number);
                        textView2.setText("0");
                        break;
                    case 3:
                        view = inflater.inflate(R.layout.expandable_album_child, null);
                        textView = (TextView) view.findViewById(R.id.child_title);
                        textView.setText(element.getTrackName());
//                        smartImageView = (SmartImageView) view.findViewById(R.id.child_jacket);
//                        smartImageView.setImageUrl(element.getArtworkUrl100());
                        textView2 = (TextView)view.findViewById(R.id.number);
                        textView2.setText("0");
                        break;
                    default:
                        view = inflater.inflate(R.layout.expandable_child, null);
                        textView = (TextView) view.findViewById(R.id.child_title);
                        textView.setText(element.getTrackName());
                        smartImageView = (SmartImageView) view.findViewById(R.id.child_jacket);
                        smartImageView.setImageUrl(element.getArtworkUrl100());
                        break;
                }

                ((ImageView)view.findViewById(R.id.child_add)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Element e = elements.get(arg0);
                        Item selected = new Item(e.getTrackName(), e.getpreviewUrl(), e.getArtworkUrl100(), e.getArtistName(), e.getCollectionName(), e.getRegisterTime());
                        selected.save();
                        ((ImageView)v).setBackground(activity.getResources().getDrawable(R.drawable.add_list));
                    }
                });
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            view = inflater.inflate(R.layout.expandable_empty, null);
            TextView textView = (TextView) view.findViewById(R.id.empty_message);
            textView.setText(element.getName());
        }

        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
        return true;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }
}
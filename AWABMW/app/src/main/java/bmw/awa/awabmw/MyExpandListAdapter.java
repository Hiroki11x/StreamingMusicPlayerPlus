package bmw.awa.awabmw;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by satsukies on 15/08/31.
 */
public class MyExpandListAdapter extends BaseExpandableListAdapter
{

    private List<Element> elements;
    private Activity activity;
    private LayoutInflater inflater;

    public MyExpandListAdapter(Activity activity, List<Element> elements)
    {
        this.activity = activity;
        this.elements = new ArrayList<>(elements);
        this.inflater = activity.getLayoutInflater();
    }

    @Override
    public Object getChild(int arg0, int arg1)
    {
        return elements.get(arg0).getChildren().get(arg1);
    }

    @Override
    public long getChildId(int arg0, int arg1)
    {
        return arg1;
    }

    @Override
    public View getChildView(int arg0, int arg1, boolean arg2, View view, ViewGroup arg4)
    {
        Element element = ((elements.get(arg0)).getChildren()).get(arg1);


        /**
         * 子アイテムの描画
         */
        view = inflater.inflate(R.layout.expandable_child, null);
        TextView textView = (TextView) view.findViewById(R.id.child_title);
        textView.setText(element.getName());

        return view;
    }

    @Override
    public int getChildrenCount(int arg0)
    {
        return elements.get(arg0).getChildren().size();
    }

    @Override
    public Object getGroup(int arg0)
    {
        return elements.get(arg0);
    }

    @Override
    public int getGroupCount()
    {
        return elements.size();
    }

    @Override
    public long getGroupId(int arg0)
    {
        return arg0;
    }

    @Override
    public View getGroupView(int arg0, boolean isExpanded, View view, ViewGroup parent)
    {
        Element element = elements.get(arg0);
        if(element.isParent())
        {
            view = inflater.inflate(R.layout.expandable_parent, null);
            TextView textView = (TextView) view.findViewById(R.id.parent_title);
            textView.setText(element.getName());
            ImageView indicator = (ImageView) view.findViewById(R.id.parent_indicator);
            indicator.setImageResource(isExpanded ? R.drawable.ic_launcher : android.R.drawable.ic_media_play);
        }
        else
        {
            /**
             * 子アイテムの描画
             */
            view = inflater.inflate(R.layout.expandable_child, null);
            TextView textView = (TextView) view.findViewById(R.id.child_title);
            textView.setText(element.getName());
        }

        return view;
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public boolean isChildSelectable(int arg0, int arg1)
    {
        return true;
    }
}
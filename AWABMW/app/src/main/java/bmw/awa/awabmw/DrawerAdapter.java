package bmw.awa.awabmw;

import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

class DrawerAdapter extends RecyclerView.Adapter<DrawerAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        static TextView mTextView;
        static ImageView mImageView;

        public ViewHolder(View itemView, int viewType) {
            super(itemView);
            switch (viewType) {
                case R.array.type_menu:
                    // menu 用レイアウトに含まれる、各Viewを取得
                    mTextView = (TextView) itemView.findViewById(R.id.menu_text);
                    mImageView = (ImageView) itemView.findViewById(R.id.menu_icon);
                    break;
                case R.array.type_header:
                    //for header
                    mImageView = (ImageView) itemView.findViewById(R.id.header_image);
                    break;
                case R.array.type_category:
                    mTextView = (TextView) itemView.findViewById(R.id.category_text);
                    break;
                case R.array.type_transparent:
                    break;
                default:
                    break;
            }
        }
    }

    private ArrayList<HashMap<String, Object>> mDrawerMenuArr;

    public DrawerAdapter(ArrayList<HashMap<String, Object>> arrayList) {
        mDrawerMenuArr = arrayList;
    }

    @Override
    public DrawerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case R.array.type_menu:
                // menu 用のレイアウトを利用する
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_item_menu, parent, false);
                itemView.setClickable(true);
                break;

            case R.array.type_header:
                // header 用のレイアウトを利用する
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_item_header, parent, false);
                itemView.setClickable(false);
                break;

            case R.array.type_category:
                //for category
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_category, parent, false);
                itemView.setClickable(true);
                break;

            case R.array.type_transparent:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_item_transparent, parent, false);
                itemView.setClickable(false);
                break;

            default:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_item_transparent, parent, false);
                itemView.setClickable(false);
                break;
        }
        ViewHolder vh = new ViewHolder(itemView, viewType);
        return vh;
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        HashMap<String, Object> menu = mDrawerMenuArr.get(position);
        switch (holder.getItemViewType()) {
            case R.array.type_menu:
                // ViewHolder で取得したViewに表示するデータをバインド
                holder.mTextView.setText(menu.get("text").toString());
                holder.mImageView.setImageDrawable((Drawable) menu.get("icon"));
                break;
            case R.array.type_header:
                // ViewHolder で取得したViewに表示するデータをバインド
                holder.mImageView.setImageDrawable((Drawable) menu.get("icon"));
                break;
            case R.array.type_category:
                holder.mTextView.setText(menu.get("text").toString());
                break;
            case R.array.type_transparent:
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
//        return mDrawerMenuArr.length;
        return mDrawerMenuArr.size();
    }

    public int getItemViewType(int position) {
        HashMap<String, Object> menu = mDrawerMenuArr.get(position);
        switch (menu.get("type").toString()) {
            case "menu":
                return R.array.type_menu;
            case "header":
                return R.array.type_header;
            case "category":
                return R.array.type_category;
            case "transparent":
                return R.array.type_transparent;
            default:
                return R.array.type_transparent;
        }
    }


}

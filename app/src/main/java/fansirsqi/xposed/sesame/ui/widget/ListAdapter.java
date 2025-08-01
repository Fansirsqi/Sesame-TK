package fansirsqi.xposed.sesame.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.entity.MapperEntity;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.util.Log;

public class ListAdapter extends BaseAdapter {
    private static final String TAG = "ListAdapter";
    @SuppressLint("StaticFieldLeak")
    private static ListAdapter adapter;
    private static ListDialog.ListType listType;
    private final Context context;  // 将 context 声明为 final
    private List<? extends MapperEntity> list;
    private SelectModelFieldFunc selectModelFieldFunc;
    private int findIndex = -1;
    private String findWord = null;
    public static List<ViewHolder> viewHolderList = new ArrayList<>();

    public static ListAdapter get(Context c) {
        if (adapter == null) {
            adapter = new ListAdapter(c.getApplicationContext());  // 使用 ApplicationContext
        }
        return adapter;
    }

    public static ListAdapter getClear(Context c) {
        ListAdapter adapter = get(c);
        adapter.resetFindState();
        return adapter;
    }

    public static ListAdapter getClear(Context c, ListDialog.ListType listType) {
        ListAdapter adapter = get(c);
        ListAdapter.listType = listType;
        adapter.resetFindState();
        return adapter;
    }

    private ListAdapter(Context c) {
        this.context = c;  // 使用传入的上下文
    }

    public void setBaseList(List<? extends MapperEntity> l) {
        if (l != list) {
            exitFind();
        }
        this.list = l;
    }

    public void setSelectedList(SelectModelFieldFunc selectModelFieldFunc) {
        this.selectModelFieldFunc = selectModelFieldFunc;
        try {
            Collections.sort(list, (o1, o2) -> {
                boolean contains1 = selectModelFieldFunc.contains(o1.id);
                boolean contains2 = selectModelFieldFunc.contains(o2.id);
                if (contains1 == contains2) {
                    return o1.compareTo(o2);
                }
                return contains1 ? -1 : 1;
            });
        } catch (Exception e) {
            Log.runtime(TAG,"ListAdapter error");
            Log.printStackTrace(e);
        }
    }

    public int findLast(String findThis) {
        return findItem(findThis, false);
    }

    public int findNext(String findThis) {
        return findItem(findThis, true);
    }

    private int findItem(String findThis, boolean forward) {
        if (list == null || list.isEmpty()) {
            return -1;
        }
        findThis = findThis.toLowerCase();
        if (!Objects.equals(findThis, findWord)) {
            resetFindState();
            findWord = findThis;
        }
        int current = Math.max(findIndex, 0);
        int size = list.size();
        int start = current;
        do {
            current = (forward) ? (current + 1) % size : (current - 1 + size) % size;
            if (list.get(current).name.toLowerCase().contains(findThis)) {
                findIndex = current;
                notifyDataSetChanged();
                return findIndex;
            }
        } while (current != start);
        return -1;
    }

    public void resetFindState() {
        findIndex = -1;
        findWord = null;
    }

    public void exitFind() {
        resetFindState();
    }

    public void selectAll() {
        selectModelFieldFunc.clear();
        for (MapperEntity item : list) {
            selectModelFieldFunc.add(item.id, 0);
        }
        notifyDataSetChanged();
    }

    public void SelectInvert() {
        for (MapperEntity item : list) {
            if (!selectModelFieldFunc.contains(item.id)) {
                selectModelFieldFunc.add(item.id, 0);
            } else {
                selectModelFieldFunc.remove(item.id);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list != null ? list.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = View.inflate(context, R.layout.list_item, null);
            vh.tv = convertView.findViewById(R.id.tv_idn);
            vh.cb = convertView.findViewById(R.id.cb_list);
            if (listType == ListDialog.ListType.SHOW) {
                vh.cb.setVisibility(View.GONE);
            }
            convertView.setTag(vh);
            viewHolderList.add(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }
        MapperEntity item = list.get(position);
        vh.tv.setText(item.name);
        int textColorPrimary = ContextCompat.getColor(context, R.color.textColorPrimary);
        vh.tv.setTextColor(findIndex == position ? Color.RED : textColorPrimary);
        vh.cb.setChecked(selectModelFieldFunc != null && selectModelFieldFunc.contains(item.id));
        return convertView;
    }

    /**
     * 内部 ViewHolder 类，用于缓存列表项视图。
     */
    public static class ViewHolder {
        TextView tv;
        CheckBox cb;
    }
}

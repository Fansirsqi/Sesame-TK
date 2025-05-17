package fansirsqi.xposed.sesame.ui;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.entity.MapperEntity;
import fansirsqi.xposed.sesame.util.Log;
import java.util.*;
public class ListAdapter extends BaseAdapter {
    @SuppressLint("StaticFieldLeak")
    private static ListAdapter adapter;
    private static ListDialog.ListType listType;
    private final Context context;  // 将 context 声明为 final
    private List<? extends MapperEntity> list;
    private SelectModelFieldFunc selectModelFieldFunc;
    private int findIndex = -1;
    private String findWord = null;
    public static List<ViewHolder> viewHolderList = new ArrayList<>();
    /**
     * 获取适配器的单例实例。
     *
     * @param c 上下文，建议传入 ApplicationContext 来避免内存泄漏
     * @return 单例 ListAdapter 实例
     */
    public static ListAdapter get(Context c) {
        if (adapter == null) {
            adapter = new ListAdapter(c.getApplicationContext());  // 使用 ApplicationContext
        }
        return adapter;
    }
    /**
     * 获取并重置适配器实例，清空查找相关的状态。
     *
     * @param c 上下文
     * @return 重置后的适配器实例
     */
    public static ListAdapter getClear(Context c) {
        ListAdapter adapter = get(c);
        adapter.resetFindState();
        return adapter;
    }
    /**
     * 获取并重置适配器实例，设置列表类型并清空查找相关状态。
     *
     * @param c        上下文
     * @param listType 列表类型
     * @return 重置后的适配器实例
     */
    public static ListAdapter getClear(Context c, ListDialog.ListType listType) {
        ListAdapter adapter = get(c);
        ListAdapter.listType = listType;
        adapter.resetFindState();
        return adapter;
    }
    /**
     * 构造函数，初始化上下文。
     *
     * @param c 上下文
     */
    private ListAdapter(Context c) {
        this.context = c;  // 使用传入的上下文
    }
    /**
     * 设置基本列表数据。
     *
     * @param l 列表数据
     */
    public void setBaseList(List<? extends MapperEntity> l) {
        if (l != list) {
            exitFind();
        }
        this.list = l;
    }
    /**
     * 设置选中的列表项，并按选中状态排序。
     *
     * @param selectModelFieldFunc 选择功能实现
     */
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
            Log.runtime("ListAdapter error");
            Log.printStackTrace(e);
        }
    }
    /**
     * 查找上一个匹配项。
     *
     * @param findThis 要查找的字符串
     * @return 查找到的索引
     */
    public int findLast(String findThis) {
        return findItem(findThis, false);
    }
    /**
     * 查找下一个匹配项。
     *
     * @param findThis 要查找的字符串
     * @return 查找到的索引
     */
    public int findNext(String findThis) {
        return findItem(findThis, true);
    }
    /**
     * 查找列表中的匹配项。
     *
     * @param findThis 查找的字符串
     * @param forward  是否向前查找
     * @return 匹配项的索引
     */
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
    /**
     * 重置查找状态。
     */
    public void resetFindState() {
        findIndex = -1;
        findWord = null;
    }
    /**
     * 退出查找模式。
     */
    public void exitFind() {
        resetFindState();
    }
    /**
     * 全选列表中的所有项。
     */
    public void selectAll() {
        selectModelFieldFunc.clear();
        for (MapperEntity item : list) {
            selectModelFieldFunc.add(item.id, 0);
        }
        notifyDataSetChanged();
    }
    /**
     * 反选列表中的所有项。
     */
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
        vh.tv.setTextColor(findIndex == position ? Color.RED : Color.BLACK);
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

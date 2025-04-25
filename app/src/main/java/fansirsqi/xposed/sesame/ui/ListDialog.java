package fansirsqi.xposed.sesame.ui;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import org.json.JSONException;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountOneModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectOneModelField;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.entity.AreaCode;
import fansirsqi.xposed.sesame.entity.CooperateEntity;
import fansirsqi.xposed.sesame.entity.MapperEntity;
import fansirsqi.xposed.sesame.util.Maps.CooperateMap;
import java.util.List;
public class ListDialog {
    static AlertDialog listDialog;
    @SuppressLint("StaticFieldLeak")
    static Button btn_find_last, btn_find_next,
            btn_select_all, btn_select_invert;
    @SuppressLint("StaticFieldLeak")
    static EditText searchText;
    @SuppressLint("StaticFieldLeak")
    static ListView lv_list;
    private static SelectModelFieldFunc selectModelFieldFunc;
    static Boolean hasCount;
    static ListType listType;
    @SuppressLint("StaticFieldLeak")
    static RelativeLayout layout_batch_process;
    public enum ListType {
        RADIO, CHECK, SHOW
    }
    public static void show(Context c, CharSequence title, SelectOneModelField selectModelField, ListType listType) {
        show(c, title, selectModelField.getExpandValue(), selectModelField, false, listType);
    }
    public static void show(Context c, CharSequence title, SelectAndCountOneModelField selectModelField, ListType listType) {
        show(c, title, selectModelField.getExpandValue(), selectModelField, false, listType);
    }
    public static void show(Context c, CharSequence title, SelectModelField selectModelField) throws JSONException {
        show(c, title, selectModelField, ListDialog.ListType.CHECK);
    }
    public static void show(Context c, CharSequence title, SelectAndCountModelField selectModelField) {
        show(c, title, selectModelField, ListDialog.ListType.CHECK);
    }
    public static void show(Context c, CharSequence title, SelectModelField selectModelField, ListType listType) throws JSONException {
        show(c, title, selectModelField.getExpandValue(), selectModelField, false, listType);
    }
    public static void show(Context c, CharSequence title, SelectAndCountModelField selectModelField, ListType listType) {
        show(c, title, selectModelField.getExpandValue(), selectModelField, true, listType);
    }
    public static void show(Context c, CharSequence title, List<? extends MapperEntity> bl, SelectModelFieldFunc selectModelFieldFunc, Boolean hasCount) {
        show(c, title, bl, selectModelFieldFunc, hasCount, ListType.CHECK);
    }
    public static void show(Context c, CharSequence title, List<? extends MapperEntity> bl, SelectModelFieldFunc selectModelFieldFunc, Boolean hasCount, ListType listType) {
        ListDialog.selectModelFieldFunc = selectModelFieldFunc;
        ListDialog.hasCount = hasCount;
        ListAdapter la = ListAdapter.getClear(c, listType);
        la.setBaseList(bl);
        la.setSelectedList(selectModelFieldFunc);
        showListDialog(c, title);
        ListDialog.listType = listType;
    }
    private static void showListDialog(Context c, CharSequence title) {
        if (listDialog == null || listDialog.getContext() != c)
            listDialog = new AlertDialog.Builder(c)
                    .setTitle(title)
                    .setView(getListView(c))
                    .setPositiveButton(c.getString(R.string.close), null)
                    .create();
        listDialog.setOnShowListener(p1 -> {
            AlertDialog d = (AlertDialog) p1;
            layout_batch_process = d.findViewById(R.id.layout_batch_process);
            assert layout_batch_process != null;
            layout_batch_process.setVisibility(listType == ListType.CHECK && !hasCount ? View.VISIBLE : View.GONE);
            ListAdapter.get(c).notifyDataSetChanged();
        });
        listDialog.show();
        Button positiveButton = listDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(c, R.color.button));
        }
    }
    private static View getListView(Context c) {
        @SuppressLint("InflateParams") View v = LayoutInflater.from(c).inflate(R.layout.dialog_list, null);
        btn_find_last = v.findViewById(R.id.btn_find_last);
        btn_find_next = v.findViewById(R.id.btn_find_next);
        btn_select_all = v.findViewById(R.id.btn_select_all);
        btn_select_invert = v.findViewById(R.id.btn_select_invert);
        View.OnClickListener onBtnClickListener = new View.OnClickListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onClick(View v) {
                if (searchText.length() <= 0) {
                    return;
                }
                ListAdapter la = ListAdapter.get(v.getContext());
                int index = -1;
                int id = v.getId();
                if (id == R.id.btn_find_last) {
                    index = la.findLast(searchText.getText().toString());
                } else if (id == R.id.btn_find_next) {
                    index = la.findNext(searchText.getText().toString());
                }
                if (index < 0) {
                    Toast.makeText(v.getContext(), "未搜到", Toast.LENGTH_SHORT).show();
                } else {
                    lv_list.setSelection(index);
                }
            }
        };
        btn_find_last.setOnClickListener(onBtnClickListener);
        btn_find_next.setOnClickListener(onBtnClickListener);
        @SuppressLint("NonConstantResourceId") View.OnClickListener batchBtnOnClickListener = v1 -> {
            ListAdapter la = ListAdapter.get(v1.getContext());
            int id = v1.getId();
            if (id == R.id.btn_select_all) {
                la.selectAll();
            } else if (id == R.id.btn_select_invert) {
                la.SelectInvert();
            }
        };
        btn_select_all.setOnClickListener(batchBtnOnClickListener);
        btn_select_invert.setOnClickListener(batchBtnOnClickListener);
        searchText = v.findViewById(R.id.edt_find);
        lv_list = v.findViewById(R.id.lv_list);
        lv_list.setAdapter(ListAdapter.getClear(c));
        lv_list.setOnItemClickListener(
                (p1, p2, p3, p4) -> {
                    if (listType == ListType.SHOW) {
                        return;
                    }
                    MapperEntity curIdAndName = (MapperEntity) p1.getAdapter().getItem(p3);
                    ListAdapter.ViewHolder curViewHolder = (ListAdapter.ViewHolder) p2.getTag();
                    if (!hasCount) {
                        if (listType == ListType.RADIO) {
                            selectModelFieldFunc.clear();
                            if (curViewHolder.cb.isChecked()) {
                                curViewHolder.cb.setChecked(false);
                            } else {
                                for (int i = 0; i < ListAdapter.viewHolderList.size(); i++) {
                                    ListAdapter.ViewHolder viewHolder = ListAdapter.viewHolderList.get(i);
                                    viewHolder.cb.setChecked(false);
                                }
                                curViewHolder.cb.setChecked(true);
                                selectModelFieldFunc.add(curIdAndName.id, 0);
                            }
                        } else {
                            if (curViewHolder.cb.isChecked()) {
                                selectModelFieldFunc.remove(curIdAndName.id);
                                curViewHolder.cb.setChecked(false);
                            } else {
                                if (!selectModelFieldFunc.contains(curIdAndName.id)) {
                                    selectModelFieldFunc.add(curIdAndName.id, 0);
                                }
                                curViewHolder.cb.setChecked(true);
                            }
                        }
                    } else {
                        EditText edt_count = new EditText(c);
                        AlertDialog edtDialog = new AlertDialog.Builder(c)
                                .setTitle(curIdAndName.name)
                                .setView(edt_count)
                                .setPositiveButton(c.getString(R.string.ok), (dialog, which) -> {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        int count = 0;
                                        if (edt_count.length() > 0)
                                            try {
                                                count = Integer.parseInt(edt_count.getText().toString());
                                            } catch (Throwable t) {
                                                return;
                                            }
                                        Integer value = selectModelFieldFunc.get(curIdAndName.id);
                                        if (count > 0) {
                                            selectModelFieldFunc.add(curIdAndName.id, count);
                                            curViewHolder.cb.setChecked(true);
                                        } else {
                                            if (value != null && value >= 0) {
                                                selectModelFieldFunc.remove(curIdAndName.id);
                                            }
                                            curViewHolder.cb.setChecked(false);
                                        }
                                    }
                                    ListAdapter.get(c).notifyDataSetChanged();
                                })
                                .setNegativeButton(c.getString(R.string.cancel), null)
                                .create();
                        if (curIdAndName instanceof CooperateEntity)
                            edt_count.setHint("浇水克数");
                        else
                            edt_count.setHint("次数");
                        Integer value = selectModelFieldFunc.get(curIdAndName.id);
                        if (value != null && value >= 0)
                            edt_count.setText(String.valueOf(value));
                        else
                            edt_count.getText().clear();
                        edtDialog.show();
                    }
                });
        lv_list.setOnItemLongClickListener(
                (p1, p2, p3, p4) -> {
                    MapperEntity curIdAndName = (MapperEntity) p1.getAdapter().getItem(p3);
                    if (curIdAndName instanceof CooperateEntity) {
                        try {
                            new AlertDialog.Builder(c)
                                    .setTitle("删除 " + curIdAndName.name)
                                    .setPositiveButton(c.getString(R.string.ok), (dialog, which) -> {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            CooperateMap.getInstance(CooperateMap.class).remove(curIdAndName.id);
                                            selectModelFieldFunc.remove(curIdAndName.id);
                                            ListAdapter.get(c).exitFind();
                                        }
                                        ListAdapter.get(c).notifyDataSetChanged();
                                    })
                                    .setNegativeButton(c.getString(R.string.cancel), null)
                                    .create().show();
                        } catch (Throwable ignored) {
                        }
                    } else if (!(curIdAndName instanceof AreaCode)) {
                        new AlertDialog.Builder(c)
                                .setTitle("选项")
                                .setAdapter(
                                        OptionsAdapter.get(c), new OnClickListener() {
                                            Context c;
                                            public OnClickListener setContext(Context c) {
                                                this.c = c;
                                                return this;
                                            }
                                            @Override
                                            public void onClick(DialogInterface p1, int p2) {
                                                String url = null;
                                                switch (p2) {
                                                    case 0:
                                                        url = "alipays://platformapi/startapp?saId=10000007&qrcode=https%3A%2F%2F60000002.h5app.alipay.com%2Fwww%2Fhome.html%3FuserId%3D";
                                                        break;
                                                    case 1:
                                                        url = "alipays://platformapi/startapp?saId=10000007&qrcode=https%3A%2F%2F66666674.h5app.alipay.com%2Fwww%2Findex.htm%3Fuid%3D";
                                                        break;
                                                    case 2:
                                                        url = "alipays://platformapi/startapp?appId=20000166&actionType=profile&userId=";
                                                        break;
                                                    case 3:
                                                        try {
                                                            new AlertDialog.Builder(c)
                                                                    .setTitle("删除 " + curIdAndName.name)
                                                                    .setPositiveButton(c.getString(R.string.ok), (dialog, which) -> {
                                                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                                                            selectModelFieldFunc.remove(curIdAndName.id);
                                                                            ListAdapter.get(c).exitFind();
                                                                        }
                                                                        ListAdapter.get(c).notifyDataSetChanged();
                                                                    })
                                                                    .setNegativeButton(c.getString(R.string.cancel), null)
                                                                    .create().show();
                                                        } catch (Throwable ignored) {
                                                        }
                                                }
                                                if (url != null) {
                                                    Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url + curIdAndName.id));
                                                    c.startActivity(it);
                                                }
                                            }
                                        }.setContext(c))
                                .setNegativeButton(c.getString(R.string.cancel), null)
                                .create().show();
                    }
                    return true;
                });
        return v;
    }
}

package fansirsqi.xposed.sesame.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;

import java.util.List;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.entity.AreaCode;
import fansirsqi.xposed.sesame.entity.CooperateEntity;
import fansirsqi.xposed.sesame.entity.MapperEntity;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountOneModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectOneModelField;
import fansirsqi.xposed.sesame.ui.OptionsAdapter;
import fansirsqi.xposed.sesame.util.maps.CooperateMap;

public class ListDialog {
    static androidx.appcompat.app.AlertDialog listDialog;
    @SuppressLint("StaticFieldLeak")
    static Button btn_find_last, btn_find_next, btn_select_all, btn_select_invert;
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
        show(c, title, selectModelField, ListType.CHECK);
    }

    public static void show(Context c, CharSequence title, SelectAndCountModelField selectModelField) {
        show(c, title, selectModelField, ListType.CHECK);
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
        fansirsqi.xposed.sesame.ui.widget.ListAdapter la = ListAdapter.getClear(c, listType);
        la.setBaseList(bl);
        la.setSelectedList(selectModelFieldFunc);
        showListDialog(c, title);
        ListDialog.listType = listType;
    }

    private static void showListDialog(Context c, CharSequence title) {
        if (listDialog == null || listDialog.getContext() != c)
            listDialog = new MaterialAlertDialogBuilder(c)
                    .setTitle(title)
                    .setView(getListView(c))
                    .setPositiveButton(c.getString(R.string.close), null)
                    .create();
        listDialog.setOnShowListener(p1 -> {
            androidx.appcompat.app.AlertDialog d = (androidx.appcompat.app.AlertDialog) p1;
            layout_batch_process = d.findViewById(R.id.layout_batch_process);
            assert layout_batch_process != null;
            layout_batch_process.setVisibility(listType == ListType.CHECK && !hasCount ? View.VISIBLE : View.GONE);
            ListAdapter.get(c).notifyDataSetChanged();
        });
        listDialog.show();
        Button positiveButton = listDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(c, R.color.selection_color));
        }
    }

    private static View getListView(Context c) {
        @SuppressLint("InflateParams") View v = LayoutInflater.from(c).inflate(R.layout.dialog_list, null);
        btn_find_last = v.findViewById(R.id.btn_find_last);
        btn_find_next = v.findViewById(R.id.btn_find_next);
        btn_select_all = v.findViewById(R.id.btn_select_all);
        btn_select_invert = v.findViewById(R.id.btn_select_invert);
        View.OnClickListener onBtnClickListener = v1 -> {
            if (searchText.length() <= 0) return;
            ListAdapter la = ListAdapter.get(v1.getContext());
            int index = -1;
            if (v1.getId() == R.id.btn_find_last) index = la.findLast(searchText.getText().toString());
            else if (v1.getId() == R.id.btn_find_next) index = la.findNext(searchText.getText().toString());
            if (index < 0) Toast.makeText(v1.getContext(), "未搜到", Toast.LENGTH_SHORT).show();
            else lv_list.setSelection(index);
        };
        btn_find_last.setOnClickListener(onBtnClickListener);
        btn_find_next.setOnClickListener(onBtnClickListener);

        View.OnClickListener batchBtnOnClickListener = v1 -> {
            ListAdapter la = ListAdapter.get(v1.getContext());
            if (v1.getId() == R.id.btn_select_all) la.selectAll();
            else if (v1.getId() == R.id.btn_select_invert) la.SelectInvert();
        };
        btn_select_all.setOnClickListener(batchBtnOnClickListener);
        btn_select_invert.setOnClickListener(batchBtnOnClickListener);

        searchText = v.findViewById(R.id.edt_find);
        lv_list = v.findViewById(R.id.lv_list);
        lv_list.setAdapter(ListAdapter.getClear(c));

        lv_list.setOnItemClickListener((p1, p2, p3, p4) -> {
            if (listType == ListType.SHOW) return;
            MapperEntity cur = (MapperEntity) p1.getAdapter().getItem(p3);
            ListAdapter.ViewHolder holder = (ListAdapter.ViewHolder) p2.getTag();
            if (!hasCount) {
                if (listType == ListType.RADIO) {
                    selectModelFieldFunc.clear();
                    if (holder.cb.isChecked()) holder.cb.setChecked(false);
                    else {
                        for (ListAdapter.ViewHolder vh : ListAdapter.viewHolderList) vh.cb.setChecked(false);
                        holder.cb.setChecked(true);
                        selectModelFieldFunc.add(cur.id, 0);
                    }
                } else {
                    if (holder.cb.isChecked()) {
                        selectModelFieldFunc.remove(cur.id);
                        holder.cb.setChecked(false);
                    } else {
                        if (!selectModelFieldFunc.contains(cur.id)) selectModelFieldFunc.add(cur.id, 0);
                        holder.cb.setChecked(true);
                    }
                }
            } else {
                EditText edt = new EditText(c);
                androidx.appcompat.app.AlertDialog edtDialog = new MaterialAlertDialogBuilder(c)
                        .setTitle(cur.name)
                        .setView(edt)
                        .setPositiveButton(c.getString(R.string.ok), (dialog, which) -> {
                            if (edt.length() > 0) {
                                try {
                                    int count = Integer.parseInt(edt.getText().toString());
                                    if (count > 0) {
                                        selectModelFieldFunc.add(cur.id, count);
                                        holder.cb.setChecked(true);
                                    } else {
                                        selectModelFieldFunc.remove(cur.id);
                                        holder.cb.setChecked(false);
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            ListAdapter.get(c).notifyDataSetChanged();
                        })
                        .setNegativeButton(c.getString(R.string.cancel), null)
                        .create();
                edt.setHint((cur instanceof CooperateEntity) ? "浇水克数" : "次数");
                Integer value = selectModelFieldFunc.get(cur.id);
                if (value != null && value >= 0) edt.setText(String.valueOf(value));
                edtDialog.show();
            }
        });

        lv_list.setOnItemLongClickListener((p1, p2, p3, p4) -> {
            MapperEntity cur = (MapperEntity) p1.getAdapter().getItem(p3);
            if (cur instanceof CooperateEntity) {
                new MaterialAlertDialogBuilder(c)
                        .setTitle("删除 " + cur.name)
                        .setPositiveButton(c.getString(R.string.ok), (dialog, which) -> {
                            CooperateMap.getInstance(CooperateMap.class).remove(cur.id);
                            selectModelFieldFunc.remove(cur.id);
                            ListAdapter.get(c).exitFind();
                            ListAdapter.get(c).notifyDataSetChanged();
                        })
                        .setNegativeButton(c.getString(R.string.cancel), null)
                        .show();
            } else if (!(cur instanceof AreaCode)) {
                new MaterialAlertDialogBuilder(c)
                        .setTitle("选项")
                        .setAdapter(OptionsAdapter.get(c), (dialog, which) -> {
                            String url = null;
                            switch (which) {
                                case 0: url = "alipays://platformapi/startapp?saId=10000007&qrcode=https%3A%2F%2F60000002.h5app.alipay.com%2Fwww%2Fhome.html%3FuserId%3D"; break;
                                case 1: url = "alipays://platformapi/startapp?saId=10000007&qrcode=https%3A%2F%2F66666674.h5app.alipay.com%2Fwww%2Findex.htm%3Fuid%3D"; break;
                                case 2: url = "alipays://platformapi/startapp?appId=20000166&actionType=profile&userId="; break;
                                case 3:
                                    new MaterialAlertDialogBuilder(c)
                                            .setTitle("删除 " + cur.name)
                                            .setPositiveButton(c.getString(R.string.ok), (d2, w2) -> {
                                                selectModelFieldFunc.remove(cur.id);
                                                ListAdapter.get(c).exitFind();
                                                ListAdapter.get(c).notifyDataSetChanged();
                                            })
                                            .setNegativeButton(c.getString(R.string.cancel), null)
                                            .show();
                                    break;
                            }
                            if (url != null) {
                                Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url + cur.id));
                                c.startActivity(it);
                            }
                        })
                        .setNegativeButton(c.getString(R.string.cancel), null)
                        .show();
            }
            return true;
        });

        return v;
    }
}

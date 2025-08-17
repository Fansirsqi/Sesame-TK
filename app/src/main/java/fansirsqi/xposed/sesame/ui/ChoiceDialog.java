package fansirsqi.xposed.sesame.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.core.content.ContextCompat;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.PriorityModelField;

public class ChoiceDialog {

    /**
     * 显示单选对话框（Material3 风格）
     *
     * @param context          当前上下文，用于构建对话框
     * @param title            对话框的标题
     * @param choiceModelField 包含选项数据的 ChoiceModelField 对象
     */
    public static void show(Context context, CharSequence title, ChoiceModelField choiceModelField) {
        // 使用 Material3 对话框构造器
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setSingleChoiceItems(choiceModelField.getExpandKey(), choiceModelField.getValue(), (p1, p2) -> choiceModelField.setObjectValue(p2))
                .setPositiveButton(context.getString(R.string.ok), null)
                .create();

        // 设置确认按钮颜色
        dialog.setOnShowListener((DialogInterface dialogInterface) -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(context, R.color.selection_color));
            }
        });
        dialog.show();
    }

    public static void show(Context context, CharSequence title, PriorityModelField choiceModelField) {
        // 使用 Material3 对话框构造器
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setSingleChoiceItems(choiceModelField.getExpandKey(), choiceModelField.getValue(), (p1, p2) -> choiceModelField.setObjectValue(p2))
                .setPositiveButton(context.getString(R.string.ok), null)
                .create();

        // 设置确认按钮颜色
        dialog.setOnShowListener((DialogInterface dialogInterface) -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(context, R.color.selection_color));
            }
        });
        dialog.show();
    }

}

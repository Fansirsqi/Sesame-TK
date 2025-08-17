package fansirsqi.xposed.sesame.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.util.Log;

/**
 * 字符串对话框工具类。
 * 提供了显示编辑对话框和读取对话框的静态方法。
 */
public class StringDialog {
    private static ModelField<?> modelField;

    public static void showEditDialog(Context c, CharSequence title, ModelField<?> modelField) {
        showEditDialog(c, title, modelField, null);
    }

    public static void showEditDialog(Context c, CharSequence title, ModelField<?> modelField, String msg) {
        StringDialog.modelField = modelField;
        AlertDialog editDialog = getEditDialog(c);
        if (msg != null) {
            editDialog.setMessage(msg);
        }
        editDialog.setTitle(title);
        editDialog.show();
    }

    private static AlertDialog getEditDialog(Context c) {
        EditText edt = new EditText(c);
        AlertDialog editDialog = new MaterialAlertDialogBuilder(c)
                .setTitle("title")
                .setView(edt)
                .setPositiveButton(
                        c.getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            Context context;

                            public DialogInterface.OnClickListener setData(Context c) {
                                context = c;
                                return this;
                            }

                            public void onClick(DialogInterface p1, int p2) {
                                try {
                                    Editable text = edt.getText();
                                    if (text == null || text.toString().isEmpty()) {
                                        modelField.setConfigValue(null);
                                    } else {
                                        modelField.setConfigValue(text.toString());
                                    }
                                } catch (Throwable e) {
                                    Log.printStackTrace(e);
                                }
                            }
                        }.setData(c))
                .setNegativeButton(c.getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .create();

        editDialog.setOnShowListener(dialog -> {
            Button positiveButton = editDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(c, R.color.selection_color));
            }
        });

        edt.setText(String.valueOf(modelField.getConfigValue()));
        return editDialog;
    }

    public static void showReadDialog(Context c, CharSequence title, ModelField<?> modelField) {
        showReadDialog(c, title, modelField, null);
    }

    public static void showReadDialog(Context c, CharSequence title, ModelField<?> modelField, String msg) {
        StringDialog.modelField = modelField;
        AlertDialog readDialog = getReadDialog(c);
        if (msg != null) {
            readDialog.setMessage(msg);
        }
        readDialog.setTitle(title);
        readDialog.show();
    }

    private static AlertDialog getReadDialog(Context c) {
        EditText edt = new EditText(c);
        edt.setInputType(InputType.TYPE_NULL);
        edt.setTextColor(Color.GRAY);
        edt.setText(String.valueOf(modelField.getConfigValue()));
        return new MaterialAlertDialogBuilder(c)
                .setTitle("读取")
                .setView(edt)
                .setPositiveButton(c.getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                .create();
    }

    public static void showAlertDialog(Context c, String title, String msg) {
        showAlertDialog(c, title, msg, "确定");
    }

    public static void showAlertDialog(Context c, String title, String msg, String positiveButton) {
        CharSequence parsedMsg = HtmlCompat.fromHtml(msg, HtmlCompat.FROM_HTML_MODE_LEGACY);

        AlertDialog alertDialog = new MaterialAlertDialogBuilder(c)
                .setTitle(title)
                .setMessage(parsedMsg)
                .setPositiveButton(positiveButton, (dialog, which) -> dialog.dismiss())
                .create();

        alertDialog.show();

        Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(ContextCompat.getColor(c, R.color.textColorPrimary));
        }
    }

    public static AlertDialog showSelectionDialog(Context c, String title, CharSequence[] items,
                                                  DialogInterface.OnClickListener onItemClick,
                                                  String positiveButton, DialogInterface.OnDismissListener onDismiss) {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(c)
                .setTitle(title)
                .setItems(items, onItemClick)
                .setOnDismissListener(onDismiss)
                .setPositiveButton(positiveButton, (dialog, which) -> dialog.dismiss())
                .create();

        alertDialog.show();

        Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(ContextCompat.getColor(c, R.color.selection_color));
        }

        return alertDialog;
    }
}

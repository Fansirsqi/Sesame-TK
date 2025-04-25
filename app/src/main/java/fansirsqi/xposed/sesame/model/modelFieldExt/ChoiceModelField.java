package fansirsqi.xposed.sesame.model.modelFieldExt;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.ui.ChoiceDialog;
public class ChoiceModelField extends ModelField<Integer> {
    private String[] choiceArray;

    public ChoiceModelField(String code, String name, Integer value) {
        super(code, name, value);
    }
    public ChoiceModelField(String code, String name, Integer value, String[] choiceArray) {
        super(code, name, value);
        this.choiceArray = choiceArray;
    }

    public ChoiceModelField(String code, String name, Integer value,String desc) {
        super(code, name, value, desc);
    }
    public ChoiceModelField(String code, String name, Integer value, String[] choiceArray,String desc) {
        super(code, name, value, desc);
        this.choiceArray = choiceArray;
    }

    @Override
    public String getType() {
        return "CHOICE";
    }
    public String[] getExpandKey() {
        return choiceArray;
    }
    @Override
    public View getView(Context context) {
        Button btn = new Button(context);
        btn.setText(getName());
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setTextColor(ContextCompat.getColor(context, R.color.button));
        btn.setBackground(ContextCompat.getDrawable(context, R.drawable.button));
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setMinHeight(150);
        btn.setMaxHeight(180);
        btn.setPaddingRelative(40, 0, 40, 0);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> ChoiceDialog.show(v.getContext(), ((Button) v).getText(), this));
        return btn;
    }
}

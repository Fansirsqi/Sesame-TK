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
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.entity.MapperEntity;
import fansirsqi.xposed.sesame.ui.widget.ListDialog;
import java.util.List;
import java.util.Objects;
public class SelectOneModelField extends ModelField<String> implements SelectModelFieldFunc {
    private SelectListFunc selectListFunc;
    private List<? extends MapperEntity> expandValue;
    public SelectOneModelField(String code, String name, String value, List<? extends MapperEntity> expandValue) {
        super(code, name, value);
        this.expandValue = expandValue;
    }
    public SelectOneModelField(String code, String name, String value, SelectListFunc selectListFunc) {
        super(code, name, value);
        this.selectListFunc = selectListFunc;
    }
    @Override
    public String getType() {
        return "SELECT_ONE";
    }
    public List<? extends MapperEntity> getExpandValue() {
        return selectListFunc == null ? expandValue : selectListFunc.getList();
    }
    @Override
    public View getView(Context context) {
        Button btn = new Button(context);
        btn.setText(getName());
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color));
        btn.setBackground(ContextCompat.getDrawable(context, R.drawable.dialog_list_button));
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setMinHeight(150);
        btn.setPaddingRelative(40, 0, 40, 0);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> ListDialog.show(v.getContext(), ((Button) v).getText(), this, ListDialog.ListType.RADIO));
        return btn;
    }
    @Override
    public void clear() {
        value = defaultValue;
    }
    @Override
    public Integer get(String id) {
        return 0;
    }
    @Override
    public void add(String id, Integer count) {
        value = id;
    }
    @Override
    public void remove(String id) {
        if (Objects.equals(value, id)) {
            value = defaultValue;
        }
    }
    @Override
    public Boolean contains(String id) {
        return Objects.equals(value, id);
    }
    public interface SelectListFunc {
        List<? extends MapperEntity> getList();
    }
}
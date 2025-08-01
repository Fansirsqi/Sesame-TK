package fansirsqi.xposed.sesame.model.modelFieldExt;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import org.json.JSONException;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.entity.MapperEntity;
import fansirsqi.xposed.sesame.ui.widget.ListDialog;
import java.util.List;
import java.util.Set;
/**
 * 数据结构说明
 * Set<String> 表示已选择的数据
 * List<? extends IdAndName> 需要选择的数据
 */
public class SelectModelField extends ModelField<Set<String>> implements SelectModelFieldFunc {
    private SelectListFunc selectListFunc;
    private List<? extends MapperEntity> expandValue;
    public SelectModelField(String code, String name, Set<String> value, List<? extends MapperEntity> expandValue) {
        super(code, name, value);
        this.expandValue = expandValue;
    }
    public SelectModelField(String code, String name, Set<String> value, SelectListFunc selectListFunc) {
        super(code, name, value);
        this.selectListFunc = selectListFunc;
    }
    public SelectModelField(String code, String name, Set<String> value, List<? extends MapperEntity> expandValue, String desc) {
        super(code, name, value, desc);
        this.expandValue = expandValue;
    }
    public SelectModelField(String code, String name, Set<String> value, SelectListFunc selectListFunc, String desc) {
        super(code, name, value, desc);
        this.selectListFunc = selectListFunc;
    }
    @Override
    public String getType() {
        return "SELECT";
    }
    public List<? extends MapperEntity> getExpandValue() throws JSONException {
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
        btn.setMaxHeight(180);
        btn.setPaddingRelative(40, 0, 40, 0);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> {
            try {
                ListDialog.show(v.getContext(), ((Button) v).getText(), this);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        return btn;
    }
    @Override
    public void clear() {
        getValue().clear();
    }
    @Override
    public Integer get(String id) {
        return 0;
    }
    @Override
    public void add(String id, Integer count) {
        getValue().add(id);
    }
    @Override
    public void remove(String id) {
        getValue().remove(id);
    }
    @Override
    public Boolean contains(String id) {
        return getValue().contains(id);
    }
    public interface SelectListFunc {
        List<? extends MapperEntity> getList() throws JSONException;
    }
}

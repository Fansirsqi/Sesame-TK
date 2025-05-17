package fansirsqi.xposed.sesame.ui;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.ViewAppInfo;
public class BaseActivity extends AppCompatActivity {
    private Toolbar toolbar;
    /**
     * 在创建活动时调用，初始化基础设置。
     *
     * @param savedInstanceState 之前保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewAppInfo.init(getApplicationContext());
    }
    /**
     * 当活动的内容视图发生变化时调用。
     * 设置工具栏并初始化标题和副标题。
     */
    @Override
    public void onContentChanged() {
        super.onContentChanged();
        // 查找并设置工具栏
        toolbar = findViewById(R.id.x_toolbar);
        toolbar.setTitle(getBaseTitle());
        toolbar.setSubtitle(getBaseSubtitle());
        // 设置工具栏为支持操作栏
        setSupportActionBar(toolbar);
    }
    public String getBaseTitle() {
        return ViewAppInfo.getAppTitle();
    }
    public String getBaseSubtitle() {
        return null;
    }
    public void setBaseTitle(String title) {
        toolbar.setTitle(title);
    }
    public void setBaseSubtitle(String subTitle) {
        toolbar.setSubtitle(subTitle);
    }
    public void setBaseTitleTextColor(int color) {
        toolbar.setTitleTextColor(color);
    }
    public void setBaseSubtitleTextColor(int color) {
        toolbar.setSubtitleTextColor(color);
    }
}

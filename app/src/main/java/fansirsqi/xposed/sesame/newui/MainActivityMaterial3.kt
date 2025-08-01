package fansirsqi.xposed.sesame.newui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import fansirsqi.xposed.sesame.util.DeviceInfoCard
import fansirsqi.xposed.sesame.util.DeviceInfoUtil
import fansirsqi.xposed.sesame.util.PreviewDeviceInfoProvider

class MainActivityMaterial3 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeviceInfoCard(DeviceInfoUtil.getDeviceInfo(this))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceInfoCardPreview(@PreviewParameter(PreviewDeviceInfoProvider::class) info: Map<String, String>) {
    DeviceInfoCard(info = info)
}

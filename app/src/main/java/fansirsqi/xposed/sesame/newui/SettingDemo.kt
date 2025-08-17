package fansirsqi.xposed.sesame.newui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fansirsqi.xposed.sesame.newutil.MMKVSettingsManager

// 定义 Friend 数据类
data class Friend(val id: String, val name: String)

class SettingDemo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MMKVSettingsManager.init(this)
        setContent {
            SettingScreen()
        }
    }
}

@Composable
fun SettingScreen() {
    // 初始好友列表状态
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }

    // 恢复数据
    LaunchedEffect(Unit) {
        friends = MMKVSettingsManager.getObject("friends", emptyList())
    }

    // 输入框状态
    var inputText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("当前好友列表：", style = MaterialTheme.typography.titleMedium)

        if (friends.isEmpty()) {
            Text("（暂无好友）", style = MaterialTheme.typography.bodyMedium)
        } else {
            friends.forEach {
                Text("- ${it.id}: ${it.name}", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(24.dp))

        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("添加新好友（格式: id,name）") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val parts = inputText.split(",", limit = 2).map { it.trim() }
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                val newFriend = Friend(parts[0], parts[1])
                friends = friends + newFriend
                MMKVSettingsManager.putObject("friends", friends)
                inputText = ""
            } else {
                // 你可以替换成 Toast 等提示
                println("⚠️ 输入格式错误，应为 id,name")
            }
        }) {
            Text("保存新好友")
        }
    }
}

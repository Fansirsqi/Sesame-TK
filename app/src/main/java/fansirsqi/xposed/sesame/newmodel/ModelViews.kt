package fansirsqi.xposed.sesame.newmodel

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fansirsqi.xposed.sesame.model.ModelField

@Composable
fun BooleanFieldView(field: ModelField<Boolean>) {
    // 显式指定类型，并提供默认值
    var isChecked by remember {
        mutableStateOf<Boolean>(field.value as? Boolean ?: false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 确保 name 字段存在且可访问
        Text(
            text = field.name ?: "Unnamed Field", // 提供默认值以防 name 为 null
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
                field.setObjectValue(it) // 更新字段值
            }
        )
    }
}
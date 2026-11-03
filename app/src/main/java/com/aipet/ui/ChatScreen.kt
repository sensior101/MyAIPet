package com.aipet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipet.ai.ChatMessage

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    val busy by vm.busy.collectAsState()
    var input by remember { mutableStateOf("") }

    // safeDrawingPadding: 시스템바 + 키보드(IME) 영역을 모두 피함
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                val mine = msg.role == ChatMessage.Role.USER
                Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) { Text(msg.text, Modifier.padding(10.dp)) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("펫에게 말 걸기") })
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.send(input); input = "" }, enabled = !busy) { Text("전송") }
        }
    }
}

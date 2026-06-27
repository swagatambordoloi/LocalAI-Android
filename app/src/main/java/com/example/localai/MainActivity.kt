package com.example.localai

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.room.Room
import kotlinx.coroutines.launch
import com.example.localai.data.AppDatabase
import com.example.localai.data.LiteRTRepository
import com.example.localai.ui.MainViewModel
import com.example.localai.ui.theme.LocalAITheme // Ensure this import matches your project

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "chat-db").build()
        val repository = LiteRTRepository(applicationContext, db.chatDao())
        val viewModel = MainViewModel(repository, application)

        setContent {
            LocalAITheme { // Wrap the whole app in your Theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val chatHistory by viewModel.chatHistory.collectAsState(initial = emptyList())

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Recent Chats", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                LazyColumn {
                    items(chatHistory) { message ->
                        if (message.role == "user") {
                            NavigationDrawerItem(
                                label = { Text(message.text, maxLines = 1) },
                                selected = false,
                                onClick = { /* Handle session click */ }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Local AI") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            ChatInterface(viewModel, Modifier.padding(padding))
        }
    }
}

@Composable
fun ModelStatusChip(isReady: Boolean) {
    AssistChip(
        onClick = { },
        label = { Text(if (isReady) "Model Ready" else "Model Not Loaded") },
        leadingIcon = {
            Icon(
                imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isReady) Color.Green else Color.Red
            )
        }
    )
}
@Composable
fun ChatInterface(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState(initial = emptyList())
    var inputQuery by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.importModel(it) } }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { filePickerLauncher.launch("application/octet-stream") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Import Custom Model") }

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            items(chatHistory) { message ->
                Text(
                    text = "${message.role.uppercase()}: ${message.text}",
                    modifier = Modifier.padding(4.dp),
                    color = MaterialTheme.colorScheme.onBackground // Theme-aware color
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(value = inputQuery, onValueChange = { inputQuery = it }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                viewModel.executePrompt(inputQuery)
                inputQuery = ""
            }) { Text("Run") }
        }
    }
}
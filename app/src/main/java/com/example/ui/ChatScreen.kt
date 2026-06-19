package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import android.provider.OpenableColumns
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.model.ChatViewModel
import com.example.model.AuthState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(), 
    onSignOut: () -> Unit = {}, 
    onRequireAuth: () -> Unit = {}, 
    authState: AuthState = AuthState.Unauthenticated, 
    isDarkTheme: Boolean = true, 
    onToggleTheme: () -> Unit = {},
    currentThemeColor: com.example.ui.theme.AppThemeColor = com.example.ui.theme.AppThemeColor.GREEN,
    onThemeColorSelected: (com.example.ui.theme.AppThemeColor) -> Unit = {}
) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val currentSession = sessions.find { it.id == currentSessionId }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var textInput by remember { mutableStateOf("") }
    var useHighThinking by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMsg by viewModel.error.collectAsState()

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    LaunchedEffect(currentSessionId) {
        isSearchActive = false
        searchQuery = ""
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            viewModel.setUser(authState.email)
        } else {
            viewModel.setUser(null)
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismissRequest = { showSettingsDialog = false }
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Chat") },
            text = { Text("Are you sure you want to delete this chat?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(showDeleteDialog!!)
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Chats") },
            text = { Text("Are you sure you want to clear all chat history? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearAllDialog = false
                }) { Text("Clear All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { 
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                    Spacer(Modifier.width(8.dp))
                    Text("New Chat")
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text(
                            "Chat History",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(sessions) { session ->
                        NavigationDrawerItem(
                            label = { Text(session.title, maxLines = 1) },
                            selected = session.id == currentSessionId,
                            onClick = {
                                viewModel.selectSession(session.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            badge = {
                                IconButton(onClick = { showDeleteDialog = session.id }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }
                val currentProvider by com.example.model.ApiConfigService.currentProvider.collectAsState()

                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    label = { Text("AI Engine Settings", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        showSettingsDialog = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    badge = {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "${currentProvider.name} (Active)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (isDarkTheme) "Nexis Dark Theme" else "Classic Light Theme", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
                }
                
                Text(
                    text = "Theme Accent", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(com.example.ui.theme.AppThemeColor.values().toList()) { colorEnum ->
                        val displayColor = when (colorEnum) {
                            com.example.ui.theme.AppThemeColor.GREEN -> com.example.ui.theme.GptGreen
                            com.example.ui.theme.AppThemeColor.LAVENDER -> com.example.ui.theme.LavenderPrimary
                            com.example.ui.theme.AppThemeColor.PASTEL_BLUE -> com.example.ui.theme.PastelBluePrimary
                            com.example.ui.theme.AppThemeColor.PINK -> com.example.ui.theme.PinkPrimary
                            com.example.ui.theme.AppThemeColor.RED -> com.example.ui.theme.RedPrimary
                            com.example.ui.theme.AppThemeColor.YELLOW -> com.example.ui.theme.YellowPrimary
                            com.example.ui.theme.AppThemeColor.MINT -> com.example.ui.theme.MintPrimary
                            com.example.ui.theme.AppThemeColor.PEACH -> com.example.ui.theme.PeachPrimary
                            com.example.ui.theme.AppThemeColor.PURPLE -> com.example.ui.theme.PurplePrimary
                        }
                        
                        val isSelected = colorEnum == currentThemeColor
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(displayColor, androidx.compose.foundation.shape.CircleShape)
                                .clickable { onThemeColorSelected(colorEnum) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = { showClearAllDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All Chats")
                }
                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { 
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                placeholder = { Text("Search chat...", fontSize = 14.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        searchQuery = ""
                                        isSearchActive = false
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Search", modifier = Modifier.size(20.dp))
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(25.dp)
                            )
                        } else {
                            Text("Nexis AI", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-0.5).sp) 
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        if (!isSearchActive) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive && currentSession != null && currentSession.messages.isNotEmpty()) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        if (authState !is AuthState.Authenticated && !isSearchActive) {
                            TextButton(onClick = onRequireAuth) {
                                Text("Log in", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Chat Area
                Box(modifier = Modifier.weight(1f)) {
                    if (currentSession == null || currentSession.messages.isEmpty()) {
                        WelcomeScreen(
                            onPromptSelected = { prompt ->
                                viewModel.sendMessage(prompt, useHighThinking)
                            },
                            authState = authState,
                            onRequireAuth = onRequireAuth
                        )
                    } else {
                        val filteredMessages = if (searchQuery.isBlank()) {
                            currentSession.messages
                        } else {
                            currentSession.messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
                        }
                        MessageList(
                            messages = filteredMessages, 
                            isLoading = isLoading,
                            onEdit = { messageId, newText ->
                                viewModel.editMessage(currentSessionId!!, messageId, newText, useHighThinking)
                            },
                            onDelete = { messageId ->
                                viewModel.deleteMessage(currentSessionId!!, messageId)
                            }
                        )
                    }
                }
                
                // Input Area
                InputArea(
                    textInput = textInput,
                    onTextChange = { textInput = it },
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput, useHighThinking)
                            textInput = ""
                        }
                    },
                    onStop = {
                        viewModel.stopResponse()
                    },
                    isLoading = isLoading,
                    authState = authState,
                    onRequireAuth = onRequireAuth
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(onPromptSelected: (String) -> Unit, authState: AuthState, onRequireAuth: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("How can I help you today?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        
        val suggestedPrompts = listOf(
            "Explain AI" to Icons.Default.Psychology,
            "Help me code in Kotlin" to Icons.Outlined.Code,
            "Recommend a movie" to Icons.Outlined.Movie,
            "Summarize this text" to Icons.Outlined.Description
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            suggestedPrompts.forEach { (prompt, icon) ->
                Card(
                    onClick = { onPromptSelected(prompt) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(prompt, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageList(
    messages: List<ChatMessage>, 
    isLoading: Boolean,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val showFab by androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            // Show FAB if there are items and the last visible item is NOT the actual last item
            lastVisibleItem != null && lastVisibleItem.index < listState.layoutInfo.totalItemsCount - 1
        }
    }
    
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    onEdit = { newText -> onEdit(message.id, newText) },
                    onDelete = { onDelete(message.id) }
                )
            }
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        
        if (showFab) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
            }
        }
    }
}

@Composable
fun ThinkingOrb() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "orb_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "orb_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "orb_alpha"
    )
    Box(
        modifier = Modifier
            .size(12.dp)
            .scale(scale)
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
    )
}

@Composable
fun FormattedTextWithCodeBlocks(text: String, isTyping: Boolean = false) {
    val blocks = text.split("```")
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        for (i in blocks.indices) {
            val block = blocks[i]
            if (i % 2 == 1) { // It's a code block
                val lines = block.lines()
                val lang = lines.firstOrNull()?.trim() ?: ""
                val code = lines.drop(1).joinToString("\n").trimEnd()
                
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF2D2D2D)).padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang.ifEmpty { "Code" }, color = androidx.compose.ui.graphics.Color.LightGray, fontSize = 12.sp)
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(code))
                                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, contentDescription = "Copy Code", modifier = Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color.LightGray)
                            }
                        }
                        Text(
                            text = if (isTyping && i == blocks.lastIndex) code + " ▌" else code,
                            color = androidx.compose.ui.graphics.Color(0xFFD4D4D4),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            } else { // Regular text
                if (block.isNotEmpty() || (isTyping && i == blocks.lastIndex)) {
                    Text(
                        text = if (isTyping && i == blocks.lastIndex) block + " ▌" else block,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isUser = message.isUser
    var feedbackState by remember { mutableIntStateOf(0) }
    var isEditing by remember { mutableStateOf(false) }
    var editInput by remember { mutableStateOf(message.text) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 8.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)) {
                Box(
                    modifier = Modifier.size(24.dp).background(Color.Transparent, CircleShape).padding(2.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nexis AI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        
        if (isEditing && isUser) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                OutlinedTextField(
                    value = editInput,
                    onValueChange = { editInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 10
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { 
                        isEditing = false
                        editInput = message.text
                    }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        isEditing = false
                        onEdit(editInput)
                    },
                    shape = CircleShape) {
                        Text("Save & Submit")
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    ))
                    .background(if (isUser) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .padding(if (isUser) 16.dp else 2.dp)
            ) {
                if (message.isThinking) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
                        ThinkingOrb()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(message.text, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                } else {
                    if (isUser) {
                        Text(
                            text = if (message.isTyping) message.text + " ▌" else message.text,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            letterSpacing = 0.2.sp
                        )
                    } else {
                        FormattedTextWithCodeBlocks(text = message.text, isTyping = message.isTyping)
                    }
                }
            }
        }
        
        if (!message.isThinking && !message.isTyping && !isEditing) {
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current
            
            if (isUser) {
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isEditing = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onDelete() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }, 
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(top = 8.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(message.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { feedbackState = 1 }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ThumbUp, contentDescription = "Like", modifier = Modifier.size(16.dp), tint = if (feedbackState == 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { feedbackState = -1 }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", modifier = Modifier.size(16.dp), tint = if (feedbackState == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            } // closes outer Row
            } // closes else
        } // closes if block
    } // closes Column
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputArea(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit = {},
    isLoading: Boolean,
    authState: AuthState,
    onRequireAuth: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = onTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message AI...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 14.sp) },
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 4
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (isLoading) {
                            onStop()
                        } else if (textInput.isNotBlank()) {
                            onSend()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isLoading) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}



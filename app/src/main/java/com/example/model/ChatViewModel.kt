package com.example.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.Part
import com.example.network.RetrofitClient
import com.example.network.ThinkingConfig
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlinx.coroutines.delay

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false,
    val isTyping: Boolean = false,
    val imageB64: String? = null,
    val imageMimeType: String? = null
)

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    var isAuthenticated: Boolean = false
        set(value) {
            field = value
            if (value) {
                persistSessions()
            }
        }

    private val firestoreService = FirestoreService(application)
    private val geminiRepository = GeminiRepository()
    
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    var currentUserEmail: String? = null
        private set

    private var collectJob: kotlinx.coroutines.Job? = null
    private var messagesCollectJob: kotlinx.coroutines.Job? = null

    private var settingsCollectJob: kotlinx.coroutines.Job? = null

    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()
    
    private val _themeColor = MutableStateFlow(com.example.ui.theme.AppThemeColor.GREEN)
    val themeColor: StateFlow<com.example.ui.theme.AppThemeColor> = _themeColor.asStateFlow()

    fun setUser(email: String?) {
        currentUserEmail = email
        isAuthenticated = (email != null)
        
        collectJob?.cancel()
        messagesCollectJob?.cancel()
        settingsCollectJob?.cancel()
        _sessions.value = emptyList()
        _currentSessionId.value = null

        if (email != null) {
            settingsCollectJob = viewModelScope.launch {
                firestoreService.getSettingsFlow(email).collect { (themePref, colorName) ->
                    _isDarkTheme.value = themePref
                    try {
                        _themeColor.value = com.example.ui.theme.AppThemeColor.valueOf(colorName)
                    } catch (e: Exception) {
                        _themeColor.value = com.example.ui.theme.AppThemeColor.GREEN
                    }
                }
            }

            collectJob = viewModelScope.launch {
                firestoreService.getChatsFlow(email).collect { storedChats ->
                    // Merge new chats with existing UI state to keep local messages if any
                    val currentList = _sessions.value
                    if (currentList.isEmpty() && storedChats.isEmpty()) {
                        createNewSession()
                    } else {
                        val merged = storedChats.map { storedChat ->
                            val existing = currentList.find { it.id == storedChat.id }
                            if (existing != null) {
                                // Keep local messages
                                storedChat.copy(messages = existing.messages)
                            } else {
                                storedChat
                            }
                        }
                        _sessions.value = merged
                        if (_currentSessionId.value == null) {
                            val firstId = merged.firstOrNull()?.id
                            _currentSessionId.value = firstId
                            if (firstId != null) subscribeToMessages(firstId)
                        }
                    }
                }
            }
        } else {
            createNewSession()
        }
    }

    fun createNewSession() {
        val newSession = ChatSession(title = "New Chat")
        _sessions.value = listOf(newSession) + _sessions.value
        _currentSessionId.value = newSession.id
        subscribeToMessages(newSession.id)
        persistSessions()
    }

    fun selectSession(id: String) {
        _currentSessionId.value = id
        subscribeToMessages(id)
    }

    private fun subscribeToMessages(chatId: String) {
        messagesCollectJob?.cancel()
        messagesCollectJob = viewModelScope.launch {
            firestoreService.getMessagesFlow(chatId).collect { dbMessages ->
                val currentList = _sessions.value
                val sessionIndex = currentList.indexOfFirst { it.id == chatId }
                if (sessionIndex != -1) {
                    val session = currentList[sessionIndex]
                    // We need to keep local 'isTyping' or 'isThinking' messages
                    val localTempMessages = session.messages.filter { it.isThinking || it.isTyping }
                    val mergedMessages = (dbMessages + localTempMessages).sortedBy { it.timestamp }
                    
                    val updatedSession = session.copy(messages = mergedMessages)
                    val newList = currentList.toMutableList()
                    newList[sessionIndex] = updatedSession
                    _sessions.value = newList
                }
            }
        }
    }

    fun setTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        val email = currentUserEmail ?: return
        val currentColor = _themeColor.value.name
        viewModelScope.launch {
            firestoreService.saveSettings(email, isDark, currentColor)
        }
    }

    fun setThemeColor(color: com.example.ui.theme.AppThemeColor) {
        _themeColor.value = color
        val email = currentUserEmail ?: return
        val currentDarkTheme = _isDarkTheme.value ?: false
        viewModelScope.launch {
            firestoreService.saveSettings(email, currentDarkTheme, color.name)
        }
    }

    fun deleteSession(id: String) {
        _sessions.value = _sessions.value.filter { it.id != id }
        if (_currentSessionId.value == id) {
            val newId = _sessions.value.firstOrNull()?.id
            _currentSessionId.value = newId
            newId?.let { subscribeToMessages(it) } ?: run { messagesCollectJob?.cancel() }
        }
        viewModelScope.launch {
            firestoreService.deleteSession(id)
        }
    }

    fun clearAll() {
        val email = currentUserEmail
        _sessions.value = emptyList()
        createNewSession()
        if (email != null) {
            viewModelScope.launch {
                firestoreService.clearAll(email)
            }
        }
    }

    private fun persistSessions() {
        val email = currentUserEmail ?: return
        if (!isAuthenticated) return
        val currentSessions = _sessions.value
        viewModelScope.launch {
            for (session in currentSessions) {
                // Save without the transient messages
                val cleanSession = session.copy(messages = session.messages.filter { !it.isThinking && !it.isTyping })
                firestoreService.saveSession(email, cleanSession)
            }
        }
    }

    private var activeApiJob: kotlinx.coroutines.Job? = null
    private var activeThinkingJob: kotlinx.coroutines.Job? = null

    fun stopResponse() {
        activeApiJob?.cancel()
        activeThinkingJob?.cancel()
        _isLoading.value = false
        val sessionId = _currentSessionId.value ?: return
        val currentSession = _sessions.value.find { it.id == sessionId } ?: return
        
        val updatedMessages = currentSession.messages.filter { !it.isThinking }.map {
            if (it.isTyping) it.copy(text = it.text + " [Stopped]", isTyping = false) else it
        }
        updateSession(currentSession.copy(messages = updatedMessages))
    }

    fun sendMessage(text: String, useHighThinking: Boolean = false, imageB64: String? = null, imageMimeType: String? = null) {
        if (text.isBlank() && imageB64 == null) return
        
        val sessionId = _currentSessionId.value ?: return
        val currentSession = _sessions.value.find { it.id == sessionId } ?: return
        
        val userMessage = ChatMessage(text = text, isUser = true, imageB64 = imageB64, imageMimeType = imageMimeType)
        
        // Update session title if it's the first message
        val updatedTitle = if (currentSession.messages.isEmpty()) {
            text.take(30) + if(text.length > 30) "..." else ""
        } else currentSession.title

        // Add thinking indicator temporarily
        val thinkingMessageId = UUID.randomUUID().toString()
        val thinkingMessage = ChatMessage(id = thinkingMessageId, text = "Nexis is thinking...", isUser = false, isThinking = true)
        
        val sessionWithThinking = currentSession.copy(
            title = updatedTitle,
            messages = currentSession.messages + userMessage + thinkingMessage
        )
        updateSession(sessionWithThinking)
        
        activeThinkingJob = viewModelScope.launch {
            val thinkingStates = listOf("Nexis is thinking", "Nexis is analyzing", "Nexis is connecting ideas")
            var idx = 0
            while (true) {
                delay(2000)
                val currentSessions = _sessions.value
                val session = currentSessions.find { it.id == sessionId }
                if (session != null) {
                    val updatedMessages = session.messages.map {
                        if (it.id == thinkingMessageId) it.copy(text = thinkingStates[idx], isThinking = true) else it
                    }
                    updateSession(session.copy(messages = updatedMessages), skipPersist = true)
                }
                idx = (idx + 1) % thinkingStates.size
            }
        }
        
        activeApiJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val currentProvider = com.example.model.ApiConfigService.currentProvider.value
            val isAuto = currentProvider == com.example.model.ApiProvider.AUTO

            var selectedProvider = currentProvider
            if (isAuto) {
                val textLower = text.lowercase()
                val isCoding = listOf(
                    "code", "programming", "python", "javascript", "react", "android", "bug", "error", 
                    "exception", "syntax", "function", "class", "compile", "develop", "database", "sql"
                ).any { textLower.contains(it) }
                
                val isCreative = listOf(
                    "write", "poem", "lyrics", "creative", "story", "generate ideas", "blog", "draft", "translate"
                ).any { textLower.contains(it) }

                val providers = com.example.model.ApiProvider.values().filter { 
                    it != com.example.model.ApiProvider.AUTO && 
                    com.example.model.ApiConfigService.isProviderConfigured(it) 
                }

                selectedProvider = if (providers.isNotEmpty()) {
                    if (isCoding) {
                        providers.find { it == com.example.model.ApiProvider.DEEPSEEK } 
                            ?: providers.find { it == com.example.model.ApiProvider.OPENROUTER } 
                            ?: providers.first()
                    } else if (isCreative) {
                        providers.find { it == com.example.model.ApiProvider.GEMINI } 
                            ?: providers.find { it == com.example.model.ApiProvider.OPENROUTER } 
                            ?: providers.first()
                    } else {
                        providers.find { it == com.example.model.ApiProvider.GROQ } 
                            ?: providers.find { it == com.example.model.ApiProvider.GEMINI } 
                            ?: providers.first()
                    }
                } else {
                    com.example.model.ApiProvider.GEMINI
                }
            }

            val fallbackChain = mutableListOf<com.example.model.ApiProvider>()
            fallbackChain.add(selectedProvider)
            
            val otherConfigured = com.example.model.ApiProvider.values().filter {
                it != com.example.model.ApiProvider.AUTO &&
                it != selectedProvider &&
                com.example.model.ApiConfigService.isProviderConfigured(it)
            }
            fallbackChain.addAll(otherConfigured)
            
            if (!fallbackChain.contains(com.example.model.ApiProvider.GEMINI) && com.example.model.ApiConfigService.isProviderConfigured(com.example.model.ApiProvider.GEMINI)) {
                fallbackChain.add(com.example.model.ApiProvider.GEMINI)
            }
            
            val finalChain = fallbackChain.filter { com.example.model.ApiConfigService.isProviderConfigured(it) }.ifEmpty { listOf(selectedProvider) }

            if (finalChain.size == 1 && !com.example.model.ApiConfigService.isProviderConfigured(finalChain.first())) {
                _isLoading.value = false
                activeThinkingJob?.cancel()
                val providerName = finalChain.first().name
                val errorMsgText = "Please define ${providerName}_API_KEY in the Secrets panel in AI Studio to use this provider."
                _error.value = errorMsgText
                
                val finalSession = _sessions.value.find { it.id == sessionId }
                if (finalSession != null) {
                    val messagesWithoutThinking = finalSession.messages.filter { it.id != thinkingMessageId }
                    val errorMsg = ChatMessage(text = errorMsgText, isUser = false)
                    updateSession(finalSession.copy(messages = messagesWithoutThinking + errorMsg))
                }
                return@launch
            }

            try {
                val historyForApi = sessionWithThinking.messages.filter { it.id != thinkingMessageId }
                
                var replyText = ""
                var success = false
                var finalUsedProvider = selectedProvider
                var lastError: Exception? = null

                for (provider in finalChain) {
                    finalUsedProvider = provider
                    if (!com.example.model.ApiConfigService.isProviderConfigured(provider)) {
                        continue
                    }
                    try {
                        com.example.model.ApiConfigService.temporaryOverrideProvider = provider
                        android.util.Log.d("NEXIS_API", "Routing request to: ${provider.name}")
                        val attemptReply = geminiRepository.generateContent(
                            history = historyForApi,
                            useHighThinking = useHighThinking,
                            overrideProvider = provider
                        )
                        if (attemptReply.isNotBlank() && !attemptReply.contains("Too much love")) {
                            replyText = attemptReply
                            success = true
                            android.util.Log.d("NEXIS_API", "Success with provider ${provider.name}")
                            break
                        } else {
                            throw Exception("Empty response or standard error from ${provider.name}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NEXIS_API_FALLBACK", "Provider ${provider.name} failed", e)
                        lastError = e
                    } finally {
                        com.example.model.ApiConfigService.temporaryOverrideProvider = null
                    }
                }

                if (!success) {
                    throw lastError ?: Exception("All configured model services are currently unavailable.")
                }

                activeThinkingJob?.cancel()
                
                // Keep only alphanumeric and standard punctuation for a cleaner experience, and process the response
                val cleanedText = cleanMarkdown(replyText)
                
                // Replace Thinking message with actual response container
                val finalSession = _sessions.value.find { it.id == sessionId }
                if (finalSession != null) {
                    val messagesWithoutThinking = finalSession.messages.filter { it.id != thinkingMessageId }
                    val aiMessageId = UUID.randomUUID().toString()
                    var aiMessage = ChatMessage(id = aiMessageId, text = "", isUser = false, isTyping = true)
                    
                    updateSession(finalSession.copy(messages = messagesWithoutThinking + aiMessage), skipPersist = true)
                    
                    // Faster typing effect
                    val chunk_size = 20
                    for (i in 1..cleanedText.length step chunk_size) {
                        val endIdx = Math.min(i + chunk_size - 1, cleanedText.length)
                        aiMessage = aiMessage.copy(text = cleanedText.substring(0, endIdx), isTyping = true)
                        
                        val currentSessions = _sessions.value
                        val currentSess = currentSessions.find { it.id == sessionId }
                        if (currentSess != null) {
                            val updatedMessages = currentSess.messages.map { if (it.id == aiMessageId) aiMessage else it }
                            updateSession(currentSess.copy(messages = updatedMessages), skipPersist = true)
                        }
                        
                        delay(2) // Fast chunk print
                    }
                    
                    // Typing finished
                    val lastSessions = _sessions.value
                    val lastSess = lastSessions.find { it.id == sessionId }
                    if (lastSess != null) {
                        aiMessage = aiMessage.copy(isTyping = false)
                        val updatedMessages = lastSess.messages.map { if (it.id == aiMessageId) aiMessage else it }
                        updateSession(lastSess.copy(messages = updatedMessages))
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("NEXIS_ERROR", "API Request failed", e)
                activeThinkingJob?.cancel()
                val fallbackMsg = "Too much love right now \uD83D\uDC9C Try again later."
                
                _error.value = fallbackMsg
                
                // Replace Thinking message with error message inside the chat
                val finalSession = _sessions.value.find { it.id == sessionId }
                if (finalSession != null) {
                    val messagesWithoutThinking = finalSession.messages.filter { it.id != thinkingMessageId }
                    val errorMsg = ChatMessage(text = fallbackMsg, isUser = false)
                    updateSession(finalSession.copy(messages = messagesWithoutThinking + errorMsg))
                }
            } finally {
                _isLoading.value = false
                if (isAuto) {
                    com.example.model.ApiConfigService.temporaryOverrideProvider = null
                }
            }
        }
    }

    // Clean up markdown formatting from AI responses
    private fun cleanMarkdown(text: String): String {
        var cleaned = text
        // Remove bold
        cleaned = cleaned.replace(Regex("\\*\\*(.*?)\\*\\*")) { it.groupValues[1] }
        // Remove italic
        cleaned = cleaned.replace(Regex("\\*(.*?)\\*")) { it.groupValues[1] }
        // Convert asterisk bullets to interpunct (after removing italics/bold, but some bullets might be dashes too)
        cleaned = cleaned.replace(Regex("(?m)^\\s*[-]\\s+")) { "• " }
        cleaned = cleaned.replace(Regex("(?m)^\\s*\\*\\s+")) { "• " }
        // Remove headings
        cleaned = cleaned.replace(Regex("(?m)^#{1,6}\\s+(.*)$")) { it.groupValues[1] }
        // Remove horizontal rules
        cleaned = cleaned.replace(Regex("(?m)^\\s*[-_*]{3,}\\s*$")) { "" }
        return cleaned.trim()
    }

    private fun updateSession(session: ChatSession, skipPersist: Boolean = false) {
        _sessions.value = _sessions.value.map { if (it.id == session.id) session else it }
        if (!skipPersist) {
            persistSessions()
        }
    }

    fun editMessage(sessionId: String, messageId: String, newText: String, useHighThinking: Boolean = false) {
        if (newText.isBlank()) return
        val session = _sessions.value.find { it.id == sessionId } ?: return
        val messageIndex = session.messages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) return

        val retainedMessages = session.messages.subList(0, messageIndex)
        updateSession(session.copy(messages = retainedMessages))
        
        sendMessage(newText, useHighThinking)
    }

    fun deleteMessage(sessionId: String, messageId: String) {
        val session = _sessions.value.find { it.id == sessionId } ?: return
        val updatedMessages = session.messages.filter { it.id != messageId }
        updateSession(session.copy(messages = updatedMessages))
    }
}

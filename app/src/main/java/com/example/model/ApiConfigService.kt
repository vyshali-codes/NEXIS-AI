package com.example.model

import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ApiProvider {
    AUTO,
    GEMINI,
    OPENROUTER,
    DEEPSEEK,
    GROQ,
    TOGETHER_AI,
    DEEPINFRA,
    HUGGING_FACE
}

data class ProviderConfig(
    val provider: ApiProvider,
    val apiKey: String,
    val baseUrl: String,
    val requiresBearer: Boolean
)

object ApiConfigService {
    var temporaryOverrideProvider: ApiProvider? = null

    private val _currentProvider = MutableStateFlow(ApiProvider.AUTO)
    val currentProvider: StateFlow<ApiProvider> = _currentProvider.asStateFlow()

    private val _currentModel = MutableStateFlow(getDefaultModelForProvider(ApiProvider.AUTO))
    val currentModel: StateFlow<String> = _currentModel.asStateFlow()

    fun setProvider(provider: ApiProvider) {
        _currentProvider.value = provider
        _currentModel.value = getDefaultModelForProvider(provider)
    }

    fun setModel(model: String) {
        _currentModel.value = model
    }

    fun getActiveConfig(): ProviderConfig {
        return getConfigForProvider(temporaryOverrideProvider ?: _currentProvider.value)
    }

    fun getActiveModel(): String {
        return temporaryOverrideProvider?.let { getDefaultModelForProvider(it) } ?: _currentModel.value
    }

    private fun getConfigForProvider(provider: ApiProvider): ProviderConfig {
        return when (provider) {
            ApiProvider.AUTO -> ProviderConfig(
                provider = ApiProvider.AUTO,
                apiKey = "",
                baseUrl = "",
                requiresBearer = false
            )
            ApiProvider.GEMINI -> ProviderConfig(
                provider = ApiProvider.GEMINI,
                apiKey = getApiKey(BuildConfig.GEMINI_API_KEY ?: ""),
                baseUrl = "https://generativelanguage.googleapis.com/",
                requiresBearer = false
            )
            ApiProvider.OPENROUTER -> ProviderConfig(
                provider = ApiProvider.OPENROUTER,
                apiKey = getApiKey(BuildConfig.OPENROUTER_API_KEY ?: ""),
                baseUrl = "https://openrouter.ai/api/v1/",
                requiresBearer = true
            )
            ApiProvider.DEEPSEEK -> ProviderConfig(
                provider = ApiProvider.DEEPSEEK,
                apiKey = getApiKey(BuildConfig.DEEPSEEK_API_KEY ?: ""),
                baseUrl = "https://integrate.api.nvidia.com/v1/",
                requiresBearer = true
            )
            ApiProvider.GROQ -> ProviderConfig(
                provider = ApiProvider.GROQ,
                apiKey = getApiKey(BuildConfig.GROQ_API_KEY ?: ""),
                baseUrl = "https://api.groq.com/openai/v1/",
                requiresBearer = true
            )
            ApiProvider.TOGETHER_AI -> ProviderConfig(
                provider = ApiProvider.TOGETHER_AI,
                apiKey = getApiKey(BuildConfig.OPENAI_API_KEY ?: ""), // Using default API key format
                baseUrl = "https://api.together.xyz/v1/",
                requiresBearer = true
            )
            ApiProvider.DEEPINFRA -> ProviderConfig(
                provider = ApiProvider.DEEPINFRA,
                apiKey = getApiKey(BuildConfig.OPENAI_API_KEY ?: ""),
                baseUrl = "https://api.deepinfra.com/v1/openai/",
                requiresBearer = true
            )
            ApiProvider.HUGGING_FACE -> ProviderConfig(
                provider = ApiProvider.HUGGING_FACE,
                apiKey = getApiKey(BuildConfig.HUGGING_FACE_API_KEY ?: ""),
                baseUrl = "https://api-inference.huggingface.co/v1/",
                requiresBearer = true
            )
        }
    }

    private fun getApiKey(key: String): String {
        return key // We don't throw exception anymore, allow request to fire and catch errors safely.
    }

    fun isProviderConfigured(provider: ApiProvider): Boolean {
        return true
    }

    fun getDefaultModelForProvider(provider: ApiProvider): String {
        return when (provider) {
            ApiProvider.AUTO -> "Auto mode"
            ApiProvider.GEMINI -> "gemini-1.5-flash"
            ApiProvider.DEEPSEEK -> "deepseek-chat"
            ApiProvider.GROQ -> "llama3-70b-8192"
            ApiProvider.OPENROUTER -> "openai/gpt-3.5-turbo"
            ApiProvider.TOGETHER_AI -> "meta-llama/Llama-3-70b-chat-hf"
            ApiProvider.DEEPINFRA -> "meta-llama/Meta-Llama-3-70B-Instruct"
            ApiProvider.HUGGING_FACE -> "meta-llama/Meta-Llama-3-8B-Instruct"
        }
    }

    suspend fun runDiagnostics(provider: ApiProvider): DiagnosticResult {
        if (provider == ApiProvider.AUTO) {
            return DiagnosticResult.Success("Auto mode determines the best provider automatically.")
        }
        
        if (!isProviderConfigured(provider)) {
            return DiagnosticResult.Error("API key not configured in AI Studio Secrets panel.")
        }
        
        try {
            val modelName = getDefaultModelForProvider(provider)
            
            if (provider == ApiProvider.GEMINI) {
                val service = com.example.network.RetrofitClient.getGeminiService()
                val testRequest = com.example.network.GenerateContentRequest(
                    contents = listOf(
                        com.example.network.Content(
                            role = "user",
                            parts = listOf(com.example.network.Part(text = "hi"))
                        )
                    ),
                    generationConfig = com.example.network.GenerationConfig(
                        temperature = 0.1f
                    ),
                    systemInstruction = null
                )
                
                temporaryOverrideProvider = provider
                val response = service.generateContent(modelName, testRequest)
                temporaryOverrideProvider = null
                
                if (response.candidates?.firstOrNull() != null) {
                    return DiagnosticResult.Success("Verified connectivity to Gemini API.")
                } else {
                    return DiagnosticResult.Error("Empty or null response from Gemini API.")
                }
            } else {
                val service = com.example.network.RetrofitClient.getOpenAiService()
                val testRequest = com.example.network.OpenAiChatRequest(
                    model = modelName,
                    messages = listOf(com.example.network.OpenAiMessage(role = "user", content = "hi"))
                )
                
                temporaryOverrideProvider = provider
                val response = service.generateContent(testRequest)
                temporaryOverrideProvider = null
                
                if (response.choices.isNotEmpty()) {
                    return DiagnosticResult.Success("Verified connectivity & credentials.")
                } else {
                    return DiagnosticResult.Error("Empty response from API.")
                }
            }
        } catch (e: Exception) {
            temporaryOverrideProvider = null
            val errorMsg = e.localizedMessage ?: e.message ?: "Unknown Connection Error"
            return DiagnosticResult.Error(if (errorMsg.contains("401")) "Invalid credentials (401 Unauthorized)." else errorMsg)
        }
    }
}

sealed class DiagnosticResult {
    data class Success(val message: String) : DiagnosticResult()
    data class Error(val message: String) : DiagnosticResult()
}

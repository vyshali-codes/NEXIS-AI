package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ApiConfigService
import com.example.model.ApiProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit
) {
    val currentProvider by ApiConfigService.currentProvider.collectAsState()
    var diagnosticStatuses by remember { mutableStateOf<Map<ApiProvider, com.example.model.DiagnosticResult?>>(emptyMap()) }
    var isRunningAllDiagnostics by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI Engine Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close settings"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Configure and switch your active AI Provider.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val currentModel by ApiConfigService.currentModel.collectAsState()
                
                OutlinedTextField(
                    value = currentModel,
                    onValueChange = { ApiConfigService.setModel(it) },
                    label = { Text("Selected Model") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Providers list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ApiProvider.values()) { provider ->
                        val isConfigured = ApiConfigService.isProviderConfigured(provider)
                        val isSelected = provider == currentProvider
                        val defaultModel = ApiConfigService.getDefaultModelForProvider(provider)
                        val envVarName = getEnvVarName(provider)
                        val diagnosticResult = diagnosticStatuses[provider]
                        
                        ProviderItemCard(
                            provider = provider,
                            isConfigured = isConfigured,
                            isSelected = isSelected,
                            defaultModel = defaultModel,
                            envVarName = envVarName,
                            diagnosticResult = diagnosticResult,
                            onClick = {
                                ApiConfigService.setProvider(provider)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply & Close")
                }
            }
        }
    }
}

@Composable
fun ProviderItemCard(
    provider: ApiProvider,
    isConfigured: Boolean,
    isSelected: Boolean,
    defaultModel: String,
    envVarName: String,
    diagnosticResult: com.example.model.DiagnosticResult? = null,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isConfigured -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        isConfigured -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    val contentAlpha = if (isConfigured) 1f else 0.6f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = true, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getFriendlyName(provider),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Text(
                        text = "Env Key: $envVarName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                    if (diagnosticResult != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val color = when (diagnosticResult) {
                                is com.example.model.DiagnosticResult.Success -> Color(0xFF4CAF50)
                                is com.example.model.DiagnosticResult.Error -> MaterialTheme.colorScheme.error
                            }
                            val text = when (diagnosticResult) {
                                is com.example.model.DiagnosticResult.Success -> diagnosticResult.message
                                is com.example.model.DiagnosticResult.Error -> diagnosticResult.message
                            }
                            val icon = when (diagnosticResult) {
                                is com.example.model.DiagnosticResult.Success -> Icons.Default.CheckCircle
                                is com.example.model.DiagnosticResult.Error -> Icons.Default.Warning
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isConfigured) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Not Configured",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    if (isSelected) {
                        RadioButton(
                            selected = true,
                            onClick = onClick,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        RadioButton(
                            selected = false,
                            onClick = onClick,
                            colors = RadioButtonDefaults.colors(
                                unselectedColor = if (isConfigured) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Base URL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f * contentAlpha)
                    )
                    Text(
                        text = getBaseUrlDisplay(provider),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f * contentAlpha)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Default Model",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f * contentAlpha)
                    )
                    Text(
                        text = defaultModel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f * contentAlpha)
                    )
                }
            }
        }
    }
}

fun getFriendlyName(provider: ApiProvider): String {
    return when (provider) {
        ApiProvider.AUTO -> "Auto mode"
        ApiProvider.GEMINI -> "Google Gemini"
        ApiProvider.OPENROUTER -> "OpenRouter"
        ApiProvider.DEEPSEEK -> "DeepSeek AI"
        ApiProvider.GROQ -> "Groq Cloud"
        ApiProvider.TOGETHER_AI -> "Together AI"
        ApiProvider.DEEPINFRA -> "DeepInfra"
        ApiProvider.HUGGING_FACE -> "Hugging Face"
    }
}

fun getEnvVarName(provider: ApiProvider): String {
    return when (provider) {
        ApiProvider.AUTO -> "Not needed"
        ApiProvider.GEMINI -> "GEMINI_API_KEY"
        ApiProvider.OPENROUTER -> "OPENROUTER_API_KEY"
        ApiProvider.DEEPSEEK -> "DEEPSEEK_API_KEY"
        ApiProvider.GROQ -> "GROQ_API_KEY"
        ApiProvider.TOGETHER_AI -> "OPENAI_API_KEY"
        ApiProvider.DEEPINFRA -> "OPENAI_API_KEY"
        ApiProvider.HUGGING_FACE -> "HUGGING_FACE_API_KEY"
    }
}

fun getBaseUrlDisplay(provider: ApiProvider): String {
    return when (provider) {
        ApiProvider.AUTO -> "Selects best source"
        ApiProvider.GEMINI -> "generativelanguage.googleapis.com"
        ApiProvider.OPENROUTER -> "openrouter.ai/api"
        ApiProvider.DEEPSEEK -> "api.deepseek.com"
        ApiProvider.GROQ -> "api.groq.com/openai"
        ApiProvider.TOGETHER_AI -> "api.together.xyz"
        ApiProvider.DEEPINFRA -> "api.deepinfra.com"
        ApiProvider.HUGGING_FACE -> "api-inference.huggingface.co"
    }
}

package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BulkImportViewModel
import com.example.ImportState
import com.example.Task

@Composable
fun BulkImportScreen(
    modifier: Modifier = Modifier,
    viewModel: BulkImportViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Smart Bulk Import",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Paste your unstructured tasks here. Our AI will automatically categorize and extract properties like dates and times.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (state is ImportState.Success) {
            val tasks = (state as ImportState.Success).tasks
            
            Text(
                text = "Imported ${tasks.size} Tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tasks) { task ->
                    TaskItemCard(task)
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    viewModel.reset() 
                    inputText = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("import_more_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Confirm & Add More", fontWeight = FontWeight.Bold)
            }
        } else {
            // Text input area
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("bulk_input_field"),
                placeholder = {
                    Text(
                        "Example:\n\nWork:\n- Meeting with team at 10 AM\n- Review PRs\n\nPersonal:\n- Buy groceries (milk, eggs)\n- Call mom tomorrow at 5 PM",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status
            when (val s = state) {
                is ImportState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(s.message, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                ImportState.Idle -> {}
                ImportState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                else -> {}
            }

            // Action Button
            Button(
                onClick = { viewModel.parseTasks(inputText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("parse_button"),
                enabled = inputText.isNotBlank() && state !is ImportState.Loading,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Smart Parse", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "⚠️ Prototype Notice: Uses direct REST API.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun TaskItemCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = task.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            if (task.date != null || task.startTime != null || task.snoozeDuration != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val timeString = listOfNotNull(task.date, task.startTime).joinToString(" at ")
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (task.snoozeDuration != null) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Snooze: ${task.snoozeDuration}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            if (task.voiceRecord || task.postpone) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.voiceRecord) {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("Voice Record", color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (task.postpone) {
                        Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text("Postponable", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val tasks: List<Task>) : ImportState()
    data class Error(val message: String) : ImportState()
}

class BulkImportViewModel : ViewModel() {
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun parseTasks(inputText: String) {
        if (inputText.isBlank()) return
        
        _state.value = ImportState.Loading
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _state.value = ImportState.Error("Please configure your Gemini API Key in the Secrets panel.")
                    return@launch
                }
                
                val prompt = """
                    You are a smart task parser for a To-Do application. 
                    The user will provide a block of text containing categories and tasks.
                    Your job is to extract the main categories and associate each sub-task with its category.
                    Convert this parsed data into a JSON array of task objects.
                    
                    Each task object MUST exactly match this JSON structure:
                    {
                        "category": "String (e.g. Work, Personal, etc.)",
                        "title": "String (the task itself)",
                        "voiceRecord": Boolean (default false),
                        "startTime": "String or null (e.g. 10:00 AM)",
                        "endTime": "String or null",
                        "snoozeDuration": Integer or null (in minutes, if snooze or postpone mentioned)",
                        "date": "String or null (YYYY-MM-DD, e.g. tomorrow's date if mentioned)",
                        "postpone": Boolean (default false)
                    }
                    
                    Infer the fields as best as you can based on the text.
                    Respond ONLY with the raw JSON array. DO NOT WRAP in Markdown formatting like ```json.
                """.trimIndent()

                val request = GenerateContentRequest(
                    systemInstruction = Content(listOf(Part(prompt))),
                    contents = listOf(Content(listOf(Part(inputText)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json")
                )

                val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                val jsonString = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "[]"
                
                val cleanJson = jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                
                val tasks = RetrofitClient.taskListAdapter.fromJson(cleanJson) ?: emptyList()
                if (tasks.isEmpty()) {
                    _state.value = ImportState.Error("Could not find any tasks in the text.")
                } else {
                    _state.value = ImportState.Success(tasks)
                }
                
            } catch (e: Exception) {
                _state.value = ImportState.Error("Failed to parse tasks: ${e.localizedMessage}")
            }
        }
    }
    
    fun reset() {
        _state.value = ImportState.Idle
    }
}

package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.models.DefaultValues
import com.example.myapplication.models.StudentRecord
import com.example.myapplication.utils.FileUtils  // ✅ Import FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRecordsScreen(navController: NavController) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var showDefaultsDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) } // ✅ Save Dialog State

    val records = remember { mutableStateListOf<StudentRecord>().apply { addAll(StudentRecord.getRecords()) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Records", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showSaveDialog = true }) { // ✅ Opens Save Dialog
                        Icon(Icons.Filled.Save, contentDescription = "Save", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Name", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Roll", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Score", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (isEditing) {
                                Text("Action", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    itemsIndexed(records) { index, record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditing) {
                                var name by remember { mutableStateOf(record.name) }
                                var roll by remember { mutableStateOf(record.rollNumber) }
                                var score by remember { mutableStateOf(record.score) }

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    modifier = Modifier.weight(1.5f).height(50.dp),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                                )

                                OutlinedTextField(
                                    value = roll,
                                    onValueChange = { roll = it },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                                )

                                OutlinedTextField(
                                    value = score,
                                    onValueChange = { score = it },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                                )

                                Button(
                                    onClick = { records.removeAt(index) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.weight(0.8f).height(40.dp)
                                ) {
                                    Text("🗑️", fontSize = 12.sp, color = Color.White)
                                }
                            } else {
                                Text(record.name, modifier = Modifier.weight(1.5f), fontSize = 14.sp)
                                Text(record.rollNumber, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                Text(record.score, modifier = Modifier.weight(1f), fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { isEditing = !isEditing }) {
                        Text(if (isEditing) "Save" else "Edit")
                    }
                    Button(onClick = {
                        records.add(StudentRecord(DefaultValues.defaultName, DefaultValues.defaultRoll, DefaultValues.defaultScore))
                    }) {
                        Text("Add Record")
                    }
                    Button(onClick = { records.clear() }) {
                        Text("Clear Data")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Records")
                }
            }
        }
    )

    // ✅ **Save Dialog**
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Records") },
            text = { Text("Choose a file format") },
            confirmButton = {
                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        FileUtils.savePdfFile(context, records)
                    }
                    showSaveDialog = false
                }) {
                    Text("Save as PDF")
                }
            },
            dismissButton = {
                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        FileUtils.saveExcelFile(context, records)
                    }
                    showSaveDialog = false
                }) {
                    Text("Save as Excel")
                }
            }
        )
    }
}

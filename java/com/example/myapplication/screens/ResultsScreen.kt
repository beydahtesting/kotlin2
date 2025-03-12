package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.viewmodels.ResultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavController, viewModel: ResultViewModel = viewModel()) {
    val context = LocalContext.current

    // Observe state from ViewModel
    val teacherBitmap by viewModel.teacherBitmap.collectAsState()
    val gradedBitmap by viewModel.gradedBitmap.collectAsState()
    val resultText by viewModel.resultText.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    // ✅ Enable scrolling
    val scrollState = rememberScrollState()

    // ✅ Load Cached Images
    LaunchedEffect(Unit) {
        viewModel.loadCachedImages(context)
        viewModel.processImagesIfNeeded(context) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results", fontSize = 18.sp) }, // ✅ Reduced Font Size
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("studentRecords") }) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Student Records")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState), // ✅ Ensures the screen scrolls
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(resultText, style = TextStyle(fontSize = 18.sp)) // ✅ Slightly smaller text
                Spacer(modifier = Modifier.height(12.dp)) // ✅ Adjusted spacing

                // ✅ **Teacher Image**
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp), // ✅ Slightly smaller height
                    contentAlignment = Alignment.Center
                ) {
                    if (teacherBitmap != null) {
                        Image(
                            bitmap = teacherBitmap!!.asImageBitmap(),
                            contentDescription = "Teacher Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("No Teacher Image Found", fontSize = 14.sp) // ✅ Smaller text
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ✅ **Graded Image**
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp), // ✅ Adjusted size
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isProcessing -> CircularProgressIndicator()
                        gradedBitmap != null -> {
                            Image(
                                bitmap = gradedBitmap!!.asImageBitmap(),
                                contentDescription = "Graded Image",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> Text("Processing failed!", fontSize = 14.sp) // ✅ Smaller text
                    }
                }

                Spacer(modifier = Modifier.height(20.dp)) // ✅ Adjusted spacing

                // ✅ **Buttons (Always Scrollable)**
                Button(
                    onClick = { navController.navigate("studentRecords") },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("View Student Record", fontSize = 14.sp) // ✅ Reduced font size
                }
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.clearStudentImage(context)
                        navController.navigate("studentImage")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Add Another Student Image", fontSize = 14.sp) // ✅ Reduced font size
                }
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.clearCache(context)
                        navController.navigate("teacherImage")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Upload New Teacher Image", fontSize = 14.sp) // ✅ Reduced font size
                }
            }
        }
    )
}

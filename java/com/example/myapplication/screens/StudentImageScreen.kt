package com.example.myapplication.screens

import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.viewmodels.StudentImageViewModel
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentImageScreen(navController: NavController, viewModel: StudentImageViewModel = viewModel()) {
    val context = LocalContext.current

    val studentBitmap by viewModel.studentBitmap
    val isProcessing by viewModel.isProcessing

    // ✅ Zoom & Offset States
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // ✅ Load cached student image when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadCachedImage(context)
    }

    // ✅ Optimized Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(contract = GetContent()) { uri ->
        uri?.let {
            val selectedBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            viewModel.processStudentImage(context, selectedBitmap) // ✅ Correctly processes image
        }
    }

    // ✅ Camera Capture Functionality
    val cameraLauncher = rememberLauncherForActivityResult(contract = TakePicturePreview()) { bitmap ->
        bitmap?.let {
            viewModel.processStudentImage(context, it) // ✅ Process captured image
        }
    }
    val cameraPermission = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermission.value = granted
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Image") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("result") }) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Upload Student Answer Sheet", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ **Zoom & Move Image Preview**
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Gray, shape = RoundedCornerShape(8.dp))
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isProcessing -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Processing Image...", color = Color.White)
                            }
                        }
                        studentBitmap != null -> {
                            studentBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Student Processed Image",
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                val newScale = max(1f, min(scale * zoom, 3f))

                                                if (newScale != scale) {
                                                    scale = newScale
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                } else if (scale > 1f) {
                                                    offsetX = (offsetX + pan.x).coerceIn(-200f, 200f)
                                                    offsetY = (offsetY + pan.y).coerceIn(-200f, 200f)
                                                }
                                            }
                                        }
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = if (scale > 1f) offsetX else 0f,
                                            translationY = if (scale > 1f) offsetY else 0f
                                        )
                                )
                            }
                        }
                        else -> {
                            Text("Student Image Preview")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ **Fixed Gallery Button**
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select from Gallery")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ **New Take Picture Button**
                Button(
                    onClick = {
                        if (!cameraPermission.value) {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        } else {
                            cameraLauncher.launch(null) // ✅ Launch camera if permission is granted
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take Picture")
                }


                Spacer(modifier = Modifier.height(16.dp))

                // ✅ **Rotate & Flip Buttons**
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.rotateImage(context) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Rotate") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.flipImage(context) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Flip") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ **Confirm Button**
                Button(
                    onClick = {
                        if (studentBitmap == null) {
                            return@Button
                        }
                        navController.navigate("result")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm")
                }
            }
        }
    )
}

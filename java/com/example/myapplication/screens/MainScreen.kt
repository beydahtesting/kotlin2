package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.models.AppMode


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("MCQ Scanner") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    AppMode.setOnlineMode(true)
                    navController.navigate("teacherImage")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Online Mode")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    AppMode.setOnlineMode(false)
                    navController.navigate("teacherImage")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Offline Mode")
            }
        }
    }
}

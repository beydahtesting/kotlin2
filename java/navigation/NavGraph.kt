package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.screens.MainScreen
import com.example.myapplication.screens.ResultScreen
import com.example.myapplication.screens.StudentImageScreen
import com.example.myapplication.screens.StudentRecordsScreen
import com.example.myapplication.screens.TeacherImageScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController) }
        composable("teacherImage") { TeacherImageScreen(navController) }
        composable("studentImage") { StudentImageScreen(navController) }
        composable("result") { ResultScreen(navController) }
        composable("studentRecords") { StudentRecordsScreen(navController) }
    }
}

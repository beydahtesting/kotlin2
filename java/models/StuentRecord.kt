package com.example.myapplication.models

data class StudentRecord(var name: String, var rollNumber: String, var score: String) {
    companion object {
        private val recordList = mutableListOf<StudentRecord>()
        fun addRecord(record: StudentRecord) {
            recordList.add(record)
        }
        fun getRecords(): MutableList<StudentRecord> = recordList
        fun clearRecords() {
            recordList.clear()
        }
    }
}

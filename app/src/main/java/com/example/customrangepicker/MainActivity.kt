package com.example.customrangepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.customrangepicker.ui.theme.CustomRangePickerTheme
import com.example.mylibrary.widget.CustomDateRangePicker
import com.example.mylibrary.widget.CustomDateRangePickerColors
import com.example.mylibrary.widget.CustomDateRangePickerTextStyle
import com.example.mylibrary.widget.rememberCustomDateRangePickerState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val rangePickerState = rememberCustomDateRangePickerState()
            CustomRangePickerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    ) {
                        CustomDateRangePicker(
                            state = rangePickerState,
                            colors = CustomDateRangePickerColors(
                                selectionStartEndColor = MaterialTheme.colorScheme.primary,
                                todayDateIndicatorColor = MaterialTheme.colorScheme.primary,
                                rangeDateColor = MaterialTheme.colorScheme.surfaceContainer,
                                selectionStartEndTextColor = MaterialTheme.colorScheme.onPrimary,
                                dateTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            textStyles = CustomDateRangePickerTextStyle(
                                MaterialTheme.typography.titleMedium,
                                MaterialTheme.typography.titleMedium,
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
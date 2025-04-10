package com.example.expensetracker.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expensetracker.roundToDecimalPlaces
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

@Composable
fun CategoryDonutChart(
    categoryBreakdown: Map<String, Double>,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    // Convert Map to List of Pairs for easier processing
    val categoryData = categoryBreakdown.toList()

    Box(
        modifier = modifier
            .size(200.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw the donut chart
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = totalSpent
            if (total <= 0) return@Canvas

            val strokeWidth = size.width * 0.15f
            val outerRadius = (min(size.width, size.height) / 2) - (strokeWidth / 2)
            val innerRadius = outerRadius - strokeWidth
            val center = Offset(size.width / 2, size.height / 2)

            // Draw donut segments
            var startAngle = -90f
            categoryData.forEach { (category, amount) ->
                val sweepAngle = (amount / total * 360).toFloat()

                // Draw the arc
                drawArc(
                    color = getCategoryColor(category),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = strokeWidth)
                )

                startAngle += sweepAngle
            }

            // Draw inner circle (white hole)
            drawCircle(
                color = Color.White,
                radius = innerRadius,
                center = center
            )
        }

        // Display total amount in the center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            Text(
                text = "Ksh.${totalSpent.roundToDecimalPlaces(2)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
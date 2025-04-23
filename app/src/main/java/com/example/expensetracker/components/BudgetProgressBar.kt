import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.example.expensetracker.helpers.calculateColor

@Composable
fun BudgetProgressBar(
    totalBudget: Double,
    totalRemainingBudget: Double
) {
    val remainingPercentage = (totalRemainingBudget / totalBudget).coerceIn(0.0, 1.0)

    LinearProgressIndicator(
        progress = { remainingPercentage.toFloat() },
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = calculateColor(totalRemainingBudget, totalBudget),
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
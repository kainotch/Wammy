import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun TestAnimate() {
    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
        item {
            Box(Modifier.animateItem())
        }
    }
}

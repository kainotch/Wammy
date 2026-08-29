import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

# Add missing imports for NavigationSystem
imports = """import androidx.compose.ui.geometry.Offset
import com.example.wammy.ui.reader.navigation.NavigationLayouts
import com.example.wammy.ui.reader.navigation.TapAction
import com.example.wammy.ui.reader.navigation.TapRegion"""

if "import com.example.wammy.ui.reader.navigation.TapAction" not in text:
    text = text.replace("import androidx.compose.ui.Alignment", imports + "\nimport androidx.compose.ui.Alignment")

# Remove duplicated variable block
duplicate_block_start = """    val prefs = com.example.wammy.AppContainer.readerPreferences"""
duplicate_block_end = """        else -> androidx.compose.ui.layout.ContentScale.Fit
    }"""
    
# We just find the first occurrence of the block and replace the second occurrence (or vice versa)
# Let's find all occurrences
count = text.count(duplicate_block_start)
if count > 1:
    # Use regex to replace only the first occurrence
    pattern = re.compile(r'    val prefs = com\.example\.wammy\.AppContainer\.readerPreferences.*?else -> androidx\.compose\.ui\.layout\.ContentScale\.Fit\n    \}', re.DOTALL)
    text = pattern.sub("", text, count=1)

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)


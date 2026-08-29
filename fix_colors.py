import re

with open('app/src/main/java/com/example/wammy/ui/screens/DetailsScreen.kt', 'r') as f:
    text = f.read()

# Replace TopBar text and icons
text = text.replace('Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)', 
                    'Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)')
text = text.replace('Text("Detail", color = Color.White', 
                    'Text("Detail", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground')
text = text.replace('Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray)',
                    'Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)')

# Replace Title
text = text.replace('color = Color.White,', 
                    'color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,')

# Replace Status & Source
text = text.replace('color = Color.LightGray, fontSize = 13.sp', 
                    'color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp')
text = text.replace('tint = Color.Gray, modifier = Modifier.size(14.dp)', 
                    'tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp)')

# Replace Tags
text = text.replace('Text(genre, color = Color.LightGray, fontSize = 12.sp)', 
                    'Text(genre, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)')

# Replace Action Buttons (Leave these hardcoded white/light gray because the button backgrounds are hardcoded dark 0xFF22222E!)
# Actually wait! The action buttons in the image are dark with white text. That's fine.
# But let's check Introduction text:
text = text.replace('Text("Introduction", color = Color.White', 
                    'Text("Introduction", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground')

# Description text
text = text.replace('Text(text = description, color = Color.LightGray', 
                    'Text(text = description, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant')
text = text.replace('Text(if (isDescriptionExpanded) "Collapse" else "Expand", color = Color.LightGray', 
                    'Text(if (isDescriptionExpanded) "Collapse" else "Expand", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant')
text = text.replace('tint = Color.LightGray,', 
                    'tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,')

# Contents Text
text = text.replace('Text("Contents", color = Color.White', 
                    'Text("Contents", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground')
text = text.replace('Text("Hide Read", color = Color.LightGray', 
                    'Text("Hide Read", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant')
text = text.replace('color = Color.Gray, strokeWidth = 2.dp', 
                    'color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, strokeWidth = 2.dp')

# Read button icon and text
# Actually read button background is Primary purple. So text should stay White!
# I will only replace specific things.

with open('app/src/main/java/com/example/wammy/ui/screens/DetailsScreen.kt', 'w') as f:
    f.write(text)


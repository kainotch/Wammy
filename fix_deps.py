import re

with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

text = text.replace('implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")',
                    'implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")\n  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.8.0")')

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)

import re

with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

replacement = """    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // Automatically sign release builds with debug key for easy testing
        }
    }"""

text = re.sub(r'    buildTypes\s*\{\s*release\s*\{\s*isMinifyEnabled\s*=\s*false\s*proguardFiles\(getDefaultProguardFile\("proguard-android-optimize.txt"\),\s*"proguard-rules.pro"\)\s*\}\s*\}', replacement, text)

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)

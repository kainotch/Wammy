import re

with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

bad_block = """    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "wammy.apk"
        }
    }"""

text = text.replace(bad_block, "")
text = text.replace('versionName = "1.0"', 'versionName = "1.0"\n        setProperty("archivesBaseName", "wammy")')

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)

import re

with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

rename_block = """
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "wammy.apk"
        }
    }
}"""

text = text.replace('    packaging {\n      resources {\n        excludes += "/META-INF/{AL2.0,LGPL2.1}"\n      }\n    }\n}', '    packaging {\n      resources {\n        excludes += "/META-INF/{AL2.0,LGPL2.1}"\n      }\n    }\n' + rename_block)

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)


import re

with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

text = text.replace('        setProperty("archivesBaseName", "wammy")\n', '')
text += '\nbase {\n    archivesName.set("wammy")\n}\n'

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)

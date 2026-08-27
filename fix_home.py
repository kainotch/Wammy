import re

with open('app/src/main/java/com/example/wammy/ui/HomeViewModel.kt', 'r') as f:
    text = f.read()

old_ext_jobs = """                val extJobs = AppContainer.extensionManager.activeSources.map { source ->"""
new_ext_jobs = """                val extJobs = AppContainer.extensionManager.activeSources
                    .filter { _pinnedMangaSources.value.contains(it.id.toString()) }
                    .map { source ->"""

text = text.replace(old_ext_jobs, new_ext_jobs)

with open('app/src/main/java/com/example/wammy/ui/HomeViewModel.kt', 'w') as f:
    f.write(text)

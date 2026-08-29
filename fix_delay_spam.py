import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# Swap delay and updatePageState to prevent worker thread-stealing spam
old_catch = """                            updatePageState(targetPage!!.index, PageState.ERROR)
                            kotlinx.coroutines.delay(2000) // wait before retry
                        }"""

new_catch = """                            kotlinx.coroutines.delay(2000) // Cooldown BEFORE marking as ERROR so other workers don't steal and spam it instantly
                            updatePageState(targetPage!!.index, PageState.ERROR)
                        }"""

text = text.replace(old_catch, new_catch)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)


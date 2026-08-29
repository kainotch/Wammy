import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# Fix priority to treat QUEUE and ERROR pages equally based on distance to the user's viewport
old_priority = """                            targetPage = currentList
                                .filter { it.state == PageState.QUEUE }
                                .minByOrNull { kotlin.math.abs(it.index - currentPageIndex) }
                                ?: if (currentList.all { it.state == PageState.READY || it.state == PageState.ERROR }) {
                                    currentList
                                        .filter { it.state == PageState.ERROR }
                                        .minByOrNull { kotlin.math.abs(it.index - currentPageIndex) }
                                } else null"""

new_priority = """                            targetPage = currentList
                                .filter { it.state == PageState.QUEUE || it.state == PageState.ERROR }
                                .minByOrNull { kotlin.math.abs(it.index - currentPageIndex) }"""

text = text.replace(old_priority, new_priority)

# Reduce workers to 2 to prevent 429 Too Many Requests from strict servers
text = text.replace("val workers = List(3) {", "val workers = List(2) {")

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)


import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

# 1. Remove ERROR from the automatic queue fetch to prevent infinite loops locking the queue
old_filter = """.filter { it.state == PageState.QUEUE || it.state == PageState.ERROR }"""
new_filter = """.filter { it.state == PageState.QUEUE }"""
text = text.replace(old_filter, new_filter)

# 2. Add retryPage function
new_fun = """    fun retryPage(index: Int) {
        _pages.update { currentPages ->
            currentPages.map { page ->
                if (page.index == index && page.state == PageState.ERROR) {
                    page.copy(state = PageState.QUEUE)
                } else page
            }
        }
    }

    fun updatePageState(index: Int, newState: PageState) {"""
text = text.replace("    fun updatePageState(index: Int, newState: PageState) {", new_fun)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)


import re

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("val sidePadding by viewModel.webtoonSidePadding.collectAsState()", "val sidePadding by com.example.wammy.AppContainer.readerPreferences.webtoonSidePadding.state.collectAsState()")

with open('app/src/main/java/com/example/wammy/ui/screens/ReaderScreen.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    vm_text = f.read()

vm_text = re.sub(r'private val _webtoonSidePadding = MutableStateFlow.*?asStateFlow\(\)', '', vm_text, flags=re.DOTALL)
vm_text = re.sub(r'fun setWebtoonSidePadding.*?\}', '', vm_text, flags=re.DOTALL)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(vm_text)


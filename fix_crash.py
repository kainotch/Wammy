import re

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'r') as f:
    text = f.read()

old_code = """                            if (targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl == null) {
                                var parsed = source?.getImageUrl(targetPage!!.sourcePage!!)
                                if (parsed != null && !parsed.startsWith("http")) {
                                    parsed = (source?.baseUrl ?: "") + if (parsed.startsWith("/")) parsed else "/$parsed"
                                }
                                targetPage!!.sourcePage!!.imageUrl = parsed
                            }

                            var response: okhttp3.Response? = null
                            try {"""

new_code = """                            var response: okhttp3.Response? = null
                            try {
                                if (targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl == null) {
                                    var parsed = source?.getImageUrl(targetPage!!.sourcePage!!)
                                    if (parsed != null && !parsed.startsWith("http")) {
                                        parsed = (source?.baseUrl ?: "") + if (parsed.startsWith("/")) parsed else "/$parsed"
                                    }
                                    targetPage!!.sourcePage!!.imageUrl = parsed
                                }"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/wammy/ui/ReaderViewModel.kt', 'w') as f:
    f.write(text)


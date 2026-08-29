import urllib.request
import re

url = 'https://www.instagram.com/kainotch/'
req = urllib.request.Request(
    url, 
    data=None, 
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    }
)

try:
    with urllib.request.urlopen(req) as response:
        html = response.read().decode('utf-8')
        match = re.search(r'<meta property="og:image" content="([^"]+)"', html)
        if match:
            print("FOUND: " + match.group(1))
        else:
            print("No og:image found.")
            print(html[:500])
except Exception as e:
    print(f"Error: {e}")

from curl_cffi import requests

targets = ['chrome110', 'chrome116', 'chrome120', 'edge101', 'safari15_5']
url = 'https://api.phim4k.lol/rest-api/all_genre'
headers = {'X-API-KEY': 'bbbb411dea44849'}

for imp in targets:
    try:
        r = requests.get(url, headers=headers, impersonate=imp, timeout=10)
        is_json = r.text[:1] in ['[', '{'] and 'Cloudflare' not in r.text[:100]
        label = 'JSON OK!' if is_json else 'BLOCKED'
        print(f'{imp}: {r.status_code} - {label}')
        if is_json:
            print(r.text[:200])
            break
    except Exception as e:
        print(f'{imp}: ERROR - {e}')

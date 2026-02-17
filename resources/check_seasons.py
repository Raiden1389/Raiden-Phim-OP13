import json, urllib.request, re

def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    return json.loads(urllib.request.urlopen(req).read())

# Test nhiều phim bộ multi-season
shows = ["the walking dead", "game of thrones", "breaking bad", "one piece", "attack on titan", "money heist"]

for show in shows:
    try:
        data = fetch(f"https://ophim1.com/v1/api/tim-kiem?keyword={show.replace(' ', '+')}")
        items = data.get("data", {}).get("items", [])
        
        # Filter items that look like seasons
        seasons = [i for i in items if re.search(r'[Pp]h[aầ]n\s*\d|[Ss]eason\s*\d|[Mm]ùa\s*\d', i["name"])]
        
        if seasons:
            print(f"\n🎬 {show.upper()} — {len(seasons)} seasons found:")
            for s in sorted(seasons, key=lambda x: x.get("year", "")):
                print(f"   {s['slug']} | {s['name']}")
        else:
            print(f"\n🎬 {show.upper()} — {len(items)} results, no multi-season pattern")
            for i in items[:3]:
                print(f"   {i['slug']} | {i['name']}")
    except Exception as e:
        print(f"\n🎬 {show.upper()} — ERROR: {e}")

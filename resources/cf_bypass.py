"""
Phim4K API — Cloudflare Bypass via Playwright (real browser)
Mở Chrome thật, giải CF challenge, rồi dùng cookies để gọi API
"""
import asyncio
import json
from playwright.async_api import async_playwright

API_BASE = "https://api.phim4k.lol/rest-api"
HEADERS = {"X-API-KEY": "bbbb411dea44849"}

async def main():
    async with async_playwright() as p:
        # Launch real Chrome (non-headless to solve CF challenge)
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        )
        page = await context.new_page()

        # 1) Navigate to API — CF might show challenge
        print("[1] Opening API page (may show CF challenge)...")
        await page.goto(f"{API_BASE}/all_genre", wait_until="networkidle", timeout=30000)
        
        # Wait for CF to resolve (up to 15s)
        await page.wait_for_timeout(5000)
        
        content = await page.content()
        if "Cloudflare" in content and "blocked" in content.lower():
            print("[!] Still blocked — waiting for manual solve or JS challenge...")
            await page.wait_for_timeout(10000)
            content = await page.content()
        
        # 2) Check if we got through
        body = await page.inner_text("body")
        if body.startswith("[") or body.startswith("{"):
            print("[✅] API RESPONSE:")
            data = json.loads(body)
            print(json.dumps(data[:3] if isinstance(data, list) else data, indent=2, ensure_ascii=False))
            
            # 3) Get cookies for reuse
            cookies = await context.cookies()
            cf_cookies = [c for c in cookies if "cf_" in c["name"].lower() or "clearance" in c["name"].lower()]
            print(f"\n[🍪] CF Cookies: {json.dumps(cf_cookies, indent=2)}")
            
            # 4) Try other endpoints
            for endpoint in ["/movies?page=1", "/home_content_for_android"]:
                resp = await page.goto(f"{API_BASE}{endpoint}", wait_until="networkidle")
                body2 = await page.inner_text("body")
                if body2.startswith("[") or body2.startswith("{"):
                    data2 = json.loads(body2)
                    preview = data2[:2] if isinstance(data2, list) else {k: str(v)[:50] for k, v in list(data2.items())[:5]}
                    print(f"\n[✅] {endpoint}: {json.dumps(preview, indent=2, ensure_ascii=False)}")
                else:
                    print(f"\n[❌] {endpoint}: Blocked")
        else:
            print("[❌] Blocked by Cloudflare")
            print(body[:300])
        
        await browser.close()

asyncio.run(main())

// Test share_link from different domains via Node.js fetch
async function main() {
    const domains = [
        'https://www.showbox.media',
        'https://showbox.media',
        'https://showbox.shegu.net',
    ];

    const headers = {
        'Accept': 'application/json',
        'Accept-Language': 'en',
        'User-Agent': 'okhttp/3.2.0',
    };

    for (const domain of domains) {
        const url = `${domain}/index/share_link?id=92&type=2`;
        console.log(`Testing: ${url}`);
        try {
            const r = await fetch(url, { headers, redirect: 'follow', signal: AbortSignal.timeout(10000) });
            console.log(`  Status: ${r.status}`);
            if (r.ok) {
                const data = await r.json();
                console.log(`  ✅ Data:`, JSON.stringify(data));
            } else {
                const text = await r.text();
                console.log(`  Body (first 200):`, text.substring(0, 200));
            }
        } catch (e) {
            console.log(`  ❌ Error: ${e.message}`);
        }
    }

    // Also try febbox.com directly for share link
    console.log('\nTesting FebBox share_link:');
    try {
        const r = await fetch('https://www.febbox.com/index/share_link?id=92&type=2', { headers });
        console.log(`  Status: ${r.status}`);
        const text = await r.text();
        console.log(`  Body: ${text.substring(0, 300)}`);
    } catch (e) { console.log(`  Error: ${e.message}`); }
}

main().catch(console.error);

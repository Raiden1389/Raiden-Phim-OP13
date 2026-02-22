/**
 * Test FebBox Direct Pipeline — Fixed with is_html=1
 * 
 * Endpoints discovered from browser inspection:
 *   1. showbox.media/index/share_link → share_key ✅
 *   2. www.febbox.com/file/file_share_list?share_key=X&is_html=1 → file list
 *   3. www.febbox.com/file/file_info?fid=X → file details
 *   4. www.febbox.com/file/share_download?share_key=X → stream URLs
 */

var COOKIE = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3NzE2OTQ2MDIsIm5iZiI6MTc3MTY5NDYwMiwiZXhwIjoxODAyNzk4NjIyLCJkYXRhIjp7InVpZCI6MTQ1MjE3OCwidG9rZW4iOiJiM2EyYWYxNzA0MDI5NjI2NzA0Njc4OTYxMWYwMThkNSJ9fQ.oFssqGTDyS6EC2zc_QsIjHdtd1bWf9CoP8zFh0y5LBc';

process.on('unhandledRejection', function (e) { console.log('UNHANDLED:', e); });

function makeHeaders(shareKey) {
    return {
        'Cookie': 'ui=' + COOKIE,
        'Accept': 'application/json, text/html, */*',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://www.febbox.com/share/' + (shareKey || ''),
        'X-Requested-With': 'XMLHttpRequest'
    };
}

async function getShareKey(showboxId, type) {
    var url = 'https://showbox.media/index/share_link?id=' + showboxId + '&type=' + type;
    console.log('[1] share_link:', url);
    var r = await fetch(url, {
        headers: { 'User-Agent': 'okhttp/3.2.0', 'Accept-Language': 'en' },
        signal: AbortSignal.timeout(10000)
    });
    var d = await r.json();
    var shareKey = d.data.link.split('/share/')[1];
    console.log('[1] ✅ Share Key:', shareKey);
    return shareKey;
}

async function getFileList(shareKey, parentId) {
    parentId = parentId || 0;
    // KEY FIX: is_html=1
    var url = 'https://www.febbox.com/file/file_share_list?share_key=' + shareKey + '&pwd=&parent_id=' + parentId + '&is_html=1';
    console.log('[2] file_share_list (parentId=' + parentId + ')');
    var r = await fetch(url, {
        headers: makeHeaders(shareKey),
        signal: AbortSignal.timeout(15000)
    });
    console.log('[2] Status:', r.status);
    var text = await r.text();

    // Try parse as JSON
    try {
        var data = JSON.parse(text);
        console.log('[2] ✅ JSON response, code:', data.code);

        // Parse HTML to extract file info
        if (data.html) {
            console.log('[2] HTML length:', data.html.length);
            // Extract file names and fids from HTML
            var fidMatches = data.html.match(/data-fid="(\d+)"/g) || [];
            var nameMatches = data.html.match(/file_name"[^>]*>([^<]+)</g) || [];
            console.log('[2] Found', fidMatches.length, 'files');

            // Also try regex for fid and filename pairs
            var fileRegex = /fid[=:]["']?(\d+)["']?/g;
            var fids = [];
            var match;
            while ((match = fileRegex.exec(data.html)) !== null) {
                fids.push(match[1]);
            }
            console.log('[2] File IDs:', fids.slice(0, 10).join(', '));
            console.log('[2] HTML snippet:', data.html.substring(0, 500));
        }
        return data;
    } catch (e) {
        console.log('[2] Not JSON, body:', text.substring(0, 300));
        return null;
    }
}

async function getFileInfo(fid) {
    var url = 'https://www.febbox.com/file/file_info?fid=' + fid;
    console.log('[3] file_info fid=' + fid);
    var r = await fetch(url, {
        headers: makeHeaders(''),
        signal: AbortSignal.timeout(10000)
    });
    console.log('[3] Status:', r.status);
    var text = await r.text();
    try {
        var data = JSON.parse(text);
        console.log('[3] ✅', JSON.stringify(data).substring(0, 400));
        return data;
    } catch (e) {
        console.log('[3] Body:', text.substring(0, 300));
        return null;
    }
}

async function getDownloadLinks(shareKey, fid) {
    var url = 'https://www.febbox.com/file/share_download?share_key=' + shareKey + '&fid=' + fid;
    console.log('[4] share_download shareKey=' + shareKey + ' fid=' + fid);
    var r = await fetch(url, {
        headers: makeHeaders(shareKey),
        signal: AbortSignal.timeout(15000)
    });
    console.log('[4] Status:', r.status);
    var text = await r.text();
    try {
        var data = JSON.parse(text);
        console.log('[4] ✅', JSON.stringify(data).substring(0, 600));
        return data;
    } catch (e) {
        console.log('[4] Body:', text.substring(0, 300));
        return null;
    }
}

async function main() {
    console.log('=== FebBox Direct Pipeline Test (Fixed) ===');
    console.log('Time:', new Date().toLocaleString('vi-VN'));
    console.log('');

    try {
        // Step 1: Get share key
        var shareKey = await getShareKey(92, 2); // Two and a Half Men (TV)

        // Step 2: Get root file list (seasons)
        console.log('\n--- Root folder (seasons) ---');
        var rootData = await getFileList(shareKey, 0);

        // Step 2b: Get Season 1 files  
        // We know from browser: Season 1 parent_id will be in the HTML
        // Let's try with known parent_id from browser inspection
        console.log('\n--- Season 12 (parent_id=2633059 from browser) ---');
        var seasonData = await getFileList(shareKey, 2633059);

        // Step 3: Get first file info
        console.log('\n--- File info ---');
        await getFileInfo(2633075);

        // Step 4: Get download links
        console.log('\n--- Download links ---');
        await getDownloadLinks(shareKey, 2633075);

    } catch (e) {
        console.log('❌ Error:', e.message);
        console.log(e.stack);
    }

    console.log('\n=== Done ===');
}

main();

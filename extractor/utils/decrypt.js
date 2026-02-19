/**
 * Utility functions ported from vidsrc-me-resolver
 * https://github.com/Ciarands/vidsrc-me-resolver
 */

/**
 * XOR decode: hex-encoded string XOR'd with seed
 */
function decodeSrc(encoded, seed) {
    const buffer = Buffer.from(encoded, 'hex');
    let decoded = '';
    for (let i = 0; i < buffer.length; i++) {
        decoded += String.fromCharCode(buffer[i] ^ seed.charCodeAt(i % seed.length));
    }
    return decoded;
}

/**
 * Hunter unpacker (used by Superembed/Multiembed)
 */
function hunter(h, u, n, t, e, r) {
    const charset = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+/';

    function hunterDef(d, e, f) {
        const sourceBase = charset.substring(0, e);
        const targetBase = charset.substring(0, f);
        const reversedInput = d.split('').reverse();
        let result = 0;

        for (let power = 0; power < reversedInput.length; power++) {
            const digit = reversedInput[power];
            if (sourceBase.includes(digit)) {
                result += sourceBase.indexOf(digit) * Math.pow(e, power);
            }
        }

        let convertedResult = '';
        while (result > 0) {
            convertedResult = targetBase[result % f] + convertedResult;
            result = Math.floor((result - (result % f)) / f);
        }

        return parseInt(convertedResult) || 0;
    }

    let i = 0;
    let resultStr = '';

    while (i < h.length) {
        let s = '';
        while (h[i] !== n[e]) {
            s += h[i];
            i++;
        }

        for (let j = 0; j < n.length; j++) {
            s = s.split(n[j]).join(j.toString());
        }

        resultStr += String.fromCharCode(hunterDef(s, e, 10) - t);
        i++;
    }

    return resultStr;
}

/**
 * Decode base64 URL-safe string
 */
function decodeBase64UrlSafe(s) {
    const standardized = s.replace(/_/g, '/').replace(/-/g, '+');
    return Buffer.from(standardized, 'base64');
}

/**
 * Decode HLS URL (VidSrc PRO format)
 */
function decodeHlsUrl(encodedUrl) {
    function formatHlsB64(data) {
        const cleaned = data.replace(/\/@#@\/[^=\/]+==/g, '');
        if (/\/@#@\/[^=\/]+==/g.test(cleaned)) {
            return formatHlsB64(cleaned);
        }
        return cleaned;
    }

    const formattedB64 = formatHlsB64(encodedUrl.substring(2));
    const b64Data = decodeBase64UrlSafe(formattedB64);
    return b64Data.toString('utf-8');
}

/**
 * Process hunter function arguments from eval()
 */
function processHunterArgs(argsStr) {
    const match = argsStr.match(/^"(.*?)",(.*?),"(.*?)",(.*?),(.*?),(.*?)$/);
    if (!match) return null;
    return [
        match[1],
        parseInt(match[2]),
        match[3],
        parseInt(match[4]),
        parseInt(match[5]),
        parseInt(match[6])
    ];
}

module.exports = {
    decodeSrc,
    hunter,
    decodeBase64UrlSafe,
    decodeHlsUrl,
    processHunterArgs
};

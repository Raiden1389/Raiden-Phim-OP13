const vscode = require('vscode');
const { exec } = require('child_process');
const os = require('os');

// ═══════════════════════════════════════════════════════════
// 🐕 RAM Watchdog — Monitor IDE memory usage
// ═══════════════════════════════════════════════════════════

/** @type {vscode.StatusBarItem} */
let statusBarItem;
/** @type {NodeJS.Timeout|null} */
let updateTimer = null;
/** @type {Map<string, boolean>} track which alerts already shown */
const alertedProcesses = new Map();

// Process names we care about (IDE-related heavy hitters)
const WATCHED_PROCESSES = [
    { match: 'language_server', label: 'Kotlin LS' },
    { match: 'kotlin-language-server', label: 'Kotlin LS' },
    { match: 'OpenJDK Platform', label: 'Gradle/JDK' },
    { match: 'java.exe', label: 'Java/Gradle' },
    { match: 'javaw.exe', label: 'Java/Gradle' },
    { match: 'node.exe', label: 'Node.js' },
    { match: 'typescript-language', label: 'TypeScript LS' },
    { match: 'tsserver', label: 'TS Server' },
    { match: 'eslint', label: 'ESLint' },
    { match: 'rust-analyzer', label: 'Rust Analyzer' },
    { match: 'clangd', label: 'Clangd' },
    { match: 'gopls', label: 'Go LS' },
    { match: 'pylsp', label: 'Python LS' },
    { match: 'Cursor', label: 'Cursor IDE' },
    { match: 'Code.exe', label: 'VS Code' },
    { match: 'code.exe', label: 'VS Code' },
    { match: 'cursor.exe', label: 'Cursor IDE' },
    { match: 'electron', label: 'IDE Core' },
    { match: 'rg.exe', label: 'ripgrep' },
    { match: 'copilot', label: 'Copilot' },
    { match: 'extensionHost', label: 'Extension Host' },
];

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
    // Status bar item — right side, high priority
    statusBarItem = vscode.window.createStatusBarItem(
        vscode.StatusBarAlignment.Left, -100
    );
    statusBarItem.command = 'ramWatchdog.showDetails';
    statusBarItem.tooltip = 'Click to see RAM details';
    statusBarItem.show();
    context.subscriptions.push(statusBarItem);

    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('ramWatchdog.showDetails', showDetailsPanel),
        vscode.commands.registerCommand('ramWatchdog.killProcess', killProcessPicker),
        vscode.commands.registerCommand('ramWatchdog.refresh', () => updateRAMStatus()),
    );

    // Initial update
    updateRAMStatus();

    // Periodic updates
    const config = vscode.workspace.getConfiguration('ramWatchdog');
    const intervalSec = config.get('updateInterval', 10);
    updateTimer = setInterval(updateRAMStatus, intervalSec * 1000);

    // React to config changes
    context.subscriptions.push(
        vscode.workspace.onDidChangeConfiguration(e => {
            if (e.affectsConfiguration('ramWatchdog.updateInterval')) {
                if (updateTimer) clearInterval(updateTimer);
                const newInterval = vscode.workspace.getConfiguration('ramWatchdog')
                    .get('updateInterval', 10);
                updateTimer = setInterval(updateRAMStatus, newInterval * 1000);
            }
        })
    );
}

// ═══════════════════════════════════════════════════════════
// Get process list from OS
// ═══════════════════════════════════════════════════════════

/**
 * @typedef {{ pid: number, name: string, label: string, memMB: number }} ProcessInfo
 */

/** @returns {Promise<ProcessInfo[]>} */
function getProcesses() {
    return new Promise((resolve) => {
        const isWin = os.platform() === 'win32';

        const cmd = isWin
            ? 'tasklist /FO CSV /NH'
            : 'ps aux --sort=-rss';

        exec(cmd, { maxBuffer: 1024 * 1024 * 10 }, (err, stdout) => {
            if (err) { resolve([]); return; }

            const processes = [];
            const lines = stdout.trim().split('\n');

            for (const line of lines) {
                try {
                    if (isWin) {
                        // CSV format: "name.exe","PID","Session","Session#","Mem Usage"
                        const parts = line.match(/"([^"]+)"/g);
                        if (!parts || parts.length < 5) continue;

                        const name = parts[0].replace(/"/g, '');
                        const pid = parseInt(parts[1].replace(/"/g, ''));
                        const memStr = parts[4].replace(/"/g, '').replace(/[^0-9]/g, '');
                        const memKB = parseInt(memStr);
                        if (isNaN(memKB) || isNaN(pid)) continue;

                        const memMB = Math.round(memKB / 1024);

                        // Check if this is a watched process
                        const watched = WATCHED_PROCESSES.find(wp =>
                            name.toLowerCase().includes(wp.match.toLowerCase())
                        );
                        if (watched && memMB > 10) {
                            processes.push({ pid, name, label: watched.label, memMB });
                        }
                    } else {
                        // ps aux format
                        const parts = line.trim().split(/\s+/);
                        if (parts.length < 11) continue;

                        const pid = parseInt(parts[1]);
                        const rss = parseInt(parts[5]); // RSS in KB
                        const name = parts.slice(10).join(' ');
                        const memMB = Math.round(rss / 1024);

                        const watched = WATCHED_PROCESSES.find(wp =>
                            name.toLowerCase().includes(wp.match.toLowerCase())
                        );
                        if (watched && memMB > 10) {
                            processes.push({ pid, name: parts[10], label: watched.label, memMB });
                        }
                    }
                } catch { /* skip bad lines */ }
            }

            // Sort by memory descending
            processes.sort((a, b) => b.memMB - a.memMB);

            // Merge same-label processes (e.g., multiple node.exe)
            const merged = new Map();
            for (const p of processes) {
                const key = `${p.label}`;
                if (merged.has(key)) {
                    const existing = merged.get(key);
                    existing.memMB += p.memMB;
                    existing.count = (existing.count || 1) + 1;
                    existing.pids.push(p.pid);
                } else {
                    merged.set(key, { ...p, count: 1, pids: [p.pid] });
                }
            }

            resolve(Array.from(merged.values()).sort((a, b) => b.memMB - a.memMB));
        });
    });
}

// ═══════════════════════════════════════════════════════════
// Update Status Bar
// ═══════════════════════════════════════════════════════════

async function updateRAMStatus() {
    const processes = await getProcesses();
    if (processes.length === 0) {
        statusBarItem.text = '$(pulse) RAM: --';
        return;
    }

    const totalMB = processes.reduce((sum, p) => sum + p.memMB, 0);
    const totalGB = (totalMB / 1024).toFixed(1);
    const config = vscode.workspace.getConfiguration('ramWatchdog');
    const warningMB = config.get('warningThresholdMB', 2048);
    const criticalMB = config.get('criticalThresholdMB', 4096);
    const totalWarningMB = config.get('totalWarningMB', 8192);
    const enableNotifications = config.get('enableNotifications', true);

    // Status bar icon + color based on severity
    let icon, bgColor;
    if (totalMB > totalWarningMB) {
        icon = '$(flame)';
        bgColor = new vscode.ThemeColor('statusBarItem.errorBackground');
    } else if (totalMB > totalWarningMB * 0.7) {
        icon = '$(warning)';
        bgColor = new vscode.ThemeColor('statusBarItem.warningBackground');
    } else {
        icon = '$(dashboard)';
        bgColor = undefined;
    }

    // Top consumer label
    const top = processes[0];
    const topLabel = `${top.label}: ${formatMB(top.memMB)}`;

    statusBarItem.text = `${icon} ${totalGB} GB — ${topLabel}`;
    statusBarItem.backgroundColor = bgColor;

    // Build detailed tooltip
    const tooltipLines = [
        `🐕 RAM Watchdog`,
        `─────────────────────`,
        `Total IDE RAM: ${totalGB} GB`,
        `System RAM: ${(os.totalmem() / 1024 / 1024 / 1024).toFixed(0)} GB`,
        `Free RAM: ${(os.freemem() / 1024 / 1024 / 1024).toFixed(1)} GB`,
        ``,
        `📊 Top processes:`,
    ];

    for (const p of processes.slice(0, 8)) {
        const bar = getBar(p.memMB, 5000);
        const countStr = p.count > 1 ? ` (×${p.count})` : '';
        tooltipLines.push(`  ${bar} ${p.label}${countStr}: ${formatMB(p.memMB)}`);
    }

    tooltipLines.push(``, `Click for full details`);
    statusBarItem.tooltip = tooltipLines.join('\n');

    // ═══ Notifications ═══
    if (enableNotifications) {
        // Per-process alerts
        for (const p of processes) {
            const alertKey = `${p.label}-${p.memMB > criticalMB ? 'critical' : 'warning'}`;
            if (p.memMB > criticalMB && !alertedProcesses.has(alertKey)) {
                alertedProcesses.set(alertKey, true);
                const action = await vscode.window.showWarningMessage(
                    `🔴 ${p.label} đang ngốn ${formatMB(p.memMB)} RAM!`,
                    'Kill Process', 'Ignore', 'Disable Alerts'
                );
                handleAlertAction(action, p);
            } else if (p.memMB > warningMB && !alertedProcesses.has(alertKey)) {
                alertedProcesses.set(alertKey, true);
                const action = await vscode.window.showInformationMessage(
                    `⚠️ ${p.label} đang dùng ${formatMB(p.memMB)} RAM`,
                    'Kill Process', 'Dismiss'
                );
                handleAlertAction(action, p);
            }
        }

        // Total alert
        if (totalMB > totalWarningMB && !alertedProcesses.has('total')) {
            alertedProcesses.set('total', true);
            vscode.window.showWarningMessage(
                `🔥 Tổng RAM IDE: ${totalGB} GB! Consider closing unused files/extensions.`,
                'Show Details'
            ).then(action => {
                if (action === 'Show Details') showDetailsPanel();
            });
        }
    }
}

/**
 * @param {string|undefined} action
 * @param {ProcessInfo & {pids: number[]}} process
 */
function handleAlertAction(action, process) {
    if (action === 'Kill Process') {
        killPids(process.pids, process.label);
    } else if (action === 'Disable Alerts') {
        vscode.workspace.getConfiguration('ramWatchdog')
            .update('enableNotifications', false, vscode.ConfigurationTarget.Global);
    }
}

// ═══════════════════════════════════════════════════════════
// Details Panel (Webview)
// ═══════════════════════════════════════════════════════════

async function showDetailsPanel() {
    const processes = await getProcesses();
    const totalMB = processes.reduce((sum, p) => sum + p.memMB, 0);
    const totalGB = (totalMB / 1024).toFixed(1);
    const systemTotalGB = (os.totalmem() / 1024 / 1024 / 1024).toFixed(0);
    const freeGB = (os.freemem() / 1024 / 1024 / 1024).toFixed(1);
    const usedPct = Math.round((1 - os.freemem() / os.totalmem()) * 100);

    // Create QuickPick items
    const items = [
        {
            label: `$(flame) Total IDE RAM: ${totalGB} GB`,
            description: `System: ${usedPct}% used (${freeGB} GB free / ${systemTotalGB} GB)`,
            kind: vscode.QuickPickItemKind.Separator
        },
    ];

    for (const p of processes) {
        const bar = getBar(p.memMB, 5000);
        const countStr = p.count > 1 ? ` (×${p.count})` : '';
        const severity = p.memMB > 4096 ? '🔴' : p.memMB > 2048 ? '🟡' : '🟢';

        items.push({
            label: `${severity} ${p.label}${countStr}`,
            description: `${formatMB(p.memMB)}  ${bar}`,
            detail: `PID: ${p.pids.join(', ')} — ${p.name}`,
            pids: p.pids,
            processLabel: p.label,
        });
    }

    items.push({
        label: '',
        kind: vscode.QuickPickItemKind.Separator
    });
    items.push({
        label: '$(trash) Kill a process...',
        description: 'Select a process above, then use this to kill it',
        isKillAction: true,
    });

    const pick = await vscode.window.showQuickPick(items, {
        title: '🐕 RAM Watchdog — Process Monitor',
        placeHolder: 'Select a process to kill, or press Esc to close',
        matchOnDescription: true,
        matchOnDetail: true,
    });

    if (pick && pick.pids) {
        const confirm = await vscode.window.showWarningMessage(
            `Kill ${pick.processLabel}? (PID: ${pick.pids.join(', ')})`,
            { modal: true },
            'Kill'
        );
        if (confirm === 'Kill') {
            killPids(pick.pids, pick.processLabel);
        }
    } else if (pick && pick.isKillAction) {
        killProcessPicker();
    }
}

// ═══════════════════════════════════════════════════════════
// Kill Process
// ═══════════════════════════════════════════════════════════

async function killProcessPicker() {
    const processes = await getProcesses();

    const items = processes.map(p => ({
        label: `${p.memMB > 4096 ? '🔴' : p.memMB > 2048 ? '🟡' : '🟢'} ${p.label}`,
        description: formatMB(p.memMB),
        detail: `PID: ${p.pids.join(', ')}`,
        pids: p.pids,
        processLabel: p.label,
    }));

    const pick = await vscode.window.showQuickPick(items, {
        title: '🗡️ Kill Process',
        placeHolder: 'Which process to terminate?',
    });

    if (pick) {
        const confirm = await vscode.window.showWarningMessage(
            `Kill ${pick.processLabel}? This may affect IDE functionality.`,
            { modal: true },
            'Kill'
        );
        if (confirm === 'Kill') {
            killPids(pick.pids, pick.processLabel);
        }
    }
}

/**
 * @param {number[]} pids
 * @param {string} label
 */
function killPids(pids, label) {
    const isWin = os.platform() === 'win32';
    for (const pid of pids) {
        const cmd = isWin ? `taskkill /F /PID ${pid}` : `kill -9 ${pid}`;
        exec(cmd, (err) => {
            if (err) {
                vscode.window.showErrorMessage(`Failed to kill ${label} (PID ${pid}): ${err.message}`);
            }
        });
    }
    vscode.window.showInformationMessage(`✅ Killed ${label} (${pids.length} process${pids.length > 1 ? 'es' : ''})`);
    // Clear alerts for this process so it can re-alert if restarted
    alertedProcesses.clear();
    // Refresh after a brief delay
    setTimeout(updateRAMStatus, 2000);
}

// ═══════════════════════════════════════════════════════════
// Utilities
// ═══════════════════════════════════════════════════════════

function formatMB(mb) {
    if (mb >= 1024) return `${(mb / 1024).toFixed(1)} GB`;
    return `${mb} MB`;
}

function getBar(valueMB, maxMB) {
    const filled = Math.min(Math.round((valueMB / maxMB) * 8), 8);
    return '█'.repeat(filled) + '░'.repeat(8 - filled);
}

function deactivate() {
    if (updateTimer) {
        clearInterval(updateTimer);
        updateTimer = null;
    }
}

module.exports = { activate, deactivate };

"""
Capture Phim4K traffic by checking active connections
"""
import subprocess, json, re

# Find Phim4K process
result = subprocess.run(["tasklist", "/FI", "IMAGENAME eq Phim4K*", "/FO", "CSV"], capture_output=True, text=True)
print("=== Phim4K Processes ===")
print(result.stdout)

# Get all connections from Phim4K
result2 = subprocess.run(["netstat", "-ano"], capture_output=True, text=True)
lines = result2.stdout.strip().split("\n")

# Find PIDs of Phim4K
pids = subprocess.run(
    ["powershell", "-Command", "Get-Process | Where-Object {$_.ProcessName -match 'phim4k|Phim4K'} | Select-Object Id,ProcessName | ConvertTo-Json"],
    capture_output=True, text=True
)
print("\n=== Phim4K PIDs ===")
print(pids.stdout)

if pids.stdout.strip():
    try:
        procs = json.loads(pids.stdout)
        if isinstance(procs, dict):
            procs = [procs]
        pid_set = {str(p["Id"]) for p in procs}
        
        print(f"\n=== Network Connections (PIDs: {pid_set}) ===")
        for line in lines:
            parts = line.split()
            if len(parts) >= 5 and parts[-1] in pid_set:
                print(line.strip())
    except:
        print("Could not parse PIDs")

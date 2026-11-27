#!/usr/bin/env python3
"""Verify the seven bundled mesh buffers and, optionally, the APK contents."""
import argparse, array, base64, hashlib, json, math, pathlib, re, subprocess, sys, tempfile, zipfile

root = pathlib.Path(__file__).resolve().parents[1]
asset = root / 'app/src/main/assets/index.html'
audit = json.loads((root / 'docs/vehicle_mesh_audit.json').read_text())
parser = argparse.ArgumentParser()
parser.add_argument('--apk', type=pathlib.Path)
args = parser.parse_args()
raw = asset.read_bytes()
s = raw.decode('utf-8')
meshes = json.loads(re.search(r'<script id="builtInMeshes"[^>]*>(.*?)</script>', s, re.S).group(1))
assert set(meshes) == set(audit) and len(meshes) == 7, 'Missing vehicle mesh'
for name, mesh in meshes.items():
    body, wheel = [base64.b64decode(mesh[k], validate=True) for k in ('body', 'wheel')]
    for part in (body, wheel):
        assert part and len(part) % (11 * 4 * 3) == 0, name + ' invalid triangles'
        v = array.array('f'); v.frombytes(part)
        if sys.byteorder != 'little': v.byteswap()
        assert all(math.isfinite(f) for f in v), name + ' non-finite vertex'
    assert hashlib.sha256(body + wheel).hexdigest() == audit[name]['sha256'], name + ' hash mismatch'
    assert len(body) // 132 + len(wheel) // 132 * len(mesh['wheels']) == audit[name]['totalTriangles'], name + ' triangle count mismatch'
    print(f"PASS {name}: {audit[name]['totalTriangles']:,} triangles")
with tempfile.TemporaryDirectory() as td:
    js = pathlib.Path(td) / 'game.js'
    js.write_text('\n'.join(re.findall(r'<script>(.*?)</script>', s, re.S)))
    assert js.stat().st_size > 1000, 'Missing game script'
    subprocess.run(['node', '--check', str(js)], check=True)
if args.apk:
    with zipfile.ZipFile(args.apk) as apk:
        assert apk.testzip() is None, 'Corrupt APK'
        assert apk.read('assets/index.html') == raw, 'APK embedded HTML differs from source'
        assert 'classes.dex' in apk.namelist(), 'No Android executable in APK'
    print('PASS APK packaged asset is byte-identical')
print('PASS offline asset integrity and JavaScript syntax')

#!/usr/bin/env python3
from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[2]
p=root/'project/native/corresponding-sources/native-source-inventory.json'
if not p.exists():p=root/'.cache/source-distribution/native-source-inventory.json'
data=json.loads(p.read_text());work=root/'.cache/native-build'
for name,expected in data['source_files'].items():
    file=work/name.removeprefix('native-sources/')
    file.resolve().relative_to(work.resolve())
    assert hashlib.sha256(file.read_bytes()).hexdigest()==expected,f'Source changed/missing: {name}'
print('Bundled native source hashes verified:',len(data['source_files']))

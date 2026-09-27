#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,re
root=Path(__file__).resolve().parents[2];sources=root/'.cache/source-distribution'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
m=json.loads((root/'project/native/maven-runtime.lock.json').read_text())
assert not m['errors']
assert all(a['licenses'] and (a['sources_url'] or a['empty_binary']) for a in m['artifacts'])
assert not any(a['coordinate'].startswith('com.google.android.gms:') for a in m['artifacts'])
for a in m['artifacts']:
 assert all(any(word in l['name'].lower() for word in ['apache','mit','bsd','bouncy castle']) for l in a['licenses']),a['coordinate']
version=re.search(r'versionName = "([^"]+)"', (root/'project/app/build.gradle.kts').read_text()).group(1)
record={'version':version,'license':'GPL-3.0-or-later','native_source_strategy':'controlled pinned-source rebuild',
 'sources':{p.name:sha(p) for p in [sources/'native-sources.tar.xz',sources/'native-source-inventory.json',sources/'maven-sources.zip',sources/'androidx-native-sources.zip']},
 'source_lock_sha256':sha(root/'project/native/sources.lock.json'),
 'runtime_manifest_sha256':sha(root/'project/native/runtime-manifest.json'),
 'androidx_native_lock_sha256':sha(root/'project/native/androidx-native.lock.json'),
 'maven_lock_sha256':sha(root/'project/native/maven-runtime.lock.json'),
 'jni_source_sha256':sha(root/'project/native/jni/opticast_mpv.c'),
 'native_binary_check_report_sha256':sha(root/'.cache/native-runtime-check.log'),
 'review':{'maven_artifacts':len(m['artifacts']),'unused_proprietary_cronet_removed':True,
 'native_sources_provided':True,'runtime_sources_provided':True,'ndk_runtime':'unmodified standard NDK r28c runtime; System Library/toolchain with included notices',
 'formal_legal_certification':False,'phone_playback_tested':False}}
(root/'project/native/distribution-manifest.json').write_text(json.dumps(record,indent=2)+'\n')
print('Source distribution recorded:',record['review']['maven_artifacts'],'runtime artifacts.')

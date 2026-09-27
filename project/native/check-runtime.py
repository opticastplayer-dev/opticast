#!/usr/bin/env python3
"""Check every packaged ABI: binary hashes, ELF class/machine, dependencies and JNI exports."""
from pathlib import Path
import json,subprocess,re,hashlib,struct
root=Path(__file__).resolve().parents[2]
manifest=json.loads((root/'project/native/runtime-manifest.json').read_text())
bin=root/'.cache/native-build/sdk/android-ndk-r28c/toolchains/llvm/prebuilt/linux-x86_64/bin'
runtime=root/'.cache/native-build/runtime'
architecture={'arm64-v8a':(2,183,0x4000),'armeabi-v7a':(1,40,0x1000)}
system={'libc.so','libm.so','libdl.so','liblog.so','libandroid.so','libmediandk.so','libOpenSLES.so','libaaudio.so','libEGL.so','libGLESv2.so','libGLESv3.so','libz.so'}
assert {str(p.relative_to(runtime)) for p in runtime.rglob('*.so')}==set(manifest['libraries'])
abis={n.split('/')[1] for n in manifest['libraries']}
assert abis and abis<=set(architecture)
for abi in sorted(abis):
    libs=runtime/'jni'/abi
    elf_class,machine,minimum_alignment=architecture[abi]
    for file in sorted(libs.glob('*.so')):
        data=file.read_bytes();key=f'jni/{abi}/{file.name}'
        assert hashlib.sha256(data).hexdigest()==manifest['libraries'][key]
        assert data[:4]==b'\x7fELF' and data[4]==elf_class and data[5]==1,(key,'ELF class/endian')
        assert struct.unpack_from('<H',data,18)[0]==machine,(key,'ELF architecture')
        headers=subprocess.check_output([str(bin/'llvm-readelf'),'-h','-l','-d',str(file)],text=True)
        loads=[line for line in headers.splitlines() if line.strip().startswith('LOAD ')]
        assert loads,(key,'No LOAD segments')
        for line in loads:assert int(line.split()[-1],16)>=minimum_alignment,(key,'page alignment')
        needed=re.findall(r'NEEDED.*?\[(.*?)\]',headers)
        assert all(n in system or (libs/n).is_file() for n in needed),(key,needed)
        print(key,f'ELF class/machine verified; >= {minimum_alignment//1024}KB alignment; dependency closure verified')
    exports=subprocess.check_output([str(bin/'llvm-nm'),'-D','--defined-only',str(libs/'libopticast_mpv.so')],text=True)
    for name in ['create','initialize','option','set','get','command','surface','events','destroy']:
        assert 'Java_com_opticast_player_player_mpv_NativeMpv_'+name in exports,(abi,name)
    avutil=(libs/'libavutil.so').read_bytes()
    assert b'--enable-gpl' in avutil and b'--enable-version3' in avutil
    print(abi,'JNI exports and actual GPL/version3 FFmpeg configuration verified.')

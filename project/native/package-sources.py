#!/usr/bin/env python3
"""Archive the actual native source trees; omit objects/caches and unbuilt font/image fixtures."""
from pathlib import Path
import tarfile,json,hashlib
ROOT=Path(__file__).resolve().parents[2]
WORK=ROOT/'.cache/native-build'
OUT=ROOT/'.cache/source-distribution'
OUT.mkdir(parents=True,exist_ok=True)
names=['mpv','ffmpeg','libplacebo','dav1d','libass','freetype2','fribidi','fontconfig','harfbuzz','unibreak','libxml2','mbedtls']
files={}; omitted=[]
archive=OUT/'native-sources.tar.xz'
with tarfile.open(archive,'w:xz',preset=6) as tar:
 for name in names:
  base=WORK/'deps'/name
  for p in sorted(base.rglob('*')):
   if not p.is_file() or p.is_symlink():continue
   relative=p.relative_to(base)
   if any(x.startswith('_build') or x in ['.git','autom4te.cache','.libs','.deps','__pycache__'] for x in relative.parts):continue
   if p.suffix in ['.o','.a','.so','.pyc','.d','.gcno','.gcda']:continue
   data=p.read_bytes()
   if data.startswith((b'\x7fELF',b'!<arch>')):continue
   # Only omit non-code test/demo/documentation fixtures, never a compiled source/header/generator.
   if any(x in ['test','tests','test-data','demos','doc','docs','fuzzing','benchmarks'] for x in relative.parts) and p.suffix.lower() in ['.ttf','.otf','.woff','.woff2','.png','.jpg','.jpeg','.pdf','.webp']:
    omitted.append(f'deps/{name}/{relative}');continue
   dest=f'native-sources/deps/{name}/{relative}'
   tar.add(p,arcname=dest,recursive=False)
   files[dest]=hashlib.sha256(data).hexdigest()
 # Symlinks are copied only when they point inside the same supplied dependency tree.
 for name in names:
  base=WORK/'deps'/name
  for p in base.rglob('*'):
   if not p.is_symlink() or any(x.startswith('_build') or x=='.git' for x in p.relative_to(base).parts):continue
   try:p.resolve().relative_to(base.resolve())
   except ValueError:continue
   tar.add(p,arcname=f'native-sources/deps/{name}/{p.relative_to(base)}',recursive=False)
manifest={'archive':archive.name,'sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),
 'included_components':names,'source_files':files,'omitted_unbuilt_fixture_files':omitted,
 'note':'Actual source trees include generated configure files and the mbedTLS config changes made by the supplied scripts. No runtime code/headers/generators are omitted. Build objects and unused font/image test fixtures are excluded.'}
(OUT/'native-source-inventory.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Native source archive:',archive.stat().st_size,'bytes;',len(files),'source files;',len(omitted),'unbuilt fixtures omitted.')

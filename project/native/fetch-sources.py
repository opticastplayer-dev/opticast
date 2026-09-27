#!/usr/bin/env python3
"""Create a pinned, hash-verified source inventory. No repository HEAD is a build input."""
from pathlib import Path
import hashlib,json,urllib.request,tarfile,concurrent.futures,configparser
ROOT=Path(__file__).resolve().parent
CACHE=ROOT.parents[1]/'.cache/native-build'
LOCK=ROOT/'sources.lock.json'
SPECS={
 'mpv':('mpv-player/mpv','2a4eb8067ca68ec19adf23daf8ccbb1a05afd6ed'),
 'ffmpeg':('FFmpeg/FFmpeg','28ab05da84b3194d49eabeead9291af97ba251d4'),
 'libplacebo':('haasn/libplacebo','c42968d8616a1d1c8ad5f4f1a8d6f5a9cb396e56'),
 'dav1d':('videolan/dav1d','1.5.1'),
 'libass':('libass/libass','0.17.4'),
 'freetype2':('freetype/freetype','VER-2-14-3'),
 'fribidi':('fribidi/fribidi','v1.0.16'),
 'harfbuzz':('harfbuzz/harfbuzz','14.4.0'),
 'unibreak':('adah1972/libunibreak','libunibreak_8_0'),
 'libxml2':('GNOME/libxml2','v2.15.4'),
 'fontconfig':('fontconfig/fontconfig','2.18.3'),
 'mbedtls':('Mbed-TLS/mbedtls','mbedtls-3.6.7'),
 'curl':('curl/curl','curl-8_21_0'),
}
def get(url):
 req=urllib.request.Request(url,headers={'User-Agent':'OptiCast-source-builder'})
 return urllib.request.urlopen(req,timeout=90).read()
def api(url): return json.loads(get('https://api.github.com/'+url))
old=json.loads(LOCK.read_text()) if LOCK.exists() else {}
CACHE.mkdir(parents=True,exist_ok=True)
(CACHE/'archives').mkdir(exist_ok=True)
def fetch_one(pair):
 name,(repo,ref)=pair
 entry=old.get(name,{})
 sha=entry.get('commit') or (ref if len(ref)==40 else api(f'repos/{repo}/commits/{ref}')['sha'])
 url=f'https://codeload.github.com/{repo}/tar.gz/{sha}'
 archive=CACHE/'archives'/f'{name}.tar.gz'
 if not archive.exists(): archive.write_bytes(get(url))
 digest=hashlib.sha256(archive.read_bytes()).hexdigest()
 if entry.get('sha256') and entry['sha256']!=digest: raise ValueError(f'{name} source hash changed')
 target=CACHE/'deps'/name
 if not target.exists():
  target.mkdir(parents=True)
  with tarfile.open(archive) as tar:
   for m in tar.getmembers():
    parts=m.name.split('/',1)
    if len(parts)<2 or not parts[1]:continue
    m.name=parts[1];tar.extract(m,target,filter='data')
 result={'repository':repo,'requested_ref':ref,'commit':sha,'url':url,'sha256':digest}
 # Capture gitlinks explicitly; build uses wrap_mode=nodownload and no moving checkouts.
 if (target/'.gitmodules').exists():
  cfg=configparser.ConfigParser();cfg.read(target/'.gitmodules')
  for section in cfg.sections():
   subpath=cfg[section]['path'];suburl=cfg[section]['url']
   if not suburl.startswith('https://github.com/'): continue
   subrepo=suburl.removeprefix('https://github.com/').removesuffix('.git')
   link=api(f'repos/{repo}/contents/{subpath}?ref={sha}')
   subsha=link['sha'];subarchive=CACHE/'archives'/f'{name}--{subpath.replace("/","_")}.tar.gz'
   if not subarchive.exists():subarchive.write_bytes(get(f'https://codeload.github.com/{subrepo}/tar.gz/{subsha}'))
   expected_sub = next((v for v in entry.get('submodules',[]) if v['path']==subpath),None)
   if expected_sub and (expected_sub['commit'] != subsha or expected_sub['sha256'] != hashlib.sha256(subarchive.read_bytes()).hexdigest()):
    raise ValueError(f'{name}/{subpath} submodule provenance changed')
   subtarget=target/subpath;subtarget.mkdir(parents=True,exist_ok=True)
   with tarfile.open(subarchive) as tar:
    for m in tar.getmembers():
     parts=m.name.split('/',1)
     if len(parts)<2 or not parts[1]:continue
     m.name=parts[1];tar.extract(m,subtarget,filter='data')
   result.setdefault('submodules',[]).append({'path':subpath,'repository':subrepo,'commit':subsha,'sha256':hashlib.sha256(subarchive.read_bytes()).hexdigest()})
 print(name,sha,flush=True)
 return name,result
results=dict(old)
errors=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
 for f in [pool.submit(fetch_one,pair) for pair in SPECS.items()]:
  try:
   k,v=f.result();results[k]=v
  except Exception as e:errors.append(str(e));print('SOURCE ERROR:',repr(e),flush=True)
LOCK.write_text(json.dumps(results,indent=2)+'\n')
missing=SPECS.keys()-results.keys()
if missing or errors:raise SystemExit('Unresolved sources: '+', '.join(sorted(missing))+'; '+str(errors))

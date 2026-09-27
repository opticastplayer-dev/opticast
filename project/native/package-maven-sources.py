#!/usr/bin/env python3
"""Collect exact resolved runtime source JARs, POMs and upstream notices for distribution."""
from pathlib import Path
import urllib.request,urllib.error,concurrent.futures,hashlib,json,zipfile,io,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]; OUT=ROOT/'.cache/source-distribution'; OUT.mkdir(parents=True,exist_ok=True)
CACHE=ROOT/'.cache/maven-sources';CACHE.mkdir(parents=True,exist_ok=True)
records=[]
for line in (ROOT/'.cache/maven-runtime.tsv').read_text().splitlines():
 g,a,v,p=line.split('\t')
 if g in ('','unspecified') or 'opticast-mpv-runtime' in a:continue
 records.append((g,a,v,Path(p)))
records=list(dict.fromkeys(records))
def fetch(url):
 try:return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'OptiCast-source-distribution'}),timeout=40).read()
 except urllib.error.HTTPError as e:
  if e.code==404:return None
  raise
ns={'m':'http://maven.apache.org/POM/4.0.0'}
def licenses(pom,depth=0):
 tree=ET.fromstring(pom)
 found=[{'name':x.findtext('m:name','',ns),'url':x.findtext('m:url','',ns)} for x in tree.findall('m:licenses/m:license',ns)]
 if found or depth>=6:return found
 parent=tree.find('m:parent',ns)
 if parent is None:return []
 g,a,v=[parent.findtext('m:'+t,'',ns) for t in ['groupId','artifactId','version']]
 base='https://dl.google.com/dl/android/maven2' if g.startswith('androidx.') or g.startswith('com.android.') else 'https://repo.maven.apache.org/maven2'
 body=fetch(f'{base}/{g.replace(".","/")}/{a}/{v}/{a}-{v}.pom')
 return licenses(body,depth+1) if body else []
def one(record):
 g,a,v,binary=record; ident=f'{g}:{a}:{v}'; base='https://dl.google.com/dl/android/maven2' if g.startswith('androidx.') or g.startswith('com.android.') else 'https://repo.maven.apache.org/maven2'
 stem=f'{g.replace(".","/")}/{a}/{v}/{a}-{v}'; location=CACHE/g/a/v;location.mkdir(parents=True,exist_ok=True)
 pom=fetch(f'{base}/{stem}.pom')
 if pom is None:raise ValueError(ident+' missing POM')
 (location/f'{a}-{v}.pom').write_bytes(pom)
 sourcefile=location/f'{a}-{v}-sources.jar'
 if not sourcefile.exists():
  source=fetch(f'{base}/{stem}-sources.jar')
  if source:sourcefile.write_bytes(source)
 content=binary.read_bytes()
 with zipfile.ZipFile(io.BytesIO(content)) as z:
  runtime_classes=any(n.endswith('.class') for n in z.namelist())
  if 'classes.jar' in z.namelist():
   with zipfile.ZipFile(io.BytesIO(z.read('classes.jar'))) as classes:runtime_classes=any(n.endswith('.class') for n in classes.namelist())
  for n in z.namelist():
   if 'META-INF/' in n and any(k in n.upper().rsplit('/',1)[-1] for k in ['LICENSE','NOTICE','COPYING']):
    try:(location/('binary-'+n.replace('/','_'))).write_bytes(z.read(n))
    except IsADirectoryError:pass
 if not sourcefile.exists() and runtime_classes:raise ValueError(ident+' has code but no source JAR')
 if sourcefile.exists():
  with zipfile.ZipFile(sourcefile) as z:
   assert z.testzip() is None
   for n in z.namelist():
    if any(k in n.upper().rsplit('/',1)[-1] for k in ['LICENSE','NOTICE','COPYING']) and not n.endswith('/'):
     (location/('source-'+n.replace('/','_'))).write_bytes(z.read(n))
 entry={'coordinate':ident,'binary_sha256':hashlib.sha256(content).hexdigest(),'licenses':licenses(pom),
   'sources_url':f'{base}/{stem}-sources.jar' if sourcefile.exists() else None,'source_sha256':hashlib.sha256(sourcefile.read_bytes()).hexdigest() if sourcefile.exists() else None,
   'empty_binary':not runtime_classes}
 if not entry['licenses'] and ident=='org.bouncycastle:bcprov-jdk18on:1.75':
  with zipfile.ZipFile(sourcefile) as z: terms=z.read('org/bouncycastle/LICENSE.java').decode()
  assert 'Permission is hereby granted, free of charge' in terms and 'The above copyright notice' in terms
  entry['licenses']=[{'name':'Bouncy Castle permissive (MIT-style) license','url':'https://www.bouncycastle.org/licence.html','evidence':'org/bouncycastle/LICENSE.java in the included exact source JAR'}]
 print(ident,entry['licenses'],flush=True);return entry
entries=[];errors=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
 futures=[pool.submit(one,r) for r in records]
 for f in futures:
  try:entries.append(f.result())
  except Exception as e: errors.append(str(e));print('SOURCE ERROR',str(e),flush=True)
(ROOT/'project/native/maven-runtime.lock.json').write_text(json.dumps({'artifacts':entries,'errors':errors},indent=2)+'\n')
if errors:raise SystemExit('Missing Maven corresponding-source inputs: '+str(errors))
selected_files=sorted(p for g,a,v,_ in records for p in (CACHE/g/a/v).rglob('*') if p.is_file())
archive=OUT/'maven-sources.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for p in selected_files:
  if p.is_file():z.write(p,str(p.relative_to(CACHE)))
parts=['Resolved Android/JVM runtime dependencies. Exact source JARs and upstream POMs accompany the APK in its corresponding-source ZIP.']
for e in entries:parts.append(e['coordinate']+'\n'+'\n'.join(x['name']+' '+x['url'] for x in e['licenses']))
for p in selected_files:
 if p.is_file() and p.suffix not in ('.jar','.pom'):
  parts.append(str(p.relative_to(CACHE))+'\n\n'+p.read_text(errors='replace'))
(ROOT/'project/app/src/main/assets/legal/MAVEN-NOTICES.txt').write_text('\n\n'.join(parts))
print('Maven source archive:',archive.stat().st_size,'bytes;',len(entries),'artifacts.')

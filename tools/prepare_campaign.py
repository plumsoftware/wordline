"""Version 2 finite campaign authoring. Stable first 30 targets; original project vocabulary.
No downloaded lexicon, no runtime generation. Editorial acceptance remains a separate task.
"""
import collections
import hashlib
import itertools
import json
import random
from pathlib import Path
import prepare_content as foundation

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/content'
RNG=random.Random(20261120)
definitions=dict(foundation.DEFINITIONS)
for line in (ROOT/'tools/dictionary-ru.tsv').read_text().splitlines():
 if not line.strip():continue
 key,definition=line.split('|',1)
 key=key.replace('Ё','Е')
 if 3<=len(key)<=7:definitions[key]=definition
words=sorted(definitions)
counters={w:collections.Counter(w) for w in words}

# Candidate bags are unions of legitimate authored words, never arbitrary random strings.
bags={}
for a,b in itertools.combinations(words,2):
 counts=counters[a] | counters[b]
 if 4<=sum(counts.values())<=7:
  bag=''.join(sorted(counts.elements()))
  bags[bag]=counts
pools=[]
for bag,counts in bags.items():
 pool=[w for w in words if not (counters[w]-counts)]
 if len(pool)>=4:pools.append((bag,pool))
pools.sort(key=lambda x:(len(x[0]),-len(x[1]),x[0]))
print(f'Vocabulary {len(words)}; candidate letter bags {len(pools)}',flush=True)

# Bounded DFS: avoid a single difficult layout consuming an unbounded authoring run.
def layout(words,placed=(),cells=None,budget=None):
 cells=cells or {};budget=budget if budget is not None else [700]
 budget[0]-=1
 if budget[0]<0:return None
 if not words:return placed
 word=words[0];choices=[(0,0,False)] if not placed else []
 for (r,c),(letter,dirs) in sorted(cells.items()):
  for i,ch in enumerate(word):
   if ch==letter:
    for down in (False,True):
     if down not in dirs:choices.append((r-i if down else r,c if down else c-i,down))
 for r,c,down in choices:
  points=foundation.positions(word,r,c,down)
  if ((r-1,c) if down else (r,c-1)) in cells or ((r+len(word),c) if down else (r,c+len(word))) in cells:continue
  valid=True
  for p,ch in zip(points,word):
   if p in cells:
    if cells[p][0]!=ch or down in cells[p][1]:valid=False;break
   else:
    a,b=p;neighbors=[(a,b-1),(a,b+1)] if down else [(a-1,b),(a+1,b)]
    if any(n in cells for n in neighbors):valid=False;break
  if not valid:continue
  merged=dict(cells)
  for p,ch in zip(points,word):merged[p]=(ch,merged.get(p,('',set()))[1]|{down})
  rows=[p[0] for p in merged];cols=[p[1] for p in merged]
  if max(rows)-min(rows)>=9 or max(cols)-min(cols)>=9:continue
  result=layout(words[1:],placed+((word,r,c,down),),merged,budget)
  if result:return result
 return None

def pack(number,bag,targets,placed,pool):
 minr=min(s[1] for s in placed);minc=min(s[2] for s in placed)
 slots=[dict(id=f's{i+1}',word=w,row=r-minr,column=c-minc,direction='down' if d else 'across') for i,(w,r,c,d) in enumerate(placed)]
 cells={p for s in slots for p in foundation.positions(s['word'],s['row'],s['column'],s['direction']=='down')}
 return dict(id=number,letters=list(bag),rows=max(p[0] for p in cells)+1,columns=max(p[1] for p in cells)+1,slots=slots,bonusPool=sorted(set(pool)-set(targets)))

existing=json.loads((OUT/'campaign-v1.json').read_text())
levels=existing['levels'][:30]
for level in levels:
 counts=collections.Counter(level['letters']);targets={s['word'] for s in level['slots']}
 level['bonusPool']=[w for w in words if w not in targets and not (counters[w]-counts)]
used={(''.join(sorted(l['letters'])),tuple(sorted(s['word'] for s in l['slots']))) for l in levels}
recent=[''.join(sorted(l['letters'])) for l in levels[-20:]]

def author(number,daily=False):
 maxletters=5 if not daily and number<=50 else 6 if not daily and number<=200 else 7
 target=3 if not daily and number<=50 else 4 if not daily and number<=200 else 5 if not daily and number<=600 else 6
 choices=[x for x in pools if len(x[0])<=maxletters and len(x[1])>=target]
 for attempt in range(2500):
  bag,pool=RNG.choice(choices)
  if bag in recent:continue
  selected=RNG.sample(pool,target)
  selected.sort(key=lambda w:(-len(w),w))
  signature=(bag,tuple(sorted(selected)))
  if signature in used:continue
  result=layout(selected)
  if result:
   used.add(signature);recent.append(bag);del recent[:-20]
   return pack(number,bag,selected,result,pool)
 raise RuntimeError(f'Cannot author {number}, target {target}')
for i in range(31,1001):
 levels.append(author(i))
 if i%100==0:print(f'Campaign {i}/1000',flush=True)
daily=[]
for i in range(1,366):
 daily.append(author(i,True))
 if i%100==0:print(f'Daily {i}/365',flush=True)
plural={'ВОРОТА','СУТКИ'}
display={'ОСЕЛ':'ОСЁЛ','АКТЕР':'АКТЁР','ШАТЕР':'ШАТЁР','ТЕС':'ТЁС','ПОМЕТ':'ПОМЁТ','ЗАЕМ':'ЗАЁМ'}
entries=[dict(key=w,display=display.get(w,w),definition=definitions[w],partOfSpeech='noun',pluraliaTantum=w in plural,difficulty=1 if len(w)<=4 else 2 if len(w)<=5 else 3,source='Original project vocabulary and definitions',licenseId='project-original',editorialStatus='pending') for w in words]
payload=dict(contentVersion=2,dictionaryVersion=2,levels=levels,dailyLevels=daily,dictionary=entries)
raw=(json.dumps(payload,ensure_ascii=False,indent=2)+'\n').encode()
(OUT/'campaign-v1.json').write_bytes(raw)
(OUT/'manifest.json').write_text(json.dumps(dict(contentVersion=2,dictionaryVersion=2,levels=1000,dailyLevels=365,dictionaryEntries=len(entries),sha256=hashlib.sha256(raw).hexdigest()),indent=2)+'\n')
report=dict(campaign=1000,daily=365,dictionary=len(entries),uniqueTargetConfigurations=len(used),repeatedBagsWithin20=[],editorialStatus='pending',note='All definitions are original. Structural validation is independent from editorial review and device QA.')
(ROOT/'tools/content-editorial-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print('Prepared 1000 campaign and 365 daily levels',flush=True)

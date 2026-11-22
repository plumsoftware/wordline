"""Surgical content v3 authoring: preserve slot IDs, lengths and crossword geometry.

Replace excluded answers through a bounded crossing/letter-bag CSP. No level IDs,
cells, completions or money are reset. Also emit v2 import compatibility metadata.
"""
from pathlib import Path
from collections import Counter,defaultdict
import json,hashlib

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/content'
BLOCKED={'РОМ','БАР','СЕМЯ','МОР'}

def cells(slot):
    down=slot['direction']=='down'
    return [(slot['row']+(i if down else 0),slot['column']+(0 if down else i)) for i in range(len(slot['word']))]

def replace_answers(level,words):
    slots=level['slots'];points=[cells(s) for s in slots];original=Counter(level['letters'])
    domains={length:[w for w in words if len(w)==length] for length in {len(s['word']) for s in slots}}
    counters={w:Counter(w) for domain in domains.values() for w in domain}
    bad={i for i,s in enumerate(slots) if s['word'] in BLOCKED}
    for allowance in range(len(slots)-len(bad)+1):
        assigned={};board={};budget=[500000]
        def visit(bag,changes):
            budget[0]-=1
            if budget[0]<0:return None
            if len(assigned)==len(slots):return dict(assigned)
            choices=[]
            for index,slot in enumerate(slots):
                if index in assigned:continue
                candidates=[]
                for word in domains[len(slot['word'])]:
                    if word in assigned.values():continue
                    changed=index not in bad and word!=slot['word']
                    if changes+changed>allowance:continue
                    if any(p in board and board[p]!=letter for p,letter in zip(points[index],word)):continue
                    merged=bag|counters[word]
                    if sum(merged.values())>7:continue
                    candidates.append((word,merged,changed))
                if not candidates:return None
                candidates.sort(key=lambda c:(c[0]!=slot['word'],sum((c[1]-original).values()),sum(c[1].values()),c[0]))
                choices.append((len(candidates),index,candidates))
            _,index,candidates=min(choices,key=lambda c:(c[0],c[1] not in bad,c[1]))
            for word,merged,changed in candidates:
                added=[]
                for p,letter in zip(points[index],word):
                    if p not in board:board[p]=letter;added.append(p)
                assigned[index]=word
                result=visit(merged,changes+changed)
                if result:return result
                del assigned[index]
                for p in added:del board[p]
            return None
        result=visit(Counter(),0)
        if result:
            needed=Counter()
            for word in result.values():needed|=counters[word]
            bag=[];remaining=needed.copy()
            for letter in level['letters']:
                if remaining[letter]>0:bag.append(letter);remaining[letter]-=1
            bag+=sorted(remaining.elements())
            target=max(len(level['letters']),len(bag))
            extras=Counter(level['letters'])-Counter(bag)
            for letter in level['letters']:
                if len(bag)>=target:break
                if extras[letter]>0:bag.append(letter);extras[letter]-=1
            return result,bag
    raise RuntimeError(f'No shape-preserving replacement for level {level["id"]}')

if __name__=='__main__':
    path=OUT/'campaign-v1.json';package=json.loads(path.read_text(encoding='utf-8'))
    if package['contentVersion']>=3:raise SystemExit('Content v3 is already authored; refusing to replace its legacy metadata')
    dictionary=[entry for entry in package['dictionary'] if entry['key'] not in BLOCKED]
    words=sorted(entry['key'] for entry in dictionary);counters={word:Counter(word) for word in words}
    legacy={'sourceVersion':2,'targetVersion':3,'levels':{},'dailyLevels':{}}
    report={'targetVersion':3,'removedWords':sorted(BLOCKED),'replacements':[],'bonusOnlyLevels':0}
    for section in ('levels','dailyLevels'):
        for level in package[section]:
            old=json.loads(json.dumps(level));has_target=any(s['word'] in BLOCKED for s in level['slots'])
            if has_target:
                assigned,bag=replace_answers(level,words)
                for index,word in assigned.items():level['slots'][index]['word']=word
                level['letters']=bag
                targets={slot['word'] for slot in level['slots']};counts=Counter(bag)
                level['bonusPool']=[word for word in words if word not in targets and not(counters[word]-counts)]
                report['replacements'].append({'section':section,'id':level['id'],'before':[s['word'] for s in old['slots']],
                    'after':[s['word'] for s in level['slots']],'lettersChanged':old['letters']!=bag})
            else:
                level['bonusPool']=[word for word in level['bonusPool'] if word not in BLOCKED]
            if old!=level:
                legacy[section][str(level['id'])]={'letterCount':len(old['letters']),'bonusPool':old['bonusPool']}
                if not has_target:report['bonusOnlyLevels']+=1
    package['dictionary']=dictionary;package['contentVersion']=3;package['dictionaryVersion']=3
    raw=(json.dumps(package,ensure_ascii=False,indent=2)+'\n').encode('utf-8');path.write_bytes(raw)
    manifest=json.loads((OUT/'manifest.json').read_text(encoding='utf-8'))
    manifest.update(contentVersion=3,dictionaryVersion=3,dictionaryEntries=len(dictionary),sha256=hashlib.sha256(raw).hexdigest())
    (OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (OUT/'content-v2-compat.json').write_text(json.dumps(legacy,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (ROOT/'docs/word-removal-v3.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'Authored v3: {len(dictionary)} dictionary entries; {len(report["replacements"])} answer replacements; {report["bonusOnlyLevels"]} bonus-only changes',flush=True)

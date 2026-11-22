"""Read-only lexical inventory of shipped project content; not a legal certification.

The user-requested topic exclusions and age-suitability terms are review flags,
not a declaration that an isolated word is unlawful. Does not modify game data.
"""
from pathlib import Path
import json,re,hashlib,sys
sys.stdout.reconfigure(encoding="utf-8")

ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/'app/src/main'
PATTERNS={
    'user_requested_topic_exclusions':r'\b(?:лгбт\w*|lgbt\w*|гомосексу\w*|лесбиян\w*|трансгендер\w*|гей|геи)\b',
    'obscene_language':r'\b(?:хуй\w*|хуя\w*|хуё\w*|пизд\w*|еба\w*|ёб\w*|бляд\w*|бля|нахуй|сука|суки|жопа)\b',
    'adult_explicit':r'\b(?:порно\w*|порнограф\w*|эротик\w*|секс|секса|сексом|сексуаль\w*)\b',
    'drug_self_harm_review':r'\b(?:наркотик\w*|кокаин|героин|метамфетамин|гашиш|марихуан\w*|суицид\w*|самоубий\w*)\b',
    'extremism_symbolism_review':r'\b(?:игил|нацист\w*|свастик\w*|терроризм\w*)\b',
}
compiled={name:re.compile(pattern,re.I) for name,pattern in PATTERNS.items()}
files=list((APP/'assets').rglob('*.json'))+list((APP/'java').rglob('*.kt'))+list((APP/'res').glob('values*/strings*.xml'))
findings=[]
for path in files:
    text=path.read_text(encoding='utf-8')
    # Kotlin comments are excluded; UI literals and their neighbouring source remain reviewable.
    if path.suffix=='.kt':
        fragments=[(text.count('\n',0,m.start())+1,m.group(0)) for m in re.finditer(r'"(?:\\.|[^"\\])*"',text)]
    else:
        fragments=list(enumerate(text.splitlines(),1))
    for line,fragment in fragments:
        for category,pattern in compiled.items():
            matches=sorted(set(m.group(0) for m in pattern.finditer(fragment)))
            if matches:findings.append({'file':path.relative_to(ROOT).as_posix(),'line':line,'category':category,'matches':matches,'context':fragment[:260]})
package=json.loads((APP/'assets/content/campaign-v1.json').read_text(encoding='utf-8'))
media=[p for p in (APP/'res').rglob('*') if p.suffix.lower() in ('.png','.webp','.gif','.wav')]
report={'date':'2026-11-22','scope':'Shipped project-owned JSON, Kotlin text literals, string resources; media inventory. No data edits.',
        'dictionaryEntries':len(package['dictionary']),'campaignLevels':len(package['levels']),'dailyLevels':len(package['dailyLevels']),
        'textFiles':len(files),'mediaFiles':len(media),'lexicalFindings':findings,
        'patterns':PATTERNS,'editorialContextReview':[{'word':e['key'],'definition':e['definition']} for e in package['dictionary'] if e['key'] in {'РОМ','БАР','РАБ','МОР','ПИКА','ТИР','ТАНК'}],'contentSha256':hashlib.sha256((APP/'assets/content/campaign-v1.json').read_bytes()).hexdigest(),
        'limits':['Keyword review is not an exhaustive legal classification.','Third-party SDK binaries, remote advertising, imported saves and future content are not certified.','Media needs separate visual review; raster text is not detected by this lexical script.']}
(ROOT/'docs/content-audit-2026-11-22.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:report[k] for k in ('dictionaryEntries','campaignLevels','dailyLevels','textFiles','mediaFiles','lexicalFindings')},ensure_ascii=False))

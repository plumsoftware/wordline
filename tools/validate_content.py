"""Validate the shipped package without Android, network, or third-party libraries."""
import collections
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/assets/content'

def validate():
    raw = (ROOT / 'campaign-v1.json').read_bytes()
    manifest = json.loads((ROOT / 'manifest.json').read_text())
    assert hashlib.sha256(raw).hexdigest() == manifest['sha256'], 'Content checksum mismatch'
    package = json.loads(raw)
    dictionary = {entry['key']: entry for entry in package['dictionary']}
    assert len(dictionary) == len(package['dictionary']), 'Duplicate dictionary key'
    for key, entry in dictionary.items():
        assert key.isalpha() and all('А' <= c <= 'Я' and c != 'Ё' for c in key), key
        assert entry['definition'].strip() and entry['licenseId'] == 'project-original', key
    assert len(package['levels']) == manifest['levels'] == 1000
    assert len(dictionary) == manifest['dictionaryEntries']
    assert [level['id'] for level in package['levels']] == list(range(1,1001))
    assert len(package['dailyLevels']) == manifest['dailyLevels'] == 365
    assert [l['id'] for l in package['dailyLevels']] == list(range(1,366))
    for level in package['levels'] + package['dailyLevels']:
        label = f"Level {level['id']}"
        letters = collections.Counter(level['letters'])
        slots = level['slots']
        assert 3 <= len(level['letters']) <= 7 and 1 <= level['rows'] <= 9 and 1 <= level['columns'] <= 9, label
        assert len({s['id'] for s in slots}) == len(slots) >= 2, label
        assert len({s['word'] for s in slots}) == len(slots), label
        cells = {}; slot_points = {}
        for slot in slots:
            word = slot['word']; down = slot['direction'] == 'down'
            assert slot['direction'] in ('across', 'down'), label
            assert word in dictionary and 3 <= len(word) <= 7 and not (collections.Counter(word)-letters), label
            points = []
            for i,c in enumerate(word):
                p = (slot['row'] + (i if down else 0), slot['column'] + (0 if down else i))
                assert 0 <= p[0] < level['rows'] and 0 <= p[1] < level['columns'], label
                entries = cells.setdefault(p,[])
                assert all(ch == c and direction != down for ch,direction in entries), label
                entries.append((c,down)); points.append(p)
            slot_points[slot['id']] = points
        reached = {slots[0]['id']}
        while True:
            linked = set().union(*(set(slot_points[i]) for i in reached))
            more = {i for i,points in slot_points.items() if linked.intersection(points)}
            if more == reached: break
            reached = more
        assert len(reached) == len(slots), label + ': disconnected'
        for down in (False, True):
            expected = {tuple(slot_points[s['id']]) for s in slots if (s['direction']=='down') == down}
            for r,c in cells:
                before = (r-1,c) if down else (r,c-1)
                if before in cells: continue
                run = []; p = (r,c)
                while p in cells:
                    run.append(p); p = (p[0]+1,p[1]) if down else (p[0],p[1]+1)
                assert len(run) < 2 or tuple(run) in expected, label + ': unintended run'
        for word in level['bonusPool']:
            assert word in dictionary and word not in {s['word'] for s in slots} and not (collections.Counter(word)-letters), label
    economy = json.loads((ROOT/'economy-v1.json').read_text())
    assert economy['economyVersion']=='1.0' and economy['bonusChestWords']==5
    for key in ('initialCoins','campaignWin','bonusChestCoins','hintLetterCost','hintWordCost','hintClueCost'):
        assert 0 <= economy[key] <= economy['walletCap'], key
    print(f"Content valid: {len(package['levels'])} campaign levels, {len(package['dailyLevels'])} daily levels, {len(dictionary)} entries")

if __name__ == '__main__': validate()

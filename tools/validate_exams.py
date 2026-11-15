from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
assets = root / 'app/src/main/assets'
questions = 0
image_count = 0
labels = ['Trafik ve Çevre Bilgisi', 'İlk Yardım', 'Motor ve Araç Tekniği', 'Trafik Adabı']
expected = {labels[0]: 23, labels[1]: 12, labels[2]: 9, labels[3]: 6}

for index in range(1, 27):
    f = assets / 'tests' / f'{index}.json'
    assert f.is_file(), f'Test {index} missing'
    exam = json.loads(f.read_text(encoding='utf-8'))
    assert exam['id'] == index, f'Incorrect test ID {index}'
    assert exam['title'] == f'Test {index}'
    assert exam['durationMinutes'] == 45
    assert exam['passCorrect'] == 35
    assert len(exam['questions']) == 50, f'Test {index} != 50 questions'
    totals = dict.fromkeys(labels, 0)
    for ordinal, q in enumerate(exam['questions'], 1):
        assert isinstance(q['text'], str) and q['text'].strip(), (index, ordinal, 'question missing')
        assert len(q['choices']) == 4, (index, ordinal, 'options count')
        assert all(isinstance(c, str) and c.strip() for c in q['choices']), (index, ordinal, 'empty option')
        assert type(q['correct']) is int and 0 <= q['correct'] <= 3, (index, ordinal, 'invalid key')
        assert q['category'] in expected, (index, ordinal, 'invalid subject')
        assert not any(tag in q['text'].lower() for tag in ('https://', 'http://')), (index, ordinal, 'source text leaked')
        totals[q['category']] += 1
        if 'image' in q:
            asset = assets / 'figures' / q['image']
            assert asset.is_file() and asset.stat().st_size > 0, (index, ordinal, 'missing image')
            image_count += 1
        questions += 1
    assert totals == expected, (index, totals)

assert len(list((assets / 'tests').glob('*.json'))) == 26, 'Unexpected question packs'
assert questions == 1300

manifest = (root / 'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
assert 'android.permission.INTERNET' not in manifest
assert 'uses-permission' not in manifest
assert 'usesCleartextTraffic="false"' in manifest
code = (root / 'app/src/main/java/com/ehliyet/deneme/MainActivity.java').read_text(encoding='utf-8')
assert 'KAYRA HAN AYDOĞAN' in code
assert 'java.net' not in code
assert 'WebView' not in code and 'http://' not in code and 'https://' not in code
print(f'PASS: 26 tests, {questions} questions, {image_count} illustration placements; all answers, images, credits, and offline checks passed.')

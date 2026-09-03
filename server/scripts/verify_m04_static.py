from pathlib import Path
import re, sys

root=Path(__file__).resolve().parents[1]
java=root/'src/main/java/com/petlink'
errors=[]
required=[
 java/'modules/animal/controller/AnimalController.java',
 java/'modules/animal/controller/AnimalMediaController.java',
 java/'modules/animal/service/AnimalQueryService.java',
 java/'modules/animal/service/AnimalTransactionalService.java',
 java/'modules/animal/service/AnimalAccessService.java',
 java/'modules/animal/service/AnimalMediaService.java',
 java/'modules/animal/service/AnimalResponseAssembler.java',
 java/'modules/animal/mapper/AnimalMapper.java',
 java/'modules/animal/mapper/AnimalImageMapper.java',
 java/'modules/animal/mapper/HealthRecordMapper.java',
 java/'modules/adoption/mapper/AdoptionRecordMapper.java',
]
for p in required:
    if not p.exists(): errors.append(f'missing {p.relative_to(root)}')
text='\n'.join(p.read_text(encoding='utf-8') for p in required if p.exists())
markers=[
 'GET /api/animals', # documentation marker is not Java; endpoint structural markers below
 'selectPublicPage','status=\'AVAILABLE\'','selectResponsiblePage','rescue_task_id',
 'selectForUpdate(animalId)','maxSortOrder(animalId)+1','bindPreparedStartingAt',
 'selectAfter(animalId,target.getSortOrder())','moveSortOrder',
 'TO_OBSERVING','OPEN_ADOPTION','SUSPEND_ADOPTION','RESUME_ADOPTION',
 'version = version + 1','OPTIMISTIC_LOCK_CONFLICT',
 'existsByAnimalAndUser','/api/media/animal-images/',
]
# Java has no literal GET /api/animals; check controller annotations instead.
markers.remove('GET /api/animals')
for m in markers:
    if m not in text: errors.append(f'missing marker: {m}')
controller=(java/'modules/animal/controller/AnimalController.java').read_text(encoding='utf-8')
for m in ['@GetMapping','@GetMapping("/{animalId}")','@GetMapping("/responsible/me")','@PatchMapping("/{animalId}")',
          '@PostMapping("/{animalId}/images")','@DeleteMapping("/{animalId}/images/{imageId}")',
          '@PostMapping("/{animalId}/health-records")','@GetMapping("/{animalId}/health-records")','@PostMapping("/{animalId}/status-actions")']:
    if m not in controller: errors.append(f'controller missing: {m}')
media=(java/'modules/animal/controller/AnimalMediaController.java').read_text(encoding='utf-8')
if '@RequestMapping("/api/media/animal-images")' not in media: errors.append('animal media endpoint missing')
security=(java/'config/SecurityConfig.java').read_text(encoding='utf-8')
for m in ['HttpMethod.GET','"/api/animals"','"/api/animals/*"','"/api/animals/*/health-records"','"/api/media/animal-images/*"']:
    if m not in security: errors.append(f'public security route missing: {m}')
security_test=root/'src/test/java/com/petlink/modules/animal/AnimalControllerSecurityWebTest.java'
if not security_test.exists(): errors.append('M04 controller security Web test missing')
else:
    st=security_test.read_text(encoding='utf-8')
    for m in ['/api/media/animal-images/1', 'MediaType.IMAGE_PNG', 'content().contentType']:
        if m not in st: errors.append(f'M04 visitor media security test missing marker: {m}')
# Frozen M04 append rule: batch <=9, but no lifetime total-image cap.
tx=(java/'modules/animal/service/AnimalTransactionalService.java').read_text(encoding='utf-8')
if 'tokens.size()>9' not in tx: errors.append('per-append image limit missing')
for bad in ['existing + tokens.size() > 9','countByAnimalId(animalId) +','countByAnimalId(animalId)+']:
    if bad in tx: errors.append('invalid lifetime 9-image cap detected')
# Frozen lock order for image/health writes: Animal FOR UPDATE before plain Task access, then temp lock.
add_images=tx[tx.find('public AnimalDetailResponse addImages'):tx.find('public AnimalDetailResponse deleteImage')]
positions=[add_images.find('selectForUpdate(animalId)'),add_images.find('requireManageAccess'),add_images.find('lockAndValidate')]
if any(x<0 for x in positions) or positions != sorted(positions): errors.append('M04 add-image lock order is not Animal -> plain Task -> TemporaryFile')
# Existing Frozen regression guards must stay.
for script_name, safe in {
 'smoke-m02.ps1':['@($mine.data.records | Where-Object','@($queue.data.records | Where-Object','@($detail.data.images).Count'],
 'smoke-m03.ps1':['@($waiting.data.records | Where-Object','@($records.data).Count','@($success.data.animalIds).Count','@($taskDetail.data.animals).Count','@($mine.data.records | Where-Object'],
 'smoke-m04.ps1':['@($responsible.data.records | Where-Object','@($detail.healthRecords).Count','@($public.data.records | Where-Object','@($withImages.images).Count','@($remaining.images).Count'],
}.items():
    p=root/'scripts'/script_name
    if not p.exists(): errors.append(f'{script_name} missing'); continue
    raw=p.read_bytes()
    if any(b>127 for b in raw): errors.append(f'{script_name} contains non-ASCII bytes')
    txt=raw.decode('ascii')
    for m in safe:
        if m not in txt: errors.append(f'{script_name} missing scalar-safe marker: {m}')
# Source test count only.
total=0;m04=0
for p in (root/'src/test/java').rglob('*.java'):
    c=len(re.findall(r'@Test\b',p.read_text(encoding='utf-8'))); total+=c
    if '/modules/animal/' in p.as_posix(): m04+=c
print(f'M04-focused @Test methods: {m04}')
print(f'Total @Test methods: {total}')
print('M04 APIs: 10 (9 animal resource routes + 1 media route)')
if errors:
    print('FAILED')
    for e in errors: print(' -',e)
    sys.exit(1)
print('PASSED')

from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'src/main/java'
TEST=ROOT/'src/test/java'
SCRIPTS=ROOT/'scripts'

def text(rel):
    p=ROOT/rel
    if not p.exists(): raise SystemExit(f'MISSING: {p}')
    return p.read_text(encoding='utf-8')

def need(blob,needle,label):
    if needle not in blob: raise SystemExit(f'FAILED: {label}: missing {needle}')

def ordered(blob,needles,label):
    pos=-1
    for n in needles:
        q=blob.find(n,pos+1)
        if q<0: raise SystemExit(f'FAILED: {label}: missing/out-of-order {n}')
        pos=q

controllers='\n'.join(p.read_text(encoding='utf-8') for p in (MAIN/'com/petlink/modules/adoption/controller').glob('*.java'))
for endpoint in [
    '/{animalId}/adoption-applications','/me','/{applicationId}','/{applicationId}/withdraw',
    '/{applicationId}/audit','/{recordId}','/{animalId}/adoption-overview']:
    need(controllers,endpoint,'M05 endpoint')
need(controllers,'/api/admin/adoption-applications','admin endpoint base')
need(controllers,'/api/adoption-records','record endpoint base')
need(controllers,'/api/adoption-applications','application endpoint base')

tx=text('src/main/java/com/petlink/modules/adoption/service/AdoptionTransactionalService.java')
need(tx,'uk_adoption_application_user_animal','named lifetime unique mapping')
need(tx,'ADOPTION_APPLICATION_ALREADY_EXISTS','40907 mapping')
need(tx,'target.getUserId()','self review check')
need(tx,'ErrorCode.FORBIDDEN','BR-07 forbidden')
need(tx,'AUTO_INVALIDATE','auto invalidate log')
need(tx,'"ADOPT"','animal adopt log')
need(tx,'record.setApplicationId(target.getId())','record source from target')
need(tx,'record.setAnimalId(target.getAnimalId())','record animal from target')
need(tx,'record.setUserId(target.getUserId())','record user from target')
need(tx,'applicationMapper.selectOtherPendingForUpdate','other pending lock')
need(tx,'animalMapper.adoptAvailable','AVAILABLE to ADOPTED')
ordered(tx,[
    'applicationMapper.selectAnimalIdById(applicationId)',
    'animalMapper.selectForUpdate(locatedAnimalId)',
    'applicationMapper.selectForUpdate(applicationId)',
    'applicationMapper.selectOtherPendingForUpdate(animal.getId(),target.getId())'
],'Frozen APPROVE lock order')
# Reject method must not lock Animal.
rej=tx[tx.index('private AdoptionAuditResponse reject'):tx.index('private AdoptionAuditResponse approve')]
if 'animalMapper.selectForUpdate' in rej: raise SystemExit('FAILED: REJECT must not lock Animal')
need(rej,'forbidSelfReview(admin,target)','reject BR-07 check')

mapper=text('src/main/java/com/petlink/modules/adoption/mapper/AdoptionApplicationMapper.java')
need(mapper,"ORDER BY id ASC FOR UPDATE",'other pending lock order')
need(mapper,"status='INVALIDATED'",'invalidate update')
need(mapper,"status='APPROVED'",'approve update')
need(mapper,"status='REJECTED'",'reject update')

animal_mapper=text('src/main/java/com/petlink/modules/animal/mapper/AnimalMapper.java')
need(animal_mapper,"status='ADOPTED'",'animal adopt SQL')
need(animal_mapper,'version=version+1','animal version increment')

media=text('src/main/java/com/petlink/modules/animal/service/AnimalMediaService.java')
need(media,'existsByAnimalAndUser','final adopter media permission')

assembler=text('src/main/java/com/petlink/modules/adoption/service/AdoptionResponseAssembler.java')
need(assembler,'isFinalAdopter','historical/final adopter cover rule')
need(assembler,'canReadImage','cover media authorization')

err=text('src/main/java/com/petlink/common/ErrorCode.java')
need(err,'ADOPTION_APPLICATION_ALREADY_EXISTS','40907 enum')

smoke=(SCRIPTS/'smoke-m05.ps1').read_bytes()
if any(b>=128 for b in smoke): raise SystemExit('FAILED: smoke-m05.ps1 must remain ASCII-only for Windows PowerShell 5.1')
smoke_txt=smoke.decode('ascii')
for needle in ['WITHDRAWN','REJECTED','APPROVED','INVALIDATED','adoption-overview','@($success.animalIds).Count','@($queue.data.records | Where-Object']:
    need(smoke_txt,needle,'smoke M05 coverage')

m05_tests=sum(len(re.findall(r'@Test\b',p.read_text(encoding='utf-8'))) for p in (TEST/'com/petlink/modules/adoption').glob('*.java'))
total_tests=sum(len(re.findall(r'@Test\b',p.read_text(encoding='utf-8'))) for p in TEST.rglob('*.java'))
if m05_tests < 20: raise SystemExit(f'FAILED: expected substantial M05 tests, found {m05_tests}')
print('M05 static implementation checks: PASSED')
print('M05 API endpoints: 9')
print('M05-focused @Test:',m05_tests)
print('Total @Test:',total_tests)
print('smoke-m05.ps1 non-ASCII bytes: 0')


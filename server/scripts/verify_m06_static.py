from pathlib import Path
import re
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

base=MAIN/'com/petlink/modules/followup'
required=[
 'controller/FollowUpAdoptionRecordController.java','controller/FollowUpController.java','controller/FollowUpAdminController.java',
 'controller/FollowUpRescuerController.java','controller/FollowUpMediaController.java','dto/SubmitFollowUpRequest.java',
 'entity/FollowUpRecord.java','entity/FollowUpImage.java','mapper/FollowUpRecordMapper.java','mapper/FollowUpImageMapper.java',
 'service/FollowUpService.java','service/FollowUpTransactionalService.java','service/FollowUpQueryService.java',
 'service/FollowUpFileBindingService.java','service/FollowUpMediaService.java','vo/FollowUpRecordResponse.java','vo/FollowUpImageResponse.java']
for rel in required:
    if not (base/rel).exists(): raise SystemExit(f'MISSING M06 source: {rel}')

controllers='\n'.join(p.read_text(encoding='utf-8') for p in (base/'controller').glob('*.java'))
for needle in ['/api/adoption-records','/{adoptionRecordId}/follow-ups','/api/follow-ups','/{followUpId}','/api/admin/follow-ups','/api/rescuer/follow-ups','/api/media/follow-up-images']:
    need(controllers,needle,'M06 endpoint')
need(controllers,"hasAnyRole('USER','RESCUER')",'submit roles')
need(controllers,"hasAnyRole('USER','RESCUER','ADMIN')",'read roles')
need(controllers,"hasRole('ADMIN')",'admin role')
need(controllers,"hasRole('RESCUER')",'rescuer role')

service=text('src/main/java/com/petlink/modules/followup/service/FollowUpService.java')
need(service,'selectBySubmitterAndKey','fast permanent idempotency lookup')
need(service,'uk_follow_up_submitter_idempotency','named unique race recovery')
need(service,'IDEMPOTENCY_KEY_CONFLICT','40908 mapping')
ordered(service,['normalizeIdempotencyKey(request)','selectBySubmitterAndKey(principal.getUserId(),key)','normalizeForCreate(request,key)','tx.create(principal,adoptionRecordId,normalized)'],'idempotency-before-old-image-validation')

tx=text('src/main/java/com/petlink/modules/followup/service/FollowUpTransactionalService.java')
ordered(tx,['adoptionRecordMapper.selectById(adoptionRecordId)','recordMapper.insert(record)','files.lockAndValidate','files.bindPrepared','files.copyPrepared'],'Frozen M06 transaction order')
need(tx,'principal.getUserId().equals(adoption.getUserId())','adoption ownership')

mapper=text('src/main/java/com/petlink/modules/followup/mapper/FollowUpRecordMapper.java')
need(mapper,'ORDER BY created_at ASC,id ASC','history ascending order')
need(mapper,'JOIN rescue_task t','responsible rescuer join')
need(mapper,'ORDER BY f.created_at DESC,f.id DESC','admin/rescuer descending page order')

files=text('src/main/java/com/petlink/modules/followup/service/FollowUpFileBindingService.java')
need(files,'selectForUpdateByTokens(tokens)','temporary file FOR UPDATE')
need(files,'"FOLLOW_UP"','FOLLOW_UP business binding')
need(files,'sort++','client image token order')
ordered(files,['prepared.createdFormalPaths.add(file.formalPath)','storage.copyTemporaryToFormal(file.tempPath,file.formalPath)'],'register rollback cleanup before physical copy')
need(files,'afterCommit','post-commit cleanup')
need(files,'STATUS_COMMITTED','rollback formal-copy cleanup')

assoc=text('src/main/java/com/petlink/infrastructure/file/service/BoundFileAssociationVerifier.java')
need(assoc,'case "FOLLOW_UP"','bound cleanup association whitelist')
need(assoc,'follow_up_image','bound cleanup image association')

media=text('src/main/java/com/petlink/modules/followup/service/FollowUpMediaService.java')
need(media,'visibleAdoptionRecord','media visibility')
need(media,'storage.readImage','canonicalized/symlink-safe shared media read')

entity=text('src/main/java/com/petlink/modules/followup/entity/FollowUpRecord.java')
for forbidden in ['status;','updatedAt','version;','deleted']:
    if forbidden in entity: raise SystemExit(f'FAILED: FollowUpRecord must remain append-only: {forbidden}')

for p in (base/'controller').glob('*.java'):
    blob=p.read_text(encoding='utf-8')
    if '@PatchMapping' in blob or '@DeleteMapping' in blob:
        raise SystemExit(f'FAILED: M06 must not expose PATCH/DELETE: {p.name}')

smoke=(SCRIPTS/'smoke-m06.ps1').read_bytes()
if any(b>=128 for b in smoke): raise SystemExit('FAILED: smoke-m06.ps1 must remain ASCII-only for Windows PowerShell 5.1')
smoke_txt=smoke.decode('ascii')
for needle in ['idempotent replay','IDEMPOTENCY_KEY_CONFLICT','real concurrent permanent-idempotency race','Start-Job','200,201','creates exactly one additional FollowUpRecord','/api/admin/follow-ups','/api/rescuer/follow-ups','/api/media/follow-up-images/','M06 smoke flow PASSED']:
    need(smoke_txt,needle,'smoke M06 coverage')

file_test=text('src/test/java/com/petlink/modules/followup/FollowUpFileBindingServiceTest.java')
for needle in ['ORDER BY id ASC FOR UPDATE','zeroAffectedRowsOnBoundStopsBeforePhysicalCopy','copyFailureRegistersFormalPathForRollbackAndPreservesTemporaryOriginal','temporaryCleanupRunsOnlyAfterCommitAndClientSortOrderIsPersisted']:
    need(file_test,needle,'direct FollowUpFileBindingService coverage')


manifest=text('docs/M06-SELECTIVE-MERGE-MANIFEST.md')
for needle in ['Never overwrite from this candidate','README-CODING.md','server/README.md','FollowUpFileBindingServiceTest.java','smoke-m06.ps1']:
    need(manifest,needle,'M06 selective-merge safety manifest')

m06_tests=sum(len(re.findall(r'@Test\b',p.read_text(encoding='utf-8'))) for p in (TEST/'com/petlink/modules/followup').glob('*.java'))
total_tests=sum(len(re.findall(r'@Test\b',p.read_text(encoding='utf-8'))) for p in TEST.rglob('*.java'))
if m06_tests < 40: raise SystemExit(f'FAILED: expected direct file-binding M06 tests, found {m06_tests}')
print('M06 static implementation checks: PASSED')
print('M06 API endpoints: 6 (5 M06 APIs + COMMON-MEDIA-03)')
print('M06-focused @Test:',m06_tests)
print('Total @Test:',total_tests)
print('smoke-m06.ps1 non-ASCII bytes: 0')

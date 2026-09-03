from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'src/main/java'
TEST=ROOT/'src/test/java'

def text(rel):
    p=ROOT/rel
    if not p.exists(): raise SystemExit(f'MISSING: {p}')
    return p.read_text(encoding='utf-8')

def need(blob,needle,label):
    if needle not in blob: raise SystemExit(f'FAILED: {label}: missing {needle}')

base=MAIN/'com/petlink/modules/content'
required=[
 'controller/AnimalFavoriteController.java','controller/FavoriteController.java','controller/AnnouncementPublicController.java','controller/AnnouncementAdminController.java',
 'dto/CreateAnnouncementRequest.java','dto/PatchAnnouncementRequest.java','dto/AnnouncementVersionRequest.java',
 'entity/Favorite.java','entity/Announcement.java','mapper/FavoriteMapper.java','mapper/AnnouncementMapper.java',
 'service/FavoriteService.java','service/FavoriteTransactionalService.java','service/FavoriteQueryService.java','service/FavoriteResponseAssembler.java',
 'service/AnnouncementService.java','service/AnnouncementTransactionalService.java','service/AnnouncementQueryService.java','service/AnnouncementResponseAssembler.java',
 'vo/FavoriteAnimalResponse.java','vo/FavoriteStateResponse.java','vo/AnnouncementPublicSummaryResponse.java','vo/AnnouncementPublicDetailResponse.java','vo/AnnouncementAdminSummaryResponse.java','vo/AnnouncementAdminDetailResponse.java']
for rel in required:
    if not (base/rel).exists(): raise SystemExit(f'MISSING M07 source: {rel}')

controllers='\n'.join(p.read_text(encoding='utf-8') for p in (base/'controller').glob('*.java'))
for endpoint in ['/{animalId}/favorite','/api/favorites','/me','/api/announcements','/api/admin/announcements','/{announcementId}/publish','/{announcementId}/withdraw']:
    need(controllers,endpoint,'M07 endpoint')
if len(re.findall(r'@(Get|Post|Patch|Delete)Mapping\b',controllers)) != 11:
    raise SystemExit('FAILED: M07 must expose exactly 11 endpoint mappings')
for role in ["hasAnyRole('USER','RESCUER')","hasRole('ADMIN')"]: need(controllers,role,'M07 role boundary')

security=text('src/main/java/com/petlink/config/SecurityConfig.java')
need(security,'"/api/announcements", "/api/announcements/*"','public announcement permitAll')

favorite=text('src/main/java/com/petlink/modules/content/service/FavoriteService.java')
if not (favorite.find('query.find(principal,animalId)') < favorite.find('tx.create(principal,animalId)')):
    raise SystemExit('FAILED: existing favorite lookup must precede Animal lock/create')
need(favorite,'uk_favorite_user_animal','named favorite unique recovery')
need(favorite,'if(!hasConstraint','other constraint passthrough')

favorite_tx=text('src/main/java/com/petlink/modules/content/service/FavoriteTransactionalService.java')
need(favorite_tx,'animalMapper.selectForShare(animalId)','Animal FOR SHARE')
need(favorite_tx,'"AVAILABLE".equals(animal.getStatus())','new favorite AVAILABLE condition')
need(favorite_tx,'deleteByUserAndAnimal','idempotent physical delete')

animal_mapper=text('src/main/java/com/petlink/modules/animal/mapper/AnimalMapper.java')
need(animal_mapper,'FOR SHARE','favorite share lock SQL')

favorite_mapper=text('src/main/java/com/petlink/modules/content/mapper/FavoriteMapper.java')
need(favorite_mapper,'ORDER BY created_at DESC,id DESC','favorite page order')
need(favorite_mapper,'DELETE FROM favorite WHERE user_id=#{userId} AND animal_id=#{animalId}','scoped delete')

favorite_response=text('src/main/java/com/petlink/modules/content/service/FavoriteResponseAssembler.java')
need(favorite_response,'animalAccess.isVisible','existing Animal media visibility')
need(favorite_response,'existsByAnimalAndUser','final adopter media visibility')
need(favorite_response,'cover!=null && mediaVisible','favorite itself grants no media')

announcement_mapper=text('src/main/java/com/petlink/modules/content/mapper/AnnouncementMapper.java')
for needle in ["status='PUBLISHED' ORDER BY published_at DESC,id DESC","id=#{id} AND status='PUBLISHED'","status IN ('DRAFT','PUBLISHED') AND version=#{version}","status='DRAFT' AND version=#{version}","status='PUBLISHED' AND version=#{version}"]:
    need(announcement_mapper,needle,'announcement state/optimistic SQL')

patch=text('src/main/java/com/petlink/modules/content/dto/PatchAnnouncementRequest.java')
need(patch,'titlePresent','PATCH title presence')
need(patch,'contentPresent','PATCH content presence')

announcement_tx=text('src/main/java/com/petlink/modules/content/service/AnnouncementTransactionalService.java')
for needle in ['(!request.isTitlePresent()&&!request.isContentPresent())','ErrorCode.OPTIMISTIC_LOCK_CONFLICT','"CREATE",null,"DRAFT"','"PUBLISH","DRAFT","PUBLISHED"','"WITHDRAW","PUBLISHED","WITHDRAWN"']:
    need(announcement_tx,needle,'announcement Frozen rule')

operation_log=text('src/main/java/com/petlink/infrastructure/audit/service/OperationLogService.java')
need(operation_log,'"ANNOUNCEMENT", Set.of("CREATE", "PUBLISH", "WITHDRAW")','announcement audit whitelist')

smoke=(ROOT/'scripts/smoke-m07.ps1').read_bytes()
if any(b>=128 for b in smoke): raise SystemExit('FAILED: smoke-m07.ps1 must remain ASCII-only for Windows PowerShell 5.1')
smoke_text=smoke.decode('ascii')
for needle in ['concurrent named-unique recovery','200,201','historical favorite and media projection','draft public detail hidden','version-only patch rejected','stale version rejected','withdrawn cannot republish','M07 smoke flow PASSED']:
    need(smoke_text,needle,'M07 smoke coverage')

m07_tests=sum(len(re.findall(r'@Test\b',p.read_text(encoding='utf-8'))) for p in (TEST/'com/petlink/modules/content').glob('*.java'))
total_tests=sum(len(re.findall(r'@Test\b',p.read_text(encoding='utf-8'))) for p in TEST.rglob('*.java'))
if m07_tests < 45: raise SystemExit(f'FAILED: expected substantial M07 tests, found {m07_tests}')
print('M07 static implementation checks: PASSED')
print('M07 API endpoints: 11 (3 favorite + 8 announcement)')
print('M07-focused @Test:',m07_tests)
print('Total @Test:',total_tests)
print('smoke-m07.ps1 non-ASCII bytes: 0')

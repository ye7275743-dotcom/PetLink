from pathlib import Path
import re,sys

ROOT=Path(__file__).resolve().parents[1]
errors=[]
def read(rel): return (ROOT/rel).read_text('utf-8')
def require(cond,msg):
    if not cond: errors.append(msg)

base=ROOT/'src/main/java/com/petlink/modules/admin'
required=[
    'controller/AdminUserController.java','controller/AdminSupervisionController.java','controller/AdminStatsController.java',
    'mapper/AdminMapper.java','service/AdminUserService.java','service/AdminSupervisionService.java','service/AdminStatsService.java',
    'vo/AdminUserSummaryResponse.java','vo/AdminUserDetailResponse.java','vo/AdminUserActionResponse.java',
    'vo/AdminStatsOverviewResponse.java','vo/AdminStatsTrendPointResponse.java','vo/AdminStatsTrendsResponse.java'
]
for rel in required: require((base/rel).exists(),f'missing M08 file {rel}')

controllers='\n'.join(read('src/main/java/com/petlink/modules/admin/controller/'+x) for x in ['AdminUserController.java','AdminSupervisionController.java','AdminStatsController.java'])
for route in ['/api/admin/users','/{userId}/enable','/{userId}/disable','/{userId}/promote-rescuer','/rescue-tasks','/animals','/adoption-records','/overview','/trends']:
    require(route in controllers,f'missing M08 route fragment {route}')
require(controllers.count('@GetMapping')==7 and controllers.count('@PostMapping')==3,'M08 endpoint count is not 10')
require(controllers.count('@PreAuthorize("hasRole(\'ADMIN\')")')==3,'all M08 controller groups must be ADMIN-only')

mapper=read('src/main/java/com/petlink/modules/admin/mapper/AdminMapper.java')
for token in ['created_at DESC,id DESC','adopted_at DESC,id DESC','finished_at>=#{from}','finished_at<#{toExclusive}',"status='SUCCESS'","GROUP BY DATE_FORMAT(finished_at,'%Y-%m-%d')","GROUP BY DATE_FORMAT(adopted_at,'%Y-%m-%d')"]:
    require(token in mapper,f'AdminMapper missing contract {token}')
users=read('src/main/java/com/petlink/modules/admin/service/AdminUserService.java')
for token in ['admin.getUserId().equals(userId)','"SYS_USER",userId,"ENABLE"','"SYS_USER",userId,"DISABLE"','"SYS_USER",userId,"PROMOTE_RESCUER"','mapper.enableUser','mapper.disableUser','mapper.promoteRescuer']:
    require(token in users,f'AdminUserService missing {token}')
stats=read('src/main/java/com/petlink/modules/admin/service/AdminStatsService.java')
for token in ['PENDING_REVIEW','WAITING_START','TREATING','INVALIDATED','"DAY"','TimeUtils.ZONE.getId()','to.plusDays(1)','getOrDefault(key,0L)']:
    require(token in stats,f'AdminStatsService missing {token}')
summary=read('src/main/java/com/petlink/modules/admin/vo/AdminUserSummaryResponse.java')
require('passwordHash' not in summary,'Admin user response leaks passwordHash')

test_root=ROOT/'src/test/java/com/petlink/modules/admin'
focused=0
for path in test_root.glob('*Test.java'): focused+=len(re.findall(r'@Test\b',path.read_text('utf-8')))
all_tests=sum(len(re.findall(r'@Test\b',p.read_text('utf-8'))) for p in (ROOT/'src/test/java').rglob('*Test.java'))
require(focused>=38,f'M08 focused tests={focused}, expected >=38')
smoke=read('scripts/smoke-m08.ps1')
require(all(ord(c)<128 for c in smoke),'smoke-m08.ps1 must be ASCII-only')
for stage in range(1,13): require(f'[{stage}/12]' in smoke,f'smoke M08 stage {stage} missing')

if errors:
    print('FAILED');print('\n'.join('- '+x for x in errors));sys.exit(1)
print('M08 static implementation checks: PASSED')
print('M08 API endpoints: 10')
print(f'M08-focused @Test: {focused}')
print(f'Total @Test: {all_tests}')
print('smoke-m08.ps1 non-ASCII bytes: 0')

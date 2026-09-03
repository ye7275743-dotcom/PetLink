from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]

def read(rel): return (ROOT/rel).read_text('utf-8')
def require(cond,msg):
    if not cond: errors.append(msg)

M='petlink-mobile/src'
# Standard UniApp CLI layout + Frozen page coverage
require((ROOT/'petlink-mobile/index.html').exists(),'mobile H5 index.html missing')
for f in ['App.vue','main.js','manifest.json','pages.json','uni.scss']:
    require((ROOT/M/f).exists(),f'mobile src/{f} missing')
pages=json.loads(read(f'{M}/pages.json'))['pages']
require(len(pages)==29,f'mobile page count={len(pages)} expected=29')
for item in pages:
    p=ROOT/M/(item['path']+'.vue')
    require(p.exists(),f'missing mobile page {item["path"]}')
    if p.exists():
        page_name=item['path'].split('/')[1]
        source=p.read_text('utf-8')
        require("from '../../composables/routeGuard.js'" in source,f'{item["path"]} missing route guard import')
        require(f"useRouteGuard('{page_name}')" in source,f'{item["path"]} missing exact page-level route guard call')
route_guard=read(f'{M}/composables/routeGuard.js')
require('onShow(() => enforceMobileRoute(name))' in route_guard,'mobile useRouteGuard is not wired to page onShow')

router=read('petlink-pc/src/router/index.js')
for name in ['dashboard','clues','tasks','animals','adoptions','adoption-records','followups','announcements','users','profile']:
    require(f"path:'{name}'" in router,f'missing PC route {name}')

# Domain split / no monolith
require(not (ROOT/M/'components/ScreenView.vue').exists(),'legacy ScreenView.vue still exists')
features=['animal/AnimalScreens.vue','clue/ClueScreens.vue','rescue/RescueScreens.vue','adoption/AdoptionScreens.vue','followup/FollowupScreens.vue','content/ContentScreens.vue','profile/ProfileScreens.vue']
for f in features: require((ROOT/M/'features'/f).exists(),f'missing mobile feature {f}')

# Pagination + filtering
require('usePagedList' in read(f'{M}/composables/pagedList.js'),'mobile usePagedList missing')
animal=read(f'{M}/features/animal/AnimalScreens.vue')
require('species' in animal and 'sex' in animal,'mobile Animal home missing species/sex filtering')
require('loadMore' in animal,'mobile Animal list missing bottom paging')
for v in ['CluesView.vue','TasksView.vue','AnimalsView.vue','AdoptionsView.vue','AdoptionRecordsView.vue','FollowupsView.vue','AnnouncementsView.vue','UsersView.vue']:
    text=read('petlink-pc/src/views/'+v)
    require('el-pagination' in text,f'PC {v} missing el-pagination')
    require('usePagination' in text,f'PC {v} missing usePagination')

# Session refresh / route permission / 401 behavior
app=read(f'{M}/App.vue')
require('onLaunch' in app and 'onShow' in app and 'authApi.me' in app,'mobile startup/resume session refresh missing')
require('enforceCurrentMobileRoute' in app,'mobile resume route enforcement missing')
route_policy=read(f'{M}/composables/routePolicy.js')
require('RESCUER_ONLY_ROUTES' in route_policy and "role !== 'RESCUER'" in route_policy,'mobile RESCUER direct-route guard missing')
mobile_req=read(f'{M}/api/request.js');pc_req=read('petlink-pc/src/api/request.js')
require('statusCode===401' in mobile_req or 'statusCode === 401' in mobile_req,'mobile 401 handler missing')
require('reLaunch' in mobile_req and '/pages/login/index' in mobile_req,'mobile 401 login redirect missing')
require('401' in pc_req and '/login' in pc_req,'PC 401 login redirect missing')
require('isWorkbenchRole' in router and 'auth.logout()' in router,'PC unsupported workbench role logout missing')
require('visibilitychange' in read('petlink-pc/src/main.js'),'PC resume session refresh missing')

# Frozen announcement version request body
pc_api=read('petlink-pc/src/api/index.js');pc_ann=read('petlink-pc/src/views/AnnouncementsView.vue');mob_api=read(f'{M}/api/index.js')
require('adminPublishAnnouncement: (id,version)' in pc_api and 'publish`,{version}' in pc_api,'PC announcement publish version missing')
require('adminWithdrawAnnouncement: (id,version)' in pc_api and 'withdraw`,{version}' in pc_api,'PC announcement withdraw version missing')
require('adminPublishAnnouncement(row.id,row.version)' in pc_ann and 'adminWithdrawAnnouncement(row.id,row.version)' in pc_ann,'PC announcement view does not pass row.version')
require("publish:(id,version)" in mob_api and "data:{version}" in mob_api,'mobile announcement version body missing')

# M08 Frozen response mappings
dashboard=read('petlink-pc/src/views/DashboardView.vue');users_view=read('petlink-pc/src/views/UsersView.vue')
for group in ['rescueClues','rescueTasks','animals','adoptionApplications']:
    require(f"count('{group}'" in dashboard,f'PC dashboard missing M08 overview group {group}')
require("timeZone:'Asia/Shanghai'" in dashboard,'PC M08 trend range is not based on Asia/Shanghai')
require('detail.statistics' in users_view and 'detail.counts' not in users_view,'PC user detail does not consume M08 statistics')

# FAILED task dangerous action dismissal safety
tasks=read('petlink-pc/src/views/TasksView.vue')
require("reason==='cancel'?'cancel':'close'" in tasks,'FAILED resolution does not distinguish cancel and close')
require('if(!action)return' in tasks,'FAILED resolution close/ESC still may call API')

# Loading/error, validation, upload, safe area, a11y
require((ROOT/M/'components/PageState.vue').exists(),'mobile PageState missing')
require((ROOT/'petlink-pc/src/components/ListError.vue').exists(),'PC ListError missing')
require((ROOT/M/'composables/validation.js').exists(),'mobile validation composable missing')
uploader=read(f'{M}/components/ImagePicker.vue')
for token in ['progress','retry','preview']: require(token.lower() in uploader.lower(),f'mobile ImagePicker missing {token}')
shell=read(f'{M}/components/MobileShell.vue')
require('safe-area-inset-top' in shell and 'safe-area-inset-bottom' in shell,'mobile safe-area support missing')
require('100dvh' in shell,'mobile 100dvh support missing')
layout=read('petlink-pc/src/layouts/AdminLayout.vue')
require('NavIcon' in layout and 'aria-label' in layout,'PC accessible SVG navigation missing')

# Reproducible project metadata
for appdir in ['petlink-pc','petlink-mobile']:
    package=json.loads(read(f'{appdir}/package.json'))
    lock=ROOT/appdir/'package-lock.json'
    require(lock.exists(),f'{appdir} package-lock missing')
    if lock.exists():
        try:
            obj=json.loads(lock.read_text('utf-8'))
            require(obj.get('lockfileVersion') in (2,3),f'{appdir} invalid package-lock')
            package_entries=obj.get('packages',{})
            minimum=70 if appdir=='petlink-pc' else 700
            require(len(package_entries)>=minimum,f'{appdir} incomplete package-lock: {len(package_entries)} package entries, expected >= {minimum}')
            lock_root=package_entries.get('',{})
            require(lock_root.get('dependencies',{})==package.get('dependencies',{}),f'{appdir} package-lock root dependencies differ from package.json')
            require(lock_root.get('devDependencies',{})==package.get('devDependencies',{}),f'{appdir} package-lock root devDependencies differ from package.json')
            direct={**package.get('dependencies',{}),**package.get('devDependencies',{})}
            for dep in direct:
                entry=package_entries.get('node_modules/'+dep,{})
                require(bool(entry.get('resolved')) and bool(entry.get('integrity')),f'{appdir} package-lock direct entry incomplete: {dep}')
        except Exception as e: require(False,f'{appdir} package-lock JSON error: {e}')
    require('test' in package.get('scripts',{}),f'{appdir} test script missing')
require((ROOT/'petlink-mobile/vite.config.js').exists(),'mobile vite.config.js missing')
require((ROOT/'petlink-mobile/.env.example').exists(),'mobile .env.example missing')
require('VITE_API_BASE_URL' in read(f'{M}/config.js'),'mobile env API config missing')
require('uniModule?.default' in read('petlink-mobile/vite.config.js'),'mobile UniApp plugin compatibility guard missing')

# Automated quality gate
require((ROOT/'petlink-mobile/tests/frontend-policy.test.mjs').exists(),'mobile policy tests missing')
require((ROOT/'petlink-pc/tests/router-policy.test.mjs').exists(),'PC router tests missing')
require((ROOT/'petlink-pc/tests/danger-action-policy.test.mjs').exists(),'PC dangerous-action test missing')
require((ROOT/'petlink-pc/tests/announcement-contract.test.mjs').exists(),'PC announcement contract test missing')
require((ROOT/'petlink-pc/tests/profile-contract.test.mjs').exists(),'PC profile contract test missing')
mobile_test=read('petlink-mobile/tests/frontend-policy.test.mjs')
require("../src/composables" in mobile_test,'mobile tests still point outside src')
for term in ['validation','pagination','action','upload','idempotency','direct-route','announcement']:
    require(term.lower() in mobile_test.lower(),f'mobile tests missing {term} coverage')

# API coverage and protected media
for client in [ROOT/'petlink-pc/src/api/index.js',ROOT/M/'api/index.js']:
    text=client.read_text('utf-8')
    required=['/auth/register','/auth/login','/rescue-clues/idempotency-keys','/rescue-tasks/','/animals/','/adoption-applications','/follow-ups','/favorites/me','/announcements','/admin/users','/admin/stats/overview']
    for x in required: require(x in text,f'{client} missing API fragment {x}')
for authimg in [ROOT/'petlink-pc/src/components/AuthImage.vue',ROOT/M/'components/AuthImage.vue']:
    require(authimg.exists(),f'missing {authimg}')
    if authimg.exists():
        text=authimg.read_text('utf-8')
        require(('Authorization' in text and 'Bearer' in text) or ('request' in text and 'useAuth' in text),f'{authimg} does not protect JWT media')
coverage=read('docs/frontend/FRONTEND-API-COVERAGE.md')
require('合计：**72** 个 API' in coverage,'API coverage is not 72')

for base in [ROOT/'petlink-pc/public/assets',ROOT/M/'static/assets']:
    require(len(list(base.glob('*.png')))==4,f'{base}: expected 4 Frozen PNG assets')

if errors:
    print('FAILED');print('\n'.join('- '+x for x in errors));sys.exit(1)
print('PASSED')
print('mobile Frozen routes: 29')
print('mobile feature domains: 7')
print('PC workbench pages: 10 (+ login)')
print('Frozen REST APIs documented: 72')
print('Rev4 fixes: complete lockfiles / page-level mobile route guard + wiring test')

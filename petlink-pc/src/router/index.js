import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../store/auth.js'
import { authApi } from '../api/index.js'
import { routeDecision, isWorkbenchRole } from './policy.js'
const PublicView=()=>import('../views/PublicView.vue')
const LoginView=()=>import('../views/LoginView.vue')
const AdminLayout=()=>import('../layouts/AdminLayout.vue')
const routes = [
  {path:'/',component:PublicView,meta:{public:true,publicMode:'home'}},
  {path:'/adopt',component:PublicView,meta:{public:true,publicMode:'adopt'}},
  {path:'/adopt/:id',component:PublicView,meta:{public:true,publicMode:'animal'}},
  {path:'/news',component:PublicView,meta:{public:true,publicMode:'news'}},
  {path:'/news/:id',component:PublicView,meta:{public:true,publicMode:'notice'}},
  {path:'/stories',component:PublicView,meta:{public:true,publicMode:'stories'}},
  {path:'/guide',component:PublicView,meta:{public:true,publicMode:'guide'}},
  {path:'/register',component:()=>import('../views/RegisterView.vue'),meta:{public:true}},
  { path:'/login', component:LoginView, meta:{public:true} },
  { path:'/workbench', component:AdminLayout, children:[
    { path:'', redirect:'/dashboard' },
    { path:'/dashboard', component:()=>import('../views/DashboardView.vue'), meta:{roles:['ADMIN','RESCUER']} },
    { path:'/clues', component:()=>import('../views/CluesView.vue'), meta:{roles:['ADMIN']} },
    { path:'/tasks', component:()=>import('../views/TasksView.vue'), meta:{roles:['ADMIN','RESCUER']} },
    { path:'/animals', component:()=>import('../views/AnimalsView.vue'), meta:{roles:['ADMIN','RESCUER']} },
    { path:'/adoptions', component:()=>import('../views/AdoptionsView.vue'), meta:{roles:['ADMIN']} },
    { path:'/adoption-records', component:()=>import('../views/AdoptionRecordsView.vue'), meta:{roles:['ADMIN']} },
    { path:'/followups', component:()=>import('../views/FollowupsView.vue'), meta:{roles:['ADMIN','RESCUER']} },
    { path:'/announcements', component:()=>import('../views/AnnouncementsView.vue'), meta:{roles:['ADMIN']} },
    { path:'/users', component:()=>import('../views/UsersView.vue'), meta:{roles:['ADMIN']} },
    { path:'/profile', component:()=>import('../views/ProfileView.vue'), meta:{roles:['ADMIN','RESCUER']} }
  ]}
]

const router=createRouter({
  history:createWebHistory(),
  routes,
  scrollBehavior(to,_from,savedPosition){
    if(savedPosition)return savedPosition
    if(to.hash)return {el:to.hash,behavior:'smooth'}
    return {top:0}
  }
})
const pageTitles = {
  '/':'宠链 · 救助与领养','/adopt':'寻找伙伴 | 宠链','/news':'平台公告 | 宠链','/stories':'救助纪实 | 宠链','/guide':'参与指南 | 宠链','/register':'注册 | 宠链',
  '/login': '登录 | 宠链',
  '/dashboard': '总览看板 | 宠链',
  '/clues': '线索审核 | 宠链',
  '/tasks': '救助任务 | 宠链',
  '/animals': '动物档案 | 宠链',
  '/adoptions': '领养审核 | 宠链',
  '/adoption-records': '领养记录 | 宠链',
  '/followups': '回访记录 | 宠链',
  '/announcements': '公告管理 | 宠链',
  '/users': '用户管理 | 宠链',
  '/profile': '个人资料 | 宠链'
}
router.beforeEach(async(to)=>{
  const auth=useAuth()
  if(to.meta.public){
    if(to.path==='/login' && auth.state.token && !isWorkbenchRole(auth.state.user?.roleCode)) auth.logout()
    return routeDecision({isPublic:true,isLogin:to.path==='/login',hasToken:!!auth.state.token,role:auth.state.user?.roleCode,allowedRoles:to.meta.roles})
  }
  if(!auth.state.token) return '/login'
  try{await auth.validateSession(authApi.me,{maxAge:15000})}catch{auth.logout();return '/login'}
  if(!isWorkbenchRole(auth.state.user?.roleCode)){auth.logout();return '/login'}
  return routeDecision({hasToken:true,role:auth.state.user?.roleCode,allowedRoles:to.meta.roles})
})
router.afterEach(to=>{
  if(typeof document!=='undefined') document.title=pageTitles[to.path]||'宠链流浪动物救助中心'
})
export default router

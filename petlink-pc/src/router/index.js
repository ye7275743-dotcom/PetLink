import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../store/auth.js'
import { authApi } from '../api/index.js'
import { routeDecision, isWorkbenchRole } from './policy.js'
import LoginView from '../views/LoginView.vue'
import AdminLayout from '../layouts/AdminLayout.vue'

const routes = [
  { path:'/login', component:LoginView, meta:{public:true} },
  { path:'/', component:AdminLayout, children:[
    { path:'', redirect:'/dashboard' },
    { path:'dashboard', component:()=>import('../views/DashboardView.vue'), meta:{roles:['ADMIN','RESCUER']} },
    { path:'clues', component:()=>import('../views/CluesView.vue'), meta:{roles:['ADMIN']} },
    { path:'tasks', component:()=>import('../views/TasksView.vue'), meta:{roles:['ADMIN','RESCUER']} },
    { path:'animals', component:()=>import('../views/AnimalsView.vue'), meta:{roles:['ADMIN','RESCUER']} },
    { path:'adoptions', component:()=>import('../views/AdoptionsView.vue'), meta:{roles:['ADMIN']} },
    { path:'adoption-records', component:()=>import('../views/AdoptionRecordsView.vue'), meta:{roles:['ADMIN']} },
    { path:'followups', component:()=>import('../views/FollowupsView.vue'), meta:{roles:['ADMIN','RESCUER']} },
    { path:'announcements', component:()=>import('../views/AnnouncementsView.vue'), meta:{roles:['ADMIN']} },
    { path:'users', component:()=>import('../views/UsersView.vue'), meta:{roles:['ADMIN']} },
    { path:'profile', component:()=>import('../views/ProfileView.vue'), meta:{roles:['ADMIN','RESCUER']} }
  ]}
]

const router=createRouter({history:createWebHistory(),routes})
const pageTitles = {
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
  try{await auth.validateSession(authApi.me,{force:!auth.state.lastValidatedAt})}catch{auth.logout();return '/login'}
  if(!isWorkbenchRole(auth.state.user?.roleCode)){auth.logout();return '/login'}
  return routeDecision({hasToken:true,role:auth.state.user?.roleCode,allowedRoles:to.meta.roles})
})
router.afterEach(to=>{
  if(typeof document!=='undefined') document.title=pageTitles[to.path]||'宠链流浪动物救助中心'
})
export default router

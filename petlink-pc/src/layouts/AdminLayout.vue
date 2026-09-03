<template>
  <div class="app-shell">
    <NetworkStatus />
    <a class="skip-link" href="#main-content">跳转到主要内容</a>
    <aside>
      <div class="brand"><NavIcon class="brand-icon" name="spark" :size="24" /> <span>宠链</span><small>流浪动物救助中心</small></div>
      <div class="role"><b>{{ roleText(user?.roleCode) }}</b><br>{{ user?.nickname || user?.account }}</div>
      <nav>
        <router-link v-for="item in menus" :key="item.path" :to="item.path" class="nav-item" active-class="active" :aria-label="item.label" :title="item.label">
          <NavIcon :name="item.icon"/><span class="nav-label">{{ item.label }}</span>
        </router-link>
      </nav>
      <button class="logout" aria-label="退出登录" title="退出登录" @click="doLogout"><span>退出登录</span><svg viewBox="0 0 24 24"><path d="M10 5H5v14h5M13 8l4 4-4 4M8 12h9" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg></button>
    </aside>
    <main id="main-content" tabindex="-1"><router-view /></main>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../store/auth.js'
import NavIcon from '../components/NavIcon.vue'
import NetworkStatus from '../components/NetworkStatus.vue'
import { roleText } from '../utils/displayText.js'
const router=useRouter(); const auth=useAuth(); const user=computed(()=>auth.state.user)
const all=[
  ['/dashboard','dashboard','总览看板',['ADMIN','RESCUER']],['/clues','clues','线索审核',['ADMIN']],['/tasks','tasks','救助任务',['ADMIN','RESCUER']],['/animals','animals','动物档案',['ADMIN','RESCUER']],['/adoptions','adoptions','领养审核',['ADMIN']],['/adoption-records','records','领养记录',['ADMIN']],['/followups','followups','回访记录',['ADMIN','RESCUER']],['/announcements','announcements','公告管理',['ADMIN']],['/users','users','用户管理',['ADMIN']],['/profile','profile','个人资料',['ADMIN','RESCUER']]
]
const menus=computed(()=>all.filter(x=>x[3].includes(user.value?.roleCode)).map(x=>({path:x[0],icon:x[1],label:x[2]})))
function doLogout(){auth.logout();router.replace('/login')}
</script>
<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 264px minmax(0, 1fr);
  min-height: 100vh;
}

aside {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 28px 18px 22px;
  overflow: auto;
  color: #f8f3e8;
  background:
    radial-gradient(circle at 20% 10%, rgba(241, 199, 75, 0.14), transparent 23%),
    linear-gradient(165deg, #173d39 0%, #12332f 68%, #102b29 100%);
  box-shadow: 14px 0 40px rgba(23, 61, 57, 0.13);
  isolation: isolate;
}

aside::before {
  position: absolute;
  inset: 0;
  z-index: -1;
  content: "";
  pointer-events: none;
  opacity: 0.28;
  background-image: linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px), linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 26px 26px;
  mask-image: linear-gradient(to bottom, black, transparent 76%);
}

.brand {
  position: relative;
  padding: 0 12px 24px 42px;
  font-family: var(--display);
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -1px;
}

.brand-icon{position:absolute;top:1px;left:4px;padding:5px;color:var(--yellow);border:1px solid rgba(241,199,75,.8);border-radius:50%}

.brand small {
  display: block;
  margin-top: 10px;
  color: #b9c9c5;
  font: 10px var(--mono);
  letter-spacing: 0.13em;
  opacity: 0.82;
}

.role {
  margin-bottom: 22px;
  padding: 13px 14px;
  color: #cfdbd8;
  font-size: 12px;
  line-height: 1.7;
  background: linear-gradient(135deg, rgba(255, 253, 248, 0.11), rgba(255, 253, 248, 0.04));
  border: 1px solid rgba(255, 253, 248, 0.17);
  border-radius: 12px;
  box-shadow: inset 0 1px rgba(255, 255, 255, 0.08);
}

.role b {
  color: #fff4c9;
  font-family: var(--mono);
}

.role b::before {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin: 0 7px 1px 0;
  content: "";
  background: var(--yellow);
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(241, 199, 75, 0.12);
}

nav {
  display: grid;
  gap: 5px;
}

.nav-item {
  position: relative;
  display: flex;
  gap: 13px;
  align-items: center;
  min-height: 46px;
  padding: 11px 13px;
  color: #c7d5d1;
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: 12px;
  transition: background 160ms ease, color 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.nav-item:hover {
  color: #fff;
  background: rgba(255, 253, 248, 0.1);
  transform: translateX(2px);
}

.nav-item.active {
  color: var(--ink);
  font-weight: 700;
  background: linear-gradient(135deg, #f8d96b 0%, var(--yellow) 100%);
  border-color: rgba(255, 255, 255, 0.28);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.14), inset 0 1px rgba(255, 255, 255, 0.48);
}

.nav-item.active::after {
  position: absolute;
  right: 12px;
  width: 6px;
  height: 6px;
  content: "";
  background: var(--orange);
  border-radius: 50%;
}

.logout {
  position: absolute;
  right: 18px;
  bottom: 22px;
  left: 18px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 10px;
  color: #c7d5d1;
  cursor: pointer;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 12px;
  transition: background 160ms ease, color 160ms ease, border-color 160ms ease;
}

.logout:hover {
  color: #fff;
  background: rgba(255, 253, 248, 0.1);
  border-color: rgba(255, 253, 248, 0.38);
}

.logout svg {
  width: 18px;
  height: 18px;
}

main {
  min-width: 0;
  padding: 40px clamp(24px, 4vw, 64px) 60px;
}

@media (max-width: 860px) {
  .app-shell {
    grid-template-columns: 78px minmax(0, 1fr);
  }

  aside {
    padding: 22px 10px;
  }

  .brand {
    padding: 0 0 25px;
    font-size: 0;
  }

  .brand-icon{left:14px}

  .brand small,
  .role,
  .nav-label {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 12px;
  }

  .nav-item.active::after {
    right: 8px;
  }

  .nav-item:hover::after {
    position: absolute;
    top: 50%;
    left: 62px;
    z-index: 30;
    padding: 7px 9px;
    color: #fff;
    font: 12px var(--mono);
    white-space: nowrap;
    content: attr(aria-label);
    background: #122f2c;
    border-radius: 6px;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.22);
    transform: translateY(-50%);
  }

  .logout span {
    display: none;
  }

  .logout {
    right: 10px;
    left: 10px;
  }
}
</style>

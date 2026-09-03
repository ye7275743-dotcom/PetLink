import request from './request.js'
const p = (id) => encodeURIComponent(String(id))

export const authApi = {
  register: data => request.post('/auth/register', data),
  login: (data, config = {}) => request.post('/auth/login', data, config),
  me: () => request.get('/users/me'),
  patchMe: data => request.patch('/users/me', data),
  uploadTemporary: (file,config={}) => { const fd=new FormData(); fd.append('file',file); return request.post('/files/temporary',fd,{headers:{'Content-Type':'multipart/form-data'},...config}) }
}

export const clueApi = {
  issueIdempotencyKey: () => request.post('/rescue-clues/idempotency-keys'),
  create: (data,key) => request.post('/rescue-clues',data,{headers:{'Idempotency-Key':key}}),
  mine: params => request.get('/rescue-clues/me',{params}),
  detail: id => request.get(`/rescue-clues/${p(id)}`),
  patch: (id,data) => request.patch(`/rescue-clues/${p(id)}`,data),
  addImages: (id,data) => request.post(`/rescue-clues/${p(id)}/images`,data),
  deleteImage: (id,imageId) => request.delete(`/rescue-clues/${p(id)}/images/${p(imageId)}`),
  withdraw: id => request.post(`/rescue-clues/${p(id)}/withdraw`),
  adminList: params => request.get('/admin/rescue-clues',{params}),
  audit: (id,data) => request.post(`/admin/rescue-clues/${p(id)}/audit`,data),
  waiting: params => request.get('/rescue-clues/waiting-acceptance',{params}),
  mediaUrl: imageId => `${request.defaults.baseURL}/media/rescue-clue-images/${p(imageId)}`
}

export const rescueApi = {
  accept: id => request.post(`/rescue-clues/${p(id)}/accept`),
  mine: params => request.get('/rescue-tasks/me',{params}),
  detail: id => request.get(`/rescue-tasks/${p(id)}`),
  start: id => request.post(`/rescue-tasks/${p(id)}/start`),
  addRecord: (id,data) => request.post(`/rescue-tasks/${p(id)}/records`,data),
  records: id => request.get(`/rescue-tasks/${p(id)}/records`),
  submitResult: (id,data) => request.post(`/rescue-tasks/${p(id)}/result`,data),
  adminCancel: (id,data) => request.post(`/admin/rescue-tasks/${p(id)}/cancel`,data),
  failureResolution: (id,data) => request.post(`/admin/rescue-tasks/${p(id)}/failure-resolution`,data)
}

export const animalApi = {
  publicList: params => request.get('/animals',{params}),
  detail: id => request.get(`/animals/${p(id)}`),
  responsible: params => request.get('/animals/responsible/me',{params}),
  patch: (id,data) => request.patch(`/animals/${p(id)}`,data),
  addImages: (id,data) => request.post(`/animals/${p(id)}/images`,data),
  deleteImage: (id,imageId) => request.delete(`/animals/${p(id)}/images/${p(imageId)}`),
  addHealth: (id,data) => request.post(`/animals/${p(id)}/health-records`,data),
  health: id => request.get(`/animals/${p(id)}/health-records`),
  statusAction: (id,data) => request.post(`/animals/${p(id)}/status-actions`,data),
  mediaUrl: imageId => `${request.defaults.baseURL}/media/animal-images/${p(imageId)}`
}

export const adoptionApi = {
  apply: (animalId,data) => request.post(`/animals/${p(animalId)}/adoption-applications`,data),
  mine: params => request.get('/adoption-applications/me',{params}),
  application: id => request.get(`/adoption-applications/${p(id)}`),
  withdraw: id => request.post(`/adoption-applications/${p(id)}/withdraw`),
  adminList: params => request.get('/admin/adoption-applications',{params}),
  audit: (id,data) => request.post(`/admin/adoption-applications/${p(id)}/audit`,data),
  recordsMine: params => request.get('/adoption-records/me',{params}),
  record: id => request.get(`/adoption-records/${p(id)}`),
  overview: animalId => request.get(`/animals/${p(animalId)}/adoption-overview`)
}

export const followupApi = {
  create: (recordId,data) => request.post(`/adoption-records/${p(recordId)}/follow-ups`,data),
  listByRecord: recordId => request.get(`/adoption-records/${p(recordId)}/follow-ups`),
  detail: id => request.get(`/follow-ups/${p(id)}`),
  adminList: params => request.get('/admin/follow-ups',{params}),
  rescuerList: params => request.get('/rescuer/follow-ups',{params}),
  mediaUrl: imageId => `${request.defaults.baseURL}/media/follow-up-images/${p(imageId)}`
}

export const contentApi = {
  favorite: animalId => request.post(`/animals/${p(animalId)}/favorite`),
  unfavorite: animalId => request.delete(`/animals/${p(animalId)}/favorite`),
  favorites: params => request.get('/favorites/me',{params}),
  announcements: params => request.get('/announcements',{params}),
  announcement: id => request.get(`/announcements/${p(id)}`),
  adminCreateAnnouncement: data => request.post('/admin/announcements',data),
  adminAnnouncements: params => request.get('/admin/announcements',{params}),
  adminAnnouncement: id => request.get(`/admin/announcements/${p(id)}`),
  adminPatchAnnouncement: (id,data) => request.patch(`/admin/announcements/${p(id)}`,data),
  adminPublishAnnouncement: (id,version) => request.post(`/admin/announcements/${p(id)}/publish`,{version}),
  adminWithdrawAnnouncement: (id,version) => request.post(`/admin/announcements/${p(id)}/withdraw`,{version})
}

export const adminApi = {
  users: params => request.get('/admin/users',{params}),
  user: id => request.get(`/admin/users/${p(id)}`),
  enableUser: id => request.post(`/admin/users/${p(id)}/enable`),
  disableUser: id => request.post(`/admin/users/${p(id)}/disable`),
  promoteRescuer: id => request.post(`/admin/users/${p(id)}/promote-rescuer`),
  rescueTasks: params => request.get('/admin/rescue-tasks',{params}),
  animals: params => request.get('/admin/animals',{params}),
  adoptionRecords: params => request.get('/admin/adoption-records',{params}),
  statsOverview: () => request.get('/admin/stats/overview'),
  statsTrends: params => request.get('/admin/stats/trends',{params})
}

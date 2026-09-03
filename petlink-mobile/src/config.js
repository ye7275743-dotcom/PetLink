const envBase = (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_API_BASE_URL) || ''
export const API_BASE_URL = String(envBase || '/api').replace(/\/$/,'')

import http from './http'
export const login=(p)=>http.post('/users/login',p)
export const register=(p)=>http.post('/users/register', { role: 'USER', ...p })

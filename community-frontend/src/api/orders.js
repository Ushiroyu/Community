import http from './http'
export const createOrder=(params)=>http.post('/orders/create',null,{params})
export const payOrder=(id)=>http.post(`/orders/${id}/pay`)
export const confirmOrder=(id)=>http.post(`/orders/${id}/confirm`)
export const myOrders=()=>http.get('/orders/me')
export const leaderOrders=(leaderId,{status,page=1,size=10}={})=>
  http.get('/orders/by-leader',{ params:{ leaderId, status, page, size } })

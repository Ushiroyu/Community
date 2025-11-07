import http from './http'

export const getCart = (userId) => http.get('/cart', { params: { userId } })
export const listCart = getCart
export const addToCart = (userId, productId, quantity = 1) =>
  http.post('/cart', null, { params: { userId, productId, quantity } })
export const removeFromCart = (userId, productId) =>
  http.delete(`/cart/${productId}`, { params: { userId } })
export const clearCart = (userId) => http.delete('/cart', { params: { userId } })

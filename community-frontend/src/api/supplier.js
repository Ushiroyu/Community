import http from './http'

export const applySupplier = (userId, companyName) =>
  http.post('/suppliers/apply', null, { params: { userId, companyName } })

export const pendingSuppliers = () => http.get('/suppliers/pending')

export const setSupplierStatus = (id, status) =>
  http.put(`/suppliers/${id}/status`, null, { params: { status } })

export const getSupplier = (id) => http.get(`/suppliers/${id}`)

export const pendingShipments = (supplierId) =>
  http.get(`/shipping/pending/${supplierId}`)

export const shipOrder = (orderId, trackingNo) =>
  http.post(`/shipping/${orderId}/ship`, null, { params: { trackingNo } })

export const listSupplierProducts = (supplierId, { page = 1, size = 20 } = {}) =>
  http.get(`/suppliers/${supplierId}/products`, { params: { page, size } })

export const createSupplierProduct = (supplierId, p) =>
  http.post(`/suppliers/${supplierId}/products`, p)

export const updateSupplierProduct = (supplierId, productId, p) =>
  http.put(`/suppliers/${supplierId}/products/${productId}`, p)

export const setSupplierProductStatus = (supplierId, productId, status) =>
  http.put(`/suppliers/${supplierId}/products/${productId}/status`, null, { params: { status } })

export const me = () => http.get('/suppliers/me')

export const listMyProducts = ({ page = 1, size = 20 } = {}) =>
  http.get('/suppliers/0/products/all-by-user', { params: { page, size } })

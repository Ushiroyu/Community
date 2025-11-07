import http from './http'

export const approveLeader = (leaderId, userId) =>
  http.post(`/admin/approve/leader/${leaderId}`, null, { params: { userId } })

export const approveSupplier = (supplierId, userId) =>
  http.post(`/admin/approve/supplier/${supplierId}`, null, { params: { userId } })

// Dev robustness: try admin-service aggregator first; fallback to order-service directly
export const overviewStats = async (from, to) => {
  try {
    return await http.get('/admin/stats/overview', { params: { from, to } })
  } catch (err) {
    // If admin-service is down or proxy target unavailable, hit order-service directly
    return await http.get('/stats/overview', { params: { from, to } })
  }
}

export const approveProduct = (id, approved = true) =>
  http.put(`/admin/products/${id}/approve`, null, { params: { approved } })

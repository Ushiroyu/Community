import http from './http'

// 鍚庣鐢?page/size
export const pageProducts = (p = {}) => {
  const params = {
    page: p.page ?? p.current ?? 1,
    size: p.size ?? p.pageSize ?? p.limit ?? 12,
    keyword: p.keyword || undefined,
    categoryId: p.categoryId || undefined,
    approved: p.approved
  }
  return http.get('/products', { params })      // 鉂?涓嶈 '/api/products'
}

export const listCategories = () => http.get('/categories')
export const getProduct = (id) => http.get(`/products/${id}`)




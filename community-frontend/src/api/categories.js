import http from './http'

export const listCategories = () => http.get('/categories')
export const createCategory = (p) => http.post('/categories', p)
export const updateCategory = (id, p) => http.put(`/categories/${id}`, p)
export const deleteCategory = (id) => http.delete(`/categories/${id}`)


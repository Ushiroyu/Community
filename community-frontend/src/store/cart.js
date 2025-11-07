import { defineStore } from 'pinia'
export const useCartStore = defineStore('cart', {
  state: ()=>({ items: [] }),
  getters: {
    totalCount: s => s.items.reduce((a,b)=>a+b.quantity,0),
    totalAmount: s => s.items.reduce((a,b)=>a+(b.price||0)*b.quantity,0)
  },
  actions: { setItems(list){ this.items=list||[] }, clear(){ this.items=[] } }
})

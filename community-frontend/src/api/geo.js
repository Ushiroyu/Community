import http from './http'
export const forwardGeo=(address,city)=>http.get('/geo/forward',{params:{address,city}})
export const reverseGeo=(lat,lng)=>http.get('/geo/reverse',{params:{lat,lng}})

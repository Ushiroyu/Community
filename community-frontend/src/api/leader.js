import http from './http'

export const applyLeader = (userId, communityName, communityAddr) =>
  http.post('/leaders/apply', null, { params: { userId, communityName, communityAddr } })

export const pendingLeaders = () => http.get('/leaders/pending')

export const updateLeaderStatus = (leaderId, status) =>
  http.put(`/leaders/${leaderId}/status`, null, { params: { status } })

export const getCommunity = (id) => http.get(`/communities/${id}`)

export const nearbyCommunities = (lat, lng, limit = 5) =>
  http.get('/communities/nearby', { params: { lat, lng, limit } })

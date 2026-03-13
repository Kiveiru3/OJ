import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'application/json'
  }
})

function clearSessionAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  if (window.location.pathname !== '/login') {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
  }
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (!res || typeof res !== 'object') {
      return Promise.reject(new Error('Invalid response format'))
    }
    if (res.code !== 200) {
      const message = res.message || 'Request failed'
      if (res.code === 401) clearSessionAndRedirect()
      return Promise.reject(new Error(message))
    }
    return res
  },
  (error) => {
    const status = error?.response?.status
    const message = error?.response?.data?.message || error.message || 'Network error'
    if (status === 401) clearSessionAndRedirect()
    return Promise.reject(new Error(message))
  }
)

export default request

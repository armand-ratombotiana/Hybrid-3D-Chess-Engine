import { create } from 'zustand'
import { api } from '../services/api'

interface AuthState {
  isAuthenticated: boolean
  user: any | null
  token: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  checkAuth: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  user: null,
  token: null,

  login: async (username: string, password: string) => {
    try {
      const response = await api.post('/api/auth/login', {
        username,
        password,
      })

      const { token, user } = response.data

      localStorage.setItem('authToken', token)
      set({ isAuthenticated: true, user, token })
    } catch (error) {
      throw new Error('Login failed')
    }
  },

  logout: () => {
    localStorage.removeItem('authToken')
    set({ isAuthenticated: false, user: null, token: null })
  },

  checkAuth: () => {
    const token = localStorage.getItem('authToken')
    if (token) {
      set({ isAuthenticated: true, token })
      // Optionally validate token with backend
    }
  },
}))

import type {
  AuthenticatedUser,
  Booking,
  CampusResource,
  CreateBookingInput,
  Credentials,
} from './types'

const API_BASE = import.meta.env.VITE_API_URL ?? '/api'

function authorization(credentials: Credentials) {
  return `Basic ${btoa(`${credentials.email}:${credentials.password}`)}`
}

async function request<T>(
  path: string,
  credentials: Credentials,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      Authorization: authorization(credentials),
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message ?? `Request failed with status ${response.status}`)
  }

  return response.json() as Promise<T>
}

export const api = {
  me: (credentials: Credentials) => request<AuthenticatedUser>('/auth/me', credentials),
  resources: (credentials: Credentials) => request<CampusResource[]>('/resources', credentials),
  bookings: (credentials: Credentials) => request<Booking[]>('/bookings', credentials),
  createBooking: (credentials: Credentials, input: CreateBookingInput) =>
    request<Booking>('/bookings', credentials, { method: 'POST', body: JSON.stringify(input) }),
  approve: (credentials: Credentials, id: number) =>
    request<Booking>(`/bookings/${id}/approve`, credentials, { method: 'PATCH' }),
  reject: (credentials: Credentials, id: number) =>
    request<Booking>(`/bookings/${id}/reject`, credentials, { method: 'PATCH' }),
  cancel: (credentials: Credentials, id: number) =>
    request<Booking>(`/bookings/${id}/cancel`, credentials, { method: 'PATCH' }),
  checkIn: (credentials: Credentials, id: number, code: string) =>
    request<Booking>(`/bookings/${id}/check-in`, credentials, {
      method: 'PATCH',
      body: JSON.stringify({ code }),
    }),
}

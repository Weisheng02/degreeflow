import type {
  ApiError,
  Credentials,
  DegreeGoal,
  GoalInput,
  GoalStatus,
  Session,
  StudySubject,
  SubjectInput,
} from './types'

const API_BASE = import.meta.env.VITE_API_URL ?? '/api'

function authorization(credentials: Credentials) {
  return `Basic ${btoa(`${credentials.email}:${credentials.password}`)}`
}

async function request<T>(path: string, credentials: Credentials, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      Authorization: authorization(credentials),
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

  if (!response.ok) {
    let error: ApiError = {}
    try {
      error = await response.json() as ApiError
    } catch {
      error = {}
    }
    const fieldMessage = error.fields ? Object.values(error.fields)[0] : undefined
    throw new Error(fieldMessage ?? error.message ?? `Request failed with status ${response.status}`)
  }

  return response.json() as Promise<T>
}

export const api = {
  login: async (credentials: Credentials): Promise<Session> => {
    const user = await request<{ email: string; roles: string[] }>('/auth/me', credentials)
    return { credentials, ...user }
  },
  subjects: (credentials: Credentials) => request<StudySubject[]>('/subjects', credentials),
  createSubject: (credentials: Credentials, input: SubjectInput) =>
    request<StudySubject>('/subjects', credentials, { method: 'POST', body: JSON.stringify(input) }),
  updateSubject: (credentials: Credentials, id: number, input: SubjectInput) =>
    request<StudySubject>(`/subjects/${id}`, credentials, { method: 'PUT', body: JSON.stringify(input) }),
  archiveSubject: (credentials: Credentials, id: number) =>
    request<StudySubject>(`/subjects/${id}/archive`, credentials, { method: 'PATCH' }),
  goals: (credentials: Credentials) => request<DegreeGoal[]>('/goals', credentials),
  createGoal: (credentials: Credentials, input: GoalInput) =>
    request<DegreeGoal>('/goals', credentials, { method: 'POST', body: JSON.stringify(input) }),
  updateGoal: (credentials: Credentials, id: number, input: GoalInput) =>
    request<DegreeGoal>(`/goals/${id}`, credentials, { method: 'PUT', body: JSON.stringify(input) }),
  changeGoalStatus: (credentials: Credentials, id: number, status: GoalStatus) =>
    request<DegreeGoal>(`/goals/${id}/status`, credentials, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
}

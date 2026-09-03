export type Credentials = {
  email: string
  password: string
}

export type Session = {
  credentials: Credentials
  email: string
  roles: string[]
}

export type StudySubject = {
  id: number
  code: string
  name: string
  semester: string
  color: string
  active: boolean
  version: number
}

export type SubjectInput = {
  code: string
  name: string
  semester: string
  color: string
}

export type GoalType = 'ASSIGNMENT' | 'STUDY_SESSION' | 'PROJECT_MILESTONE' | 'PORTFOLIO'
export type GoalPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type GoalStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export type DegreeGoal = {
  id: number
  subjectId: number
  subjectCode: string
  subjectName: string
  subjectColor: string
  ownerEmail: string
  title: string
  goalType: GoalType
  priority: GoalPriority
  plannedStart: string | null
  dueAt: string
  notes: string
  evidenceUrl: string | null
  portfolioVisible: boolean
  status: GoalStatus
  completedAt: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export type GoalInput = {
  subjectId: number
  title: string
  goalType: GoalType
  priority: GoalPriority
  plannedStart: string | null
  dueAt: string
  notes: string
  evidenceUrl: string | null
  portfolioVisible: boolean
}

export type ApiError = {
  code?: string
  message?: string
  fields?: Record<string, string>
}

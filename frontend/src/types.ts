export type Role = 'ADMIN' | 'STUDENT'

export interface AuthenticatedUser {
  email: string
  roles: Role[]
}

export interface Credentials {
  email: string
  password: string
}

export interface CampusResource {
  id: number
  name: string
  resourceType: 'ROOM' | 'EQUIPMENT'
  location: string
  capacity: number
  active: boolean
}

export type BookingStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CHECKED_IN'
  | 'CANCELLED'

export interface Booking {
  id: number
  resourceId: number
  resourceName: string
  bookedBy: string
  startTime: string
  endTime: string
  purpose: string
  status: BookingStatus
  checkInCode: string
  checkedInAt: string | null
  version: number
}

export interface CreateBookingInput {
  resourceId: number
  startTime: string
  endTime: string
  purpose: string
}

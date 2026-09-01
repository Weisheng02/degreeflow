export interface CheckInIntent {
  bookingId: number
  code: string
}

const BOOKING_PARAM = 'checkInBooking'
const CODE_PARAM = 'code'

export function readCheckInIntent(url: string): CheckInIntent | null {
  const parsed = new URL(url)
  const bookingId = Number(parsed.searchParams.get(BOOKING_PARAM))
  const code = parsed.searchParams.get(CODE_PARAM)?.trim() ?? ''

  if (!Number.isSafeInteger(bookingId) || bookingId <= 0 || !/^[A-Z0-9-]{4,32}$/i.test(code)) {
    return null
  }

  return { bookingId, code }
}

export function createCheckInUrl(bookingId: number, code: string, currentUrl: string) {
  const url = new URL(currentUrl)
  url.search = ''
  url.hash = ''
  url.searchParams.set(BOOKING_PARAM, String(bookingId))
  url.searchParams.set(CODE_PARAM, code)
  return url.toString()
}

export function removeCheckInIntentFromUrl(currentUrl: string) {
  const url = new URL(currentUrl)
  url.searchParams.delete(BOOKING_PARAM)
  url.searchParams.delete(CODE_PARAM)
  return `${url.pathname}${url.search}${url.hash}`
}

import { expect, test } from '@playwright/test'

test('a scanned QR link survives login and completes an authenticated check-in', async ({ page }) => {
  let checkInRequestBody = ''

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname

    if (path === '/api/auth/me') {
      await route.fulfill({ json: { email: 'student@campus.local', roles: ['STUDENT'] } })
      return
    }

    if (path === '/api/resources') {
      await route.fulfill({ json: [{ id: 1, name: 'Innovation Lab', resourceType: 'ROOM', location: 'Block A', capacity: 8, active: true }] })
      return
    }

    if (path === '/api/bookings/42/check-in' && request.method() === 'PATCH') {
      checkInRequestBody = request.postData() ?? ''
      await route.fulfill({ json: booking('CHECKED_IN') })
      return
    }

    if (path === '/api/bookings') {
      await route.fulfill({ json: [booking(checkInRequestBody ? 'CHECKED_IN' : 'APPROVED')] })
      return
    }

    await route.fulfill({ status: 404, json: { message: 'Unexpected mocked request' } })
  })

  await page.goto('/?checkInBooking=42&code=ABCD1234')
  await expect(page.getByText('Check-in link detected')).toBeVisible()
  if (process.env.CAPTURE_PORTFOLIO) {
    await page.screenshot({ path: '../docs/screenshots/qr-login.png', fullPage: true })
  }

  await page.getByRole('button', { name: 'Enter dashboard' }).click()
  await expect(page.getByRole('heading', { name: 'Confirm booking #42' })).toBeVisible()
  if (process.env.CAPTURE_PORTFOLIO) {
    await page.screenshot({ path: '../docs/screenshots/qr-confirmation.png', fullPage: true })
  }
  await page.getByRole('button', { name: 'Confirm check-in' }).click()

  await expect(page.getByText('QR check-in recorded.')).toBeVisible()
  expect(JSON.parse(checkInRequestBody)).toEqual({ code: 'ABCD1234' })
  await expect(page).toHaveURL('http://127.0.0.1:4173/')
})

function booking(status: 'APPROVED' | 'CHECKED_IN') {
  return {
    id: 42,
    resourceId: 1,
    resourceName: 'Innovation Lab',
    bookedBy: 'student@campus.local',
    startTime: '2030-01-02T02:00:00Z',
    endTime: '2030-01-02T03:00:00Z',
    purpose: 'QR workflow test',
    status,
    checkInCode: 'ABCD1234',
    checkedInAt: status === 'CHECKED_IN' ? '2030-01-02T02:05:00Z' : null,
    version: status === 'CHECKED_IN' ? 1 : 0,
  }
}

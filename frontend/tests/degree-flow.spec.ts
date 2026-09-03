import { expect, test } from '@playwright/test'

type Subject = {
  id: number
  code: string
  name: string
  semester: string
  color: string
  active: boolean
  version: number
}

type Goal = {
  id: number
  subjectId: number
  subjectCode: string
  subjectName: string
  subjectColor: string
  ownerEmail: string
  title: string
  goalType: string
  priority: string
  plannedStart: string | null
  dueAt: string
  notes: string
  evidenceUrl: string | null
  portfolioVisible: boolean
  status: string
  completedAt: string | null
  createdAt: string
  updatedAt: string
  version: number
}

test('student manages subjects and moves a portfolio goal through its workflow', async ({ page }) => {
  const subjects: Subject[] = [subject(1, 'SE', 'Software Engineering', '#EC5B35')]
  const goals: Goal[] = []
  let createdGoalPayload: Record<string, unknown> | null = null
  let updatedGoalPayload: Record<string, unknown> | null = null

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname

    if (path === '/api/auth/me') {
      await route.fulfill({ json: { email: 'student@degreeflow.local', roles: ['STUDENT'] } })
      return
    }
    if (path === '/api/subjects' && request.method() === 'GET') {
      await route.fulfill({ json: subjects.filter((item) => item.active) })
      return
    }
    if (path === '/api/subjects' && request.method() === 'POST') {
      const input = request.postDataJSON()
      const created = { ...subject(2, input.code, input.name, input.color), semester: input.semester }
      subjects.push(created)
      await route.fulfill({ status: 201, json: created })
      return
    }
    if (path === '/api/subjects/2' && request.method() === 'PUT') {
      const input = request.postDataJSON()
      Object.assign(subjects[1], input, { version: 1 })
      await route.fulfill({ json: subjects[1] })
      return
    }
    if (path === '/api/goals' && request.method() === 'GET') {
      await route.fulfill({ json: goals })
      return
    }
    if (path === '/api/goals' && request.method() === 'POST') {
      createdGoalPayload = request.postDataJSON()
      const created = goalFromPayload(createdGoalPayload, subjects, 42)
      goals.push(created)
      await route.fulfill({ status: 201, json: created })
      return
    }
    if (path === '/api/goals/42' && request.method() === 'PUT') {
      updatedGoalPayload = request.postDataJSON()
      Object.assign(goals[0], goalFromPayload(updatedGoalPayload, subjects, 42), { version: 1 })
      await route.fulfill({ json: goals[0] })
      return
    }
    if (path === '/api/goals/42/status' && request.method() === 'PATCH') {
      const { status } = request.postDataJSON()
      goals[0].status = status
      goals[0].completedAt = status === 'COMPLETED' ? new Date().toISOString() : null
      goals[0].version += 1
      await route.fulfill({ json: goals[0] })
      return
    }
    await route.fulfill({ status: 404, json: { message: `Unexpected ${request.method()} ${path}` } })
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Enter workspace' }).click()
  await expect(page.getByRole('heading', { name: 'Know what matters next.' })).toBeVisible()
  await expect(page.getByRole('button', { name: /SE Software Engineering/ })).toBeVisible()

  await page.getByRole('button', { name: 'Add subject' }).click()
  await page.getByLabel('Code').fill('FYP')
  await page.getByLabel('Subject name').fill('Final Year Project')
  await page.getByLabel('Semester').fill('Next Semester')
  await page.getByRole('button', { name: 'Create subject' }).click()
  await expect(page.getByText('Subject added to your degree plan.')).toBeVisible()

  await page.getByRole('button', { name: 'Edit FYP' }).click()
  await page.getByLabel('Subject name').fill('FYP Preparation')
  await page.getByRole('button', { name: 'Save subject' }).click()
  await expect(page.getByText('Subject updated.')).toBeVisible()
  await expect(page.getByRole('button', { name: /FYP FYP Preparation/ })).toBeVisible()

  await page.getByLabel('Goal title').fill('Prepare stakeholder interview')
  await page.getByLabel('Subject').selectOption('2')
  await page.locator('label').filter({ hasText: /^Type/ }).getByRole('combobox').selectOption('PROJECT_MILESTONE')
  await page.getByLabel('Priority').selectOption('HIGH')
  await page.getByLabel('Notes').fill('Interview one real user and record verified requirements.')
  await page.getByLabel(/Evidence URL/).fill('https://example.com/fyp-evidence')
  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: 'Add goal' }).click()
  await expect(page.getByText('Goal added to your plan.')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Prepare stakeholder interview' })).toBeVisible()

  await page.getByRole('button', { name: 'Edit', exact: true }).click()
  await page.getByLabel('Goal title').fill('Prepare FYP stakeholder interview')
  await page.getByRole('button', { name: 'Save changes' }).click()
  await expect(page.getByText('Goal updated.')).toBeVisible()

  await page.getByRole('button', { name: 'Start' }).click()
  await expect(page.getByText('Prepare FYP stakeholder interview marked in progress.')).toBeVisible()
  await page.getByRole('button', { name: 'Complete' }).click()
  await expect(page.getByText('Prepare FYP stakeholder interview marked completed.')).toBeVisible()
  const completedGoal = page.getByRole('article').filter({
    has: page.getByRole('heading', { name: 'Prepare FYP stakeholder interview' }),
  })
  await expect(completedGoal.getByText('Completed', { exact: true })).toBeVisible()

  expect(createdGoalPayload).toMatchObject({
    subjectId: 2,
    goalType: 'PROJECT_MILESTONE',
    priority: 'HIGH',
    portfolioVisible: true,
  })
  expect(updatedGoalPayload).toMatchObject({ title: 'Prepare FYP stakeholder interview' })
})

test('reviewer sees only shared evidence in a read-only workspace', async ({ page }) => {
  let subjectEndpointCalled = false
  const publicGoal = goalFromPayload({
    subjectId: 7,
    title: 'Tested full-stack case study',
    goalType: 'PORTFOLIO',
    priority: 'HIGH',
    plannedStart: null,
    dueAt: '2030-10-10T10:00:00Z',
    notes: 'Architecture, automated tests and public deployment evidence.',
    evidenceUrl: 'https://example.com/case-study',
    portfolioVisible: true,
  }, [subject(7, 'CAREER', 'Career & Portfolio', '#B0702F')], 70)
  publicGoal.status = 'COMPLETED'

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    if (path === '/api/auth/me') {
      await route.fulfill({ json: { email: 'reviewer@degreeflow.local', roles: ['REVIEWER'] } })
      return
    }
    if (path === '/api/goals' && request.method() === 'GET') {
      await route.fulfill({ json: [publicGoal] })
      return
    }
    if (path === '/api/subjects') subjectEndpointCalled = true
    await route.fulfill({ status: 403, json: { message: 'Read-only reviewer' } })
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Reviewer' }).click()
  await page.getByRole('button', { name: 'Enter workspace' }).click()

  await expect(page.getByRole('heading', { name: 'Evidence, not just claims.' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Tested full-stack case study' })).toBeVisible()
  await expect(page.getByText('Read-only by design')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Evidence' })).toHaveAttribute('href', 'https://example.com/case-study')
  await expect(page.getByRole('button', { name: 'Add goal' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Add subject' })).toHaveCount(0)
  expect(subjectEndpointCalled).toBe(false)
})

function subject(id: number, code: string, name: string, color: string): Subject {
  return { id, code, name, semester: 'Current Semester', color, active: true, version: 0 }
}

function goalFromPayload(input: Record<string, any>, subjects: Subject[], id: number): Goal {
  const linked = subjects.find((item) => item.id === Number(input.subjectId)) ?? subjects[0]
  const now = new Date().toISOString()
  return {
    id,
    subjectId: linked.id,
    subjectCode: linked.code,
    subjectName: linked.name,
    subjectColor: linked.color,
    ownerEmail: 'student@degreeflow.local',
    title: input.title,
    goalType: input.goalType,
    priority: input.priority,
    plannedStart: input.plannedStart ?? null,
    dueAt: input.dueAt,
    notes: input.notes ?? '',
    evidenceUrl: input.evidenceUrl ?? null,
    portfolioVisible: input.portfolioVisible ?? false,
    status: 'TODO',
    completedAt: null,
    createdAt: now,
    updatedAt: now,
    version: 0,
  }
}

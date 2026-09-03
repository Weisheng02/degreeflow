import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import {
  Archive,
  BookOpen,
  BriefcaseBusiness,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  Clock3,
  ExternalLink,
  Eye,
  Flag,
  FolderKanban,
  GraduationCap,
  LockKeyhole,
  LogOut,
  Pencil,
  Play,
  Plus,
  RefreshCw,
  Rocket,
  Save,
  ShieldCheck,
  Target,
  TimerReset,
  X,
} from 'lucide-react'
import './App.css'
import { api } from './api'
import type {
  Credentials,
  DegreeGoal,
  GoalInput,
  GoalPriority,
  GoalStatus,
  GoalType,
  Session,
  StudySubject,
  SubjectInput,
} from './types'

const DEMO_ACCOUNTS = {
  student: { email: 'student@degreeflow.local', password: 'Student123!' },
  reviewer: { email: 'reviewer@degreeflow.local', password: 'Reviewer123!' },
} satisfies Record<string, Credentials>

type GoalForm = {
  subjectId: string
  title: string
  goalType: GoalType
  priority: GoalPriority
  plannedStart: string
  dueAt: string
  notes: string
  evidenceUrl: string
  portfolioVisible: boolean
}

type SubjectForm = SubjectInput
type FilterType = 'ALL' | GoalType

function toLocalDateTime(date: Date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}

function newGoalForm(subjectId = ''): GoalForm {
  const due = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
  due.setHours(18, 0, 0, 0)
  return {
    subjectId,
    title: '',
    goalType: 'ASSIGNMENT',
    priority: 'MEDIUM',
    plannedStart: '',
    dueAt: toLocalDateTime(due),
    notes: '',
    evidenceUrl: '',
    portfolioVisible: false,
  }
}

const emptySubject: SubjectForm = {
  code: '',
  name: '',
  semester: 'Current Semester',
  color: '#EC5B35',
}

function readable(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('en-MY', { day: '2-digit', month: 'short', year: 'numeric' })
    .format(new Date(value))
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('en-MY', { hour: '2-digit', minute: '2-digit' })
    .format(new Date(value))
}

function Brand() {
  return <div className="brand-lockup"><span>DF</span><strong>DegreeFlow</strong></div>
}

function LoginScreen({ onAuthenticated }: { onAuthenticated: (session: Session) => void }) {
  const [mode, setMode] = useState<'student' | 'reviewer'>('student')
  const [credentials, setCredentials] = useState<Credentials>(DEMO_ACCOUNTS.student)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function chooseAccount(nextMode: 'student' | 'reviewer') {
    setMode(nextMode)
    setCredentials(DEMO_ACCOUNTS[nextMode])
    setError('')
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      onAuthenticated(await api.login(credentials))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to sign in')
    } finally {
      setSubmitting(false)
    }
  }

  return <main className="login-page">
    <section className="login-intro">
      <Brand />
      <p className="eyebrow">Your degree, made visible</p>
      <h1>Turn every semester into clear progress.</h1>
      <p className="login-summary">
        Plan assignments, protect focused study time, prepare your FYP and collect portfolio evidence in one reliable workflow.
      </p>
      <div className="login-proof">
        <span><BookOpen /> Subject planning</span>
        <span><TimerReset /> Conflict-safe study sessions</span>
        <span><BriefcaseBusiness /> Portfolio evidence</span>
      </div>
    </section>

    <section className="login-panel" aria-labelledby="login-title">
      <div className="panel-icon"><GraduationCap /></div>
      <p className="eyebrow">Demo workspace</p>
      <h2 id="login-title">Open DegreeFlow</h2>
      <p className="muted">Manage the student workspace or inspect only the portfolio items shared with reviewers.</p>

      <div className="account-switcher" aria-label="Demo account">
        <button type="button" className={mode === 'student' ? 'active' : ''} onClick={() => chooseAccount('student')}>
          <GraduationCap /> Student
        </button>
        <button type="button" className={mode === 'reviewer' ? 'active' : ''} onClick={() => chooseAccount('reviewer')}>
          <Eye /> Reviewer
        </button>
      </div>

      <form onSubmit={submit}>
        <label>Email<input type="email" value={credentials.email} onChange={(event) => setCredentials({ ...credentials, email: event.target.value })} autoComplete="username" required /></label>
        <label>Password<input type="password" value={credentials.password} onChange={(event) => setCredentials({ ...credentials, password: event.target.value })} autoComplete="current-password" required /></label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="primary-button" disabled={submitting} type="submit">
          <LockKeyhole /> {submitting ? 'Signing in…' : 'Enter workspace'}
        </button>
      </form>
      <p className="security-note">Demo credentials stay in memory and are never written to browser storage.</p>
    </section>
  </main>
}

function Dashboard({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const reviewer = session.roles.includes('REVIEWER')
  const [subjects, setSubjects] = useState<StudySubject[]>([])
  const [goals, setGoals] = useState<DegreeGoal[]>([])
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState<{ kind: 'success' | 'error'; message: string } | null>(null)
  const [filterType, setFilterType] = useState<FilterType>('ALL')
  const [subjectFilter, setSubjectFilter] = useState<number | null>(null)
  const [showSubjectForm, setShowSubjectForm] = useState(false)
  const [subjectForm, setSubjectForm] = useState<SubjectForm>(emptySubject)
  const [editingSubjectId, setEditingSubjectId] = useState<number | null>(null)
  const [goalForm, setGoalForm] = useState<GoalForm>(newGoalForm())
  const [editingGoalId, setEditingGoalId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)

  async function loadData(preserveNotice = false) {
    setLoading(true)
    if (!preserveNotice) setNotice(null)
    try {
      if (reviewer) {
        setSubjects([])
        setGoals(await api.goals(session.credentials))
      } else {
        const [loadedSubjects, loadedGoals] = await Promise.all([
          api.subjects(session.credentials),
          api.goals(session.credentials),
        ])
        setSubjects(loadedSubjects)
        setGoals(loadedGoals)
        setGoalForm((current) => current.subjectId || loadedSubjects.length === 0
          ? current
          : { ...current, subjectId: String(loadedSubjects[0].id) })
      }
    } catch (reason) {
      setNotice({ kind: 'error', message: reason instanceof Error ? reason.message : 'Unable to load the workspace' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadData() }, [])

  const now = Date.now()
  const weekFromNow = now + 7 * 24 * 60 * 60 * 1000
  const activeGoals = goals.filter((goal) => goal.status === 'TODO' || goal.status === 'IN_PROGRESS')
  const uniqueSubjects = new Set(goals.map((goal) => goal.subjectId)).size
  const stats = {
    subjects: reviewer ? uniqueSubjects : subjects.length,
    dueWeek: activeGoals.filter((goal) => new Date(goal.dueAt).getTime() >= now && new Date(goal.dueAt).getTime() <= weekFromNow).length,
    inProgress: goals.filter((goal) => goal.status === 'IN_PROGRESS').length,
    completed: goals.filter((goal) => goal.status === 'COMPLETED').length,
    portfolioReady: goals.filter((goal) => goal.status === 'COMPLETED' && goal.portfolioVisible && goal.evidenceUrl).length,
    upcoming: activeGoals.filter((goal) => new Date(goal.dueAt).getTime() >= now).length,
    overdue: activeGoals.filter((goal) => new Date(goal.dueAt).getTime() < now).length,
  }

  const visibleGoals = useMemo(() => goals
    .filter((goal) => filterType === 'ALL' || goal.goalType === filterType)
    .filter((goal) => subjectFilter === null || goal.subjectId === subjectFilter)
    .sort((a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime()),
  [goals, filterType, subjectFilter])

  async function saveSubject(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setNotice(null)
    try {
      if (editingSubjectId) {
        await api.updateSubject(session.credentials, editingSubjectId, subjectForm)
        setNotice({ kind: 'success', message: 'Subject updated.' })
      } else {
        await api.createSubject(session.credentials, subjectForm)
        setNotice({ kind: 'success', message: 'Subject added to your degree plan.' })
      }
      setSubjectForm(emptySubject)
      setEditingSubjectId(null)
      setShowSubjectForm(false)
      await loadData(true)
    } catch (reason) {
      setNotice({ kind: 'error', message: reason instanceof Error ? reason.message : 'Unable to save subject' })
    } finally {
      setSaving(false)
    }
  }

  function editSubject(subject: StudySubject) {
    setSubjectForm({ code: subject.code, name: subject.name, semester: subject.semester, color: subject.color })
    setEditingSubjectId(subject.id)
    setShowSubjectForm(true)
  }

  async function archiveSubject(subject: StudySubject) {
    if (!window.confirm(`Archive ${subject.code}? Existing goals will stay in your history.`)) return
    try {
      await api.archiveSubject(session.credentials, subject.id)
      if (subjectFilter === subject.id) setSubjectFilter(null)
      setNotice({ kind: 'success', message: `${subject.code} archived. Existing goals were preserved.` })
      await loadData(true)
    } catch (reason) {
      setNotice({ kind: 'error', message: reason instanceof Error ? reason.message : 'Unable to archive subject' })
    }
  }

  function goalPayload(): GoalInput {
    return {
      subjectId: Number(goalForm.subjectId),
      title: goalForm.title,
      goalType: goalForm.goalType,
      priority: goalForm.priority,
      plannedStart: goalForm.plannedStart ? new Date(goalForm.plannedStart).toISOString() : null,
      dueAt: new Date(goalForm.dueAt).toISOString(),
      notes: goalForm.notes,
      evidenceUrl: goalForm.evidenceUrl || null,
      portfolioVisible: goalForm.portfolioVisible,
    }
  }

  async function saveGoal(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setNotice(null)
    try {
      if (editingGoalId) {
        await api.updateGoal(session.credentials, editingGoalId, goalPayload())
        setNotice({ kind: 'success', message: 'Goal updated.' })
      } else {
        await api.createGoal(session.credentials, goalPayload())
        setNotice({ kind: 'success', message: 'Goal added to your plan.' })
      }
      const nextSubject = goalForm.subjectId || (subjects[0] ? String(subjects[0].id) : '')
      setGoalForm(newGoalForm(nextSubject))
      setEditingGoalId(null)
      await loadData(true)
    } catch (reason) {
      setNotice({ kind: 'error', message: reason instanceof Error ? reason.message : 'Unable to save goal' })
    } finally {
      setSaving(false)
    }
  }

  function editGoal(goal: DegreeGoal) {
    setEditingGoalId(goal.id)
    setGoalForm({
      subjectId: String(goal.subjectId),
      title: goal.title,
      goalType: goal.goalType,
      priority: goal.priority,
      plannedStart: goal.plannedStart ? toLocalDateTime(new Date(goal.plannedStart)) : '',
      dueAt: toLocalDateTime(new Date(goal.dueAt)),
      notes: goal.notes,
      evidenceUrl: goal.evidenceUrl ?? '',
      portfolioVisible: goal.portfolioVisible,
    })
    document.getElementById('goal-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  async function changeStatus(goal: DegreeGoal, status: GoalStatus) {
    try {
      await api.changeGoalStatus(session.credentials, goal.id, status)
      setNotice({ kind: 'success', message: `${goal.title} marked ${readable(status).toLowerCase()}.` })
      await loadData(true)
    } catch (reason) {
      setNotice({ kind: 'error', message: reason instanceof Error ? reason.message : 'Unable to update goal' })
    }
  }

  return <div className="dashboard-shell">
    <header className="app-header">
      <Brand />
      <div className="header-user">
        <div><strong>{session.email}</strong><span>{reviewer ? 'Read-only reviewer' : 'Student workspace'}</span></div>
        <button className="icon-button" type="button" onClick={onLogout} aria-label="Sign out"><LogOut /></button>
      </div>
    </header>

    <main className="dashboard">
      <section className="dashboard-heading">
        <div>
          <p className="eyebrow">{reviewer ? 'Shared portfolio progress' : 'Degree command centre'}</p>
          <h1>{reviewer ? 'Evidence, not just claims.' : 'Know what matters next.'}</h1>
          <p>{reviewer ? 'Only project and portfolio goals deliberately shared by the student appear here.' : 'Connect this semester’s work to your FYP and graduate portfolio.'}</p>
        </div>
        <button className="secondary-button" type="button" onClick={() => void loadData()}><RefreshCw /> Refresh</button>
      </section>

      {notice && <div className={`notice ${notice.kind}`} role="status">
        {notice.kind === 'success' ? <CheckCircle2 /> : <CircleAlert />} {notice.message}
      </div>}

      <section className="stat-grid" aria-label="Degree progress summary">
        <article><BookOpen /><div><strong>{stats.subjects}</strong><span>Active subjects</span></div></article>
        <article><CalendarDays /><div><strong>{stats.dueWeek}</strong><span>Due this week</span></div></article>
        <article><Play /><div><strong>{stats.inProgress}</strong><span>In progress</span></div></article>
        <article><CheckCircle2 /><div><strong>{stats.completed}</strong><span>Completed</span></div></article>
        <article><Rocket /><div><strong>{stats.portfolioReady}</strong><span>Portfolio ready</span></div></article>
        <article><Clock3 /><div><strong>{stats.upcoming}</strong><span>Upcoming deadlines</span></div></article>
        <article className={stats.overdue > 0 ? 'stat-warning' : ''}><CircleAlert /><div><strong>{stats.overdue}</strong><span>Overdue goals</span></div></article>
      </section>

      {!reviewer && <section className="workspace-grid">
        <article className="content-card subject-panel">
          <div className="card-heading">
            <div><p className="eyebrow">Degree structure</p><h2>Your subjects</h2></div>
            <button className="small-button" type="button" onClick={() => {
              setShowSubjectForm((shown) => !shown)
              setEditingSubjectId(null)
              setSubjectForm(emptySubject)
            }}><Plus /> Add subject</button>
          </div>

          {showSubjectForm && <form className="subject-form" onSubmit={saveSubject}>
            <div className="form-row">
              <label>Code<input value={subjectForm.code} onChange={(event) => setSubjectForm({ ...subjectForm, code: event.target.value })} placeholder="FYP" maxLength={24} required /></label>
              <label>Colour<input type="color" value={subjectForm.color} onChange={(event) => setSubjectForm({ ...subjectForm, color: event.target.value.toUpperCase() })} /></label>
            </div>
            <label>Subject name<input value={subjectForm.name} onChange={(event) => setSubjectForm({ ...subjectForm, name: event.target.value })} placeholder="Final Year Project Preparation" maxLength={120} required /></label>
            <label>Semester<input value={subjectForm.semester} onChange={(event) => setSubjectForm({ ...subjectForm, semester: event.target.value })} placeholder="Next Semester" maxLength={80} required /></label>
            <div className="form-actions"><button className="primary-button compact" disabled={saving} type="submit"><Save /> {editingSubjectId ? 'Save subject' : 'Create subject'}</button><button className="text-button" type="button" onClick={() => setShowSubjectForm(false)}>Cancel</button></div>
          </form>}

          <div className="subject-list">
            <button className={`subject-row ${subjectFilter === null ? 'selected' : ''}`} type="button" onClick={() => setSubjectFilter(null)}>
              <span className="subject-mark all"><FolderKanban /></span><span><strong>All subjects</strong><small>Every active goal</small></span><ChevronRight />
            </button>
            {subjects.map((subject) => {
              const openCount = activeGoals.filter((goal) => goal.subjectId === subject.id).length
              return <div className={`subject-row with-actions ${subjectFilter === subject.id ? 'selected' : ''}`} key={subject.id}>
                <button className="subject-select" type="button" onClick={() => setSubjectFilter(subject.id)}>
                  <span className="subject-mark" style={{ background: `${subject.color}20`, color: subject.color }}>{subject.code.slice(0, 3)}</span>
                  <span><strong>{subject.name}</strong><small>{subject.semester} · {openCount} open</small></span>
                </button>
                <div className="row-actions"><button type="button" aria-label={`Edit ${subject.code}`} onClick={() => editSubject(subject)}><Pencil /></button><button type="button" aria-label={`Archive ${subject.code}`} onClick={() => void archiveSubject(subject)}><Archive /></button></div>
              </div>
            })}
          </div>
        </article>

        <article className="content-card" id="goal-form">
          <div className="card-heading"><div><p className="eyebrow">Plan the work</p><h2>{editingGoalId ? 'Edit goal' : 'Add a degree goal'}</h2></div><Target /></div>
          {subjects.length === 0 ? <div className="empty-state">Create your first subject before adding a goal.</div> : <form className="goal-form" onSubmit={saveGoal}>
            <label>Goal title<input value={goalForm.title} onChange={(event) => setGoalForm({ ...goalForm, title: event.target.value })} placeholder="Prepare FYP stakeholder questions" maxLength={160} required /></label>
            <div className="form-row">
              <label>Subject<select value={goalForm.subjectId} onChange={(event) => setGoalForm({ ...goalForm, subjectId: event.target.value })} required>{subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code} · {subject.name}</option>)}</select></label>
              <label>Type<select value={goalForm.goalType} onChange={(event) => {
                const goalType = event.target.value as GoalType
                setGoalForm({ ...goalForm, goalType, portfolioVisible: goalType === 'PROJECT_MILESTONE' || goalType === 'PORTFOLIO' ? goalForm.portfolioVisible : false })
              }}><option value="ASSIGNMENT">Assignment</option><option value="STUDY_SESSION">Study session</option><option value="PROJECT_MILESTONE">Project milestone</option><option value="PORTFOLIO">Portfolio</option></select></label>
            </div>
            <div className="form-row">
              <label>Priority<select value={goalForm.priority} onChange={(event) => setGoalForm({ ...goalForm, priority: event.target.value as GoalPriority })}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></label>
              <label>{goalForm.goalType === 'STUDY_SESSION' ? 'Ends' : 'Due date'}<input type="datetime-local" value={goalForm.dueAt} onChange={(event) => setGoalForm({ ...goalForm, dueAt: event.target.value })} required /></label>
            </div>
            <label>Planned start {goalForm.goalType !== 'STUDY_SESSION' && <span className="optional">Optional</span>}<input type="datetime-local" value={goalForm.plannedStart} onChange={(event) => setGoalForm({ ...goalForm, plannedStart: event.target.value })} required={goalForm.goalType === 'STUDY_SESSION'} /></label>
            <label>Notes<textarea value={goalForm.notes} onChange={(event) => setGoalForm({ ...goalForm, notes: event.target.value })} placeholder="Define the next concrete outcome and what evidence will prove it." maxLength={1000} /></label>
            {(goalForm.goalType === 'PROJECT_MILESTONE' || goalForm.goalType === 'PORTFOLIO') && <>
              <label>Evidence URL <span className="optional">GitHub, live demo or report</span><input type="url" value={goalForm.evidenceUrl} onChange={(event) => setGoalForm({ ...goalForm, evidenceUrl: event.target.value })} placeholder="https://github.com/..." maxLength={500} /></label>
              <label className="checkbox-row"><input type="checkbox" checked={goalForm.portfolioVisible} onChange={(event) => setGoalForm({ ...goalForm, portfolioVisible: event.target.checked })} /><span><strong>Share with reviewers</strong><small>Only enable this when the evidence is ready to show.</small></span></label>
            </>}
            <div className="form-actions"><button className="primary-button" disabled={saving} type="submit"><Save /> {editingGoalId ? 'Save changes' : 'Add goal'}</button>{editingGoalId && <button className="text-button" type="button" onClick={() => { setEditingGoalId(null); setGoalForm(newGoalForm(String(subjects[0]?.id ?? ''))) }}>Cancel edit</button>}</div>
          </form>}
        </article>
      </section>}

      {reviewer && <section className="reviewer-note"><ShieldCheck /><div><p className="eyebrow">Privacy boundary</p><h2>Read-only by design</h2><p>Private assignments and study sessions are excluded by the API. This view contains only project or portfolio goals the student deliberately marked visible.</p></div></section>}

      <section className="content-card goals-panel">
        <div className="card-heading goals-heading">
          <div><p className="eyebrow">{reviewer ? 'Selected evidence' : 'Your next outcomes'}</p><h2>{reviewer ? 'Shared project & portfolio goals' : 'Degree goals'}</h2></div>
          <span>{visibleGoals.length} shown</span>
        </div>
        <div className="filter-bar" aria-label="Goal type filter">
          {(['ALL', 'ASSIGNMENT', 'STUDY_SESSION', 'PROJECT_MILESTONE', 'PORTFOLIO'] as FilterType[]).map((type) => <button type="button" className={filterType === type ? 'active' : ''} key={type} onClick={() => setFilterType(type)}>{type === 'ALL' ? 'All' : readable(type)}</button>)}
        </div>

        {loading ? <div className="empty-state">Loading your degree plan…</div> : visibleGoals.length === 0 ? <div className="empty-state">{reviewer ? 'No portfolio evidence has been shared yet.' : 'No goals match this view. Add one concrete next step above.'}</div> : <div className="goal-list">
          {visibleGoals.map((goal) => {
            const due = new Date(goal.dueAt)
            const overdue = activeGoals.some((item) => item.id === goal.id) && due.getTime() < now
            return <article className={`goal-row ${overdue ? 'overdue' : ''}`} key={goal.id}>
              <div className="goal-date"><strong>{due.getDate().toString().padStart(2, '0')}</strong><span>{due.toLocaleString('en-MY', { month: 'short' })}</span></div>
              <div className="goal-main">
                <div className="goal-title-row"><span className="subject-dot" style={{ background: goal.subjectColor }} /><h3>{goal.title}</h3><span className={`status status-${goal.status.toLowerCase()}`}>{readable(goal.status)}</span><span className={`priority priority-${goal.priority.toLowerCase()}`}>{goal.priority}</span></div>
                <p>{goal.notes || 'No notes added.'}</p>
                <div className="goal-meta"><span><BookOpen /> {goal.subjectCode} · {goal.subjectName}</span><span><Flag /> {readable(goal.goalType)}</span><span><CalendarDays /> {formatDate(goal.dueAt)} · {formatTime(goal.dueAt)}</span>{overdue && <span className="overdue-copy"><CircleAlert /> Overdue</span>}</div>
              </div>
              <div className="goal-evidence">{goal.evidenceUrl ? <a href={goal.evidenceUrl} target="_blank" rel="noreferrer"><ExternalLink /> Evidence</a> : goal.portfolioVisible ? <span><Eye /> Shared</span> : null}</div>
              {!reviewer && <div className="goal-actions">
                {goal.status === 'TODO' && <button className="start" type="button" onClick={() => void changeStatus(goal, 'IN_PROGRESS')}><Play /> Start</button>}
                {(goal.status === 'TODO' || goal.status === 'IN_PROGRESS') && <button className="complete" type="button" onClick={() => void changeStatus(goal, 'COMPLETED')}><Check /> Complete</button>}
                {(goal.status === 'TODO' || goal.status === 'IN_PROGRESS') && <button className="text-button" type="button" onClick={() => editGoal(goal)}><Pencil /> Edit</button>}
                {(goal.status === 'TODO' || goal.status === 'IN_PROGRESS') && <button className="text-button" type="button" onClick={() => void changeStatus(goal, 'CANCELLED')}><X /> Cancel</button>}
              </div>}
            </article>
          })}
        </div>}
      </section>
    </main>
  </div>
}

function App() {
  const [session, setSession] = useState<Session | null>(null)
  return session
    ? <Dashboard session={session} onLogout={() => setSession(null)} />
    : <LoginScreen onAuthenticated={setSession} />
}

export default App

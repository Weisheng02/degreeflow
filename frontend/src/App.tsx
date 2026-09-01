import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import {
  ArrowRight,
  Building2,
  CalendarCheck2,
  Check,
  CheckCircle2,
  Clock3,
  DoorOpen,
  KeyRound,
  Laptop2,
  LogOut,
  MapPin,
  PackageOpen,
  Plus,
  RefreshCw,
  ShieldCheck,
  Users,
  X,
} from 'lucide-react'
import { QRCodeSVG } from 'qrcode.react'
import { api } from './api'
import { createCheckInUrl, readCheckInIntent, removeCheckInIntentFromUrl } from './checkIn'
import type { CheckInIntent } from './checkIn'
import type { AuthenticatedUser, Booking, CampusResource, Credentials } from './types'
import './App.css'

interface Session {
  credentials: Credentials
  user: AuthenticatedUser
}

const demoAccounts = {
  student: { email: 'student@campus.local', password: 'Student123!' },
  admin: { email: 'admin@campus.local', password: 'Admin123!' },
}

function localDateTime(hoursAhead: number) {
  const date = new Date(Date.now() + hoursAhead * 60 * 60 * 1000)
  date.setMinutes(0, 0, 0)
  const offset = date.getTimezoneOffset()
  return new Date(date.getTime() - offset * 60 * 1000).toISOString().slice(0, 16)
}

function LoginScreen({ onAuthenticated, checkInIntent }: { onAuthenticated: (session: Session) => void; checkInIntent: CheckInIntent | null }) {
  const [credentials, setCredentials] = useState<Credentials>(demoAccounts.student)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function signIn(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const user = await api.me(credentials)
      onAuthenticated({ credentials, user })
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to sign in')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-intro">
        <div className="brand-lockup"><span>CR</span> Campus Reserve</div>
        <p className="eyebrow">Portfolio project · Full-stack vertical slice</p>
        <h1>Book shared campus resources without the scheduling chaos.</h1>
        <p className="login-summary">
          A role-aware booking workflow with conflict prevention, approval and
          check-in—built as the foundation for a larger Software Engineering FYP.
        </p>
        <div className="login-proof">
          <span><ShieldCheck /> Spring Security</span>
          <span><CalendarCheck2 /> Conflict-safe booking</span>
          <span><PackageOpen /> PostgreSQL-ready</span>
        </div>
      </section>

      <section className="login-panel" aria-labelledby="login-title">
        <div className="panel-icon"><KeyRound /></div>
        <p className="eyebrow">Demo access</p>
        <h2 id="login-title">Sign in to the workspace</h2>
        <p className="muted">Choose a demo role or enter the supplied local credentials.</p>
        {checkInIntent && <div className="scan-prompt"><QRCodeSVG value={window.location.href} size={36} /><span><strong>Check-in link detected</strong>Sign in as the booking owner to continue.</span></div>}
        <div className="account-switcher" aria-label="Demo account">
          <button type="button" className={credentials.email === demoAccounts.student.email ? 'active' : ''} onClick={() => setCredentials(demoAccounts.student)}><Users /> Student</button>
          <button type="button" className={credentials.email === demoAccounts.admin.email ? 'active' : ''} onClick={() => setCredentials(demoAccounts.admin)}><ShieldCheck /> Admin</button>
        </div>
        <form onSubmit={signIn}>
          <label>Email<input type="email" value={credentials.email} onChange={(event) => setCredentials({ ...credentials, email: event.target.value })} autoComplete="username" required /></label>
          <label>Password<input type="password" value={credentials.password} onChange={(event) => setCredentials({ ...credentials, password: event.target.value })} autoComplete="current-password" required /></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="primary-button" type="submit" disabled={loading}>{loading ? 'Connecting…' : 'Enter dashboard'} <ArrowRight /></button>
        </form>
        <p className="security-note">Demo credentials stay in memory and are never written to browser storage.</p>
      </section>
    </main>
  )
}

function Dashboard({ session, onLogout, checkInIntent, onClearCheckInIntent }: { session: Session; onLogout: () => void; checkInIntent: CheckInIntent | null; onClearCheckInIntent: () => void }) {
  const [resources, setResources] = useState<CampusResource[]>([])
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [selectedResource, setSelectedResource] = useState<number | ''>('')
  const [startTime, setStartTime] = useState(localDateTime(24))
  const [endTime, setEndTime] = useState(localDateTime(25))
  const [purpose, setPurpose] = useState('Project team planning')
  const isAdmin = session.user.roles.includes('ADMIN')

  const loadData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [resourceData, bookingData] = await Promise.all([api.resources(session.credentials), api.bookings(session.credentials)])
      setResources(resourceData)
      setBookings(bookingData)
      setSelectedResource((current) => current || resourceData[0]?.id || '')
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to load workspace')
    } finally {
      setLoading(false)
    }
  }, [session.credentials])

  useEffect(() => {
    // Loading remote API state is the intended synchronization for this dashboard.
    // oxlint-disable-next-line react/set-state-in-effect
    void loadData()
  }, [loadData])

  const stats = useMemo(() => ({
    resources: resources.length,
    pending: bookings.filter((booking) => booking.status === 'PENDING').length,
    approved: bookings.filter((booking) => booking.status === 'APPROVED').length,
  }), [bookings, resources])

  async function createBooking(event: FormEvent) {
    event.preventDefault()
    if (!selectedResource) return
    setError('')
    setNotice('')
    try {
      await api.createBooking(session.credentials, {
        resourceId: selectedResource,
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        purpose,
      })
      setNotice('Booking submitted for approval.')
      setPurpose('Project team planning')
      await loadData()
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to create booking')
    }
  }

  async function runAction(action: () => Promise<Booking>, success: string) {
    setError('')
    setNotice('')
    try {
      await action()
      setNotice(success)
      await loadData()
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Action failed')
    }
  }

  async function confirmScannedCheckIn() {
    if (!checkInIntent) return
    setError('')
    setNotice('')
    try {
      await api.checkIn(session.credentials, checkInIntent.bookingId, checkInIntent.code)
      onClearCheckInIntent()
      setNotice('QR check-in recorded.')
      await loadData()
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to check in')
    }
  }

  return (
    <div className="dashboard-shell">
      <header className="app-header">
        <div className="brand-lockup"><span>CR</span> Campus Reserve</div>
        <div className="header-user"><div><strong>{session.user.email}</strong><span>{isAdmin ? 'Administrator' : 'Student account'}</span></div><button type="button" className="icon-button" onClick={onLogout} aria-label="Sign out"><LogOut /></button></div>
      </header>
      <main className="dashboard">
        <section className="dashboard-heading">
          <div><p className="eyebrow">Resource operations</p><h1>{isAdmin ? 'Approval workspace' : 'Find your next space.'}</h1><p>{isAdmin ? 'Review campus demand and keep shared resources moving.' : 'Reserve a room or equipment in one conflict-safe workflow.'}</p></div>
          <button className="secondary-button" type="button" onClick={() => void loadData()}><RefreshCw /> Refresh</button>
        </section>
        {checkInIntent && <section className="scan-confirmation" aria-labelledby="scan-title"><div className="scan-confirmation-icon"><QRCodeSVG value={window.location.href} size={46} /></div><div><p className="eyebrow">Scanned check-in</p><h2 id="scan-title">Confirm booking #{checkInIntent.bookingId}</h2><p>The QR code has been verified as a valid link. Your account and booking status will still be checked by the server.</p></div><div className="scan-actions"><button className="primary-button" type="button" onClick={() => void confirmScannedCheckIn()}><Check /> Confirm check-in</button><button className="text-button" type="button" onClick={onClearCheckInIntent}>Cancel</button></div></section>}
        {(error || notice) && <div className={`notice ${error ? 'error' : 'success'}`} role="status">{error ? <X /> : <CheckCircle2 />} {error || notice}</div>}
        <section className="stat-grid" aria-label="Booking summary">
          <article><DoorOpen /><div><strong>{stats.resources}</strong><span>Active resources</span></div></article>
          <article><Clock3 /><div><strong>{stats.pending}</strong><span>Pending approval</span></div></article>
          <article><CheckCircle2 /><div><strong>{stats.approved}</strong><span>Ready to check in</span></div></article>
          <article><ShieldCheck /><div><strong>{isAdmin ? 'ADMIN' : 'STUDENT'}</strong><span>Current access</span></div></article>
        </section>
        <div className="workspace-grid">
          <section className="content-card resources-panel">
            <div className="card-heading"><div><p className="eyebrow">Available inventory</p><h2>Rooms & equipment</h2></div><span>{resources.length} listed</span></div>
            {loading ? <div className="empty-state">Loading resources…</div> : <div className="resource-list">{resources.map((resource) => (
              <button type="button" key={resource.id} className={selectedResource === resource.id ? 'resource-row selected' : 'resource-row'} onClick={() => setSelectedResource(resource.id)}>
                <span className="resource-icon">{resource.resourceType === 'ROOM' ? <Building2 /> : <Laptop2 />}</span>
                <span className="resource-copy"><strong>{resource.name}</strong><span><MapPin /> {resource.location}</span></span>
                <span className="capacity">{resource.capacity > 1 ? `${resource.capacity} seats` : '1 unit'}</span>
                {selectedResource === resource.id && <Check className="selected-check" />}
              </button>
            ))}</div>}
          </section>
          <section className="content-card booking-panel">
            <div className="card-heading"><div><p className="eyebrow">New request</p><h2>Reserve a resource</h2></div><Plus /></div>
            <form className="booking-form" onSubmit={createBooking}>
              <label>Resource<select value={selectedResource} onChange={(event) => setSelectedResource(Number(event.target.value))} required>{resources.map((resource) => <option key={resource.id} value={resource.id}>{resource.name}</option>)}</select></label>
              <div className="form-row"><label>Starts<input type="datetime-local" value={startTime} onChange={(event) => setStartTime(event.target.value)} required /></label><label>Ends<input type="datetime-local" value={endTime} onChange={(event) => setEndTime(event.target.value)} required /></label></div>
              <label>Purpose<textarea value={purpose} onChange={(event) => setPurpose(event.target.value)} maxLength={240} required /></label>
              <button className="primary-button" type="submit" disabled={!selectedResource}>Submit booking <ArrowRight /></button>
            </form>
          </section>
        </div>
        <section className="content-card bookings-panel">
          <div className="card-heading"><div><p className="eyebrow">Workflow evidence</p><h2>{isAdmin ? 'All booking requests' : 'Your bookings'}</h2></div><span>{bookings.length} total</span></div>
          {bookings.length === 0 ? <div className="empty-state">No bookings yet. Select a resource above to create the first one.</div> : <div className="booking-list">{bookings.map((booking) => (
            <article className="booking-row" key={booking.id}>
              <div className="booking-date"><strong>{new Date(booking.startTime).toLocaleDateString('en-MY', { day: '2-digit' })}</strong><span>{new Date(booking.startTime).toLocaleDateString('en-MY', { month: 'short' })}</span></div>
              <div className="booking-main"><div className="booking-title-row"><h3>{booking.resourceName}</h3><span className={`status status-${booking.status.toLowerCase()}`}>{booking.status.replace('_', ' ')}</span></div><p>{booking.purpose}</p><span className="booking-meta"><Clock3 /> {new Date(booking.startTime).toLocaleTimeString('en-MY', { hour: '2-digit', minute: '2-digit' })} – {new Date(booking.endTime).toLocaleTimeString('en-MY', { hour: '2-digit', minute: '2-digit' })}{isAdmin && <> · {booking.bookedBy}</>}</span></div>
              {booking.status === 'APPROVED' && <div className="qr-block" title={`Scan to check in with code ${booking.checkInCode}`}><QRCodeSVG value={createCheckInUrl(booking.id, booking.checkInCode, window.location.href)} size={56} /><span>{booking.checkInCode}</span></div>}
              <div className="booking-actions">
                {isAdmin && booking.status === 'PENDING' && <><button type="button" className="approve" onClick={() => void runAction(() => api.approve(session.credentials, booking.id), 'Booking approved.')}><Check /> Approve</button><button type="button" className="reject" onClick={() => void runAction(() => api.reject(session.credentials, booking.id), 'Booking rejected.')}><X /> Reject</button></>}
                {booking.status === 'APPROVED' && <button type="button" className="approve" onClick={() => void runAction(() => api.checkIn(session.credentials, booking.id, booking.checkInCode), 'Check-in recorded.')}><Check /> Check in</button>}
                {(booking.status === 'PENDING' || booking.status === 'APPROVED') && <button type="button" className="text-button" onClick={() => void runAction(() => api.cancel(session.credentials, booking.id), 'Booking cancelled.')}>Cancel</button>}
              </div>
            </article>
          ))}</div>}
        </section>
      </main>
    </div>
  )
}

export default function App() {
  const [session, setSession] = useState<Session | null>(null)
  const [checkInIntent, setCheckInIntent] = useState<CheckInIntent | null>(() => readCheckInIntent(window.location.href))

  function clearCheckInIntent() {
    window.history.replaceState({}, '', removeCheckInIntentFromUrl(window.location.href))
    setCheckInIntent(null)
  }

  return session
    ? <Dashboard session={session} onLogout={() => setSession(null)} checkInIntent={checkInIntent} onClearCheckInIntent={clearCheckInIntent} />
    : <LoginScreen onAuthenticated={setSession} checkInIntent={checkInIntent} />
}

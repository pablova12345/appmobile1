import { Loader2 } from 'lucide-react'

import { cn } from './cn'

/* Componentes UI calcados de proyecto-erp/Frontend/src/shared/ui (mismas props:
   variant / size / icon / loading, cards "glass"). Al integrar al ERP se usan
   los de ellos. */

export function Spinner({ className }) {
  return <Loader2 className={cn('h-5 w-5 animate-spin text-accent-600', className)} />
}

const BTN_VARIANTS = {
  primary:
    'bg-brand-800 text-white shadow-md shadow-brand-800/20 hover:bg-brand-600 focus-visible:ring-brand-600/40',
  secondary:
    'bg-white/80 text-slate-800 border border-slate-200 shadow-xs hover:bg-white hover:border-slate-300 focus-visible:ring-slate-400/50',
  danger:
    'bg-white/80 text-state-danger border border-state-danger/30 shadow-xs hover:bg-state-danger/10 hover:border-state-danger/50 focus-visible:ring-state-danger/40',
  warning:
    'bg-state-amber text-white shadow-md shadow-state-amber/20 hover:bg-state-orange-deep focus-visible:ring-state-amber/40',
  ghost: 'text-slate-600 hover:bg-slate-100/70 hover:text-slate-900 focus-visible:ring-slate-400/50',
}
const BTN_SIZES = {
  sm: 'px-3 py-1.5 text-xs rounded-lg',
  md: 'px-4 py-2.5 text-xs sm:text-sm rounded-xl',
  lg: 'px-5 py-3 text-sm rounded-xl',
}

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  className = '',
  icon: Icon,
  type = 'button',
  ...props
}) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={cn(
        'inline-flex cursor-pointer items-center justify-center gap-2 font-semibold transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 disabled:cursor-not-allowed disabled:opacity-60',
        BTN_VARIANTS[variant] || BTN_VARIANTS.primary,
        BTN_SIZES[size] || BTN_SIZES.md,
        className,
      )}
      {...props}
    >
      {loading ? (
        <Loader2 className="h-4 w-4 shrink-0 animate-spin" />
      ) : Icon ? (
        <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
      ) : null}
      {children != null && <span>{children}</span>}
    </button>
  )
}

export function Card({ children, className = '', glass = true, ...props }) {
  return (
    <div
      className={cn(
        'rounded-3xl p-6 sm:p-7',
        glass
          ? 'border border-white/60 bg-white/55 shadow-[0_8px_32px_rgba(100,116,139,0.12),inset_0_1px_1px_rgba(255,255,255,0.7)] backdrop-blur-md'
          : 'border border-slate-200/80 bg-white shadow-xs',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  )
}

const BADGE_VARIANTS = {
  success: 'border-state-success/30 bg-state-success/10 text-slate-700',
  accent: 'border-accent-400/30 bg-accent-300/20 text-accent-600',
  neutral: 'border-slate-200 bg-slate-100 text-slate-600',
  warning: 'border-state-amber/40 bg-state-amber/10 text-slate-700',
  danger: 'border-state-danger/30 bg-state-danger/10 text-state-danger',
}
const BADGE_DOT = {
  success: 'bg-state-success',
  danger: 'bg-state-danger',
  warning: 'bg-state-amber',
  accent: 'bg-accent-500',
  neutral: 'bg-slate-400',
}

export function Badge({ children, variant = 'neutral', dot = false, className = '' }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-0.5 text-xs font-semibold',
        BADGE_VARIANTS[variant] || BADGE_VARIANTS.neutral,
        className,
      )}
    >
      {dot && <span className={cn('h-1.5 w-1.5 rounded-full', BADGE_DOT[variant])} />}
      {children}
    </span>
  )
}

export function Input({
  id,
  name,
  label,
  error,
  icon: Icon,
  className = '',
  containerClassName = '',
  type = 'text',
  ...props
}) {
  return (
    <div className={cn('flex flex-col', containerClassName)}>
      {label && (
        <label htmlFor={id || name} className="mb-1.5 block text-sm font-medium text-slate-700">
          {label}
        </label>
      )}
      <div className="relative">
        {Icon && (
          <Icon className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        )}
        <input
          id={id || name}
          name={name}
          type={type}
          aria-invalid={Boolean(error)}
          className={cn(
            'w-full rounded-xl border bg-white/60 py-3 pr-4 text-sm text-slate-900 placeholder:text-slate-400 outline-none transition-colors duration-200 focus:border-accent-500/60 focus:bg-white focus-visible:ring-2 focus-visible:ring-accent-400/40',
            Icon ? 'pl-10' : 'pl-4',
            error ? 'border-state-danger/50' : 'border-slate-200',
            className,
          )}
          {...props}
        />
      </div>
      {error && <p className="mt-1.5 text-xs text-state-danger">{error}</p>}
    </div>
  )
}

export function SectionHeader({ icon: Icon, eyebrow, title, subtitle, actions, className = '' }) {
  return (
    <div
      className={cn(
        'mb-6 flex items-start justify-between gap-3 border-b border-slate-200/60 pb-4',
        className,
      )}
    >
      <div className="flex items-center gap-3">
        {Icon && (
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-accent-50 text-accent-600 ring-1 ring-accent-200">
            <Icon className="h-5 w-5" />
          </span>
        )}
        <div>
          {eyebrow && (
            <p className="text-xs font-semibold uppercase tracking-wider text-accent-600">{eyebrow}</p>
          )}
          {title && <h2 className="text-lg font-bold text-slate-900">{title}</h2>}
          {subtitle && <p className="mt-0.5 text-sm text-slate-500">{subtitle}</p>}
        </div>
      </div>
      {actions}
    </div>
  )
}

export function EmptyState({ icon: Icon, title, subtitle, children, className = '' }) {
  return (
    <div className={cn('flex flex-col items-center justify-center py-12 text-center', className)}>
      {Icon && <Icon size={44} className="mb-3 text-slate-300" aria-hidden="true" />}
      {title && <p className="text-sm font-bold text-slate-600">{title}</p>}
      {subtitle && <p className="mt-1 max-w-sm text-xs text-slate-400">{subtitle}</p>}
      {children}
    </div>
  )
}

export function Alert({ tone = 'danger', children, className = '' }) {
  if (!children) return null
  const tones = {
    danger: 'border-state-danger/30 bg-state-danger/10 text-state-danger',
    warning: 'border-state-amber/40 bg-state-amber/10 text-slate-700',
    accent: 'border-accent-400/30 bg-accent-300/20 text-accent-600',
  }
  return (
    <div className={cn('rounded-xl border px-3 py-2 text-sm', tones[tone], className)}>{children}</div>
  )
}

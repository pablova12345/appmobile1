import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** clsx + tailwind-merge (mismo helper que usa el proyecto-erp). */
export function cn(...inputs) {
  return twMerge(clsx(inputs))
}

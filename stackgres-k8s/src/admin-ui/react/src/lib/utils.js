import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

// Merge conditional class names, resolving conflicting Tailwind utilities.
export function cn(...inputs) {
  return twMerge(clsx(inputs))
}

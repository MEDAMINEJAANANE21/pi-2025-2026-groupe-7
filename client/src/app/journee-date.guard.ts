import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

function formatToday(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function isValidYyyyMmDd(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }

  const [yearStr, monthStr, dayStr] = value.split('-');
  const year = Number(yearStr);
  const month = Number(monthStr);
  const day = Number(dayStr);

  const date = new Date(Date.UTC(year, month - 1, day));
  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  );
}

export const redirectJourneeToTodayGuard: CanActivateFn = () => {
  const router = inject(Router);
  return router.createUrlTree(['/journee', formatToday()]);
};

export const validateJourneeDateGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const date = route.paramMap.get('date');

  if (date && isValidYyyyMmDd(date)) {
    return true;
  }

  return router.createUrlTree(['/journee', formatToday()]);
};

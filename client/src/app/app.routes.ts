import { Routes, ActivatedRouteSnapshot } from '@angular/router';
import { JourneePage } from './pages/Journee/Journee';
import { Solution } from './pages/Solution/Solution';
import { redirectJourneeToTodayGuard, validateJourneeDateGuard } from './journee-date.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'journee',
  },
  {
    path: 'journee',
    pathMatch: 'full',
    canActivate: [redirectJourneeToTodayGuard],
    component: JourneePage,
  },
  {
    path: 'journee/:date',
    title: (route: ActivatedRouteSnapshot) => `Journée du ${route.paramMap.get('date')} | CyberCommandes`,
    canActivate: [validateJourneeDateGuard],
    component: JourneePage,
  },
  {
    path: 'solutions/:solutionId',
    title: (route: ActivatedRouteSnapshot) => `Solution #${route.paramMap.get('solutionId')} | CyberCommandes`,
    component: Solution,
  },
];

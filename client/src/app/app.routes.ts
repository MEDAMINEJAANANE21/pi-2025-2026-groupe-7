import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, Routes } from '@angular/router';
import { formatLongDay, parseIsoDay, today } from './core/util/dates';

/** Une date d'URL invalide renvoie vers aujourd'hui plutôt que vers une page cassée. */
const validDay: CanActivateFn = (route) =>
  parseIsoDay(route.paramMap.get('date') ?? '') ? true : inject(Router).createUrlTree(['/journees', today()]);

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Calendrier · CyberCommandes',
    loadComponent: () => import('./features/calendrier/calendrier-page').then((m) => m.CalendrierPage),
  },
  {
    path: 'journees/:date',
    canActivate: [validDay],
    title: (route: ActivatedRouteSnapshot) => `${formatLongDay(route.paramMap.get('date') ?? '')} · CyberCommandes`,
    loadComponent: () => import('./features/journee/journee-page').then((m) => m.JourneePage),
  },
  {
    path: 'solutions/:solutionId',
    title: (route: ActivatedRouteSnapshot) => `Solution ${route.paramMap.get('solutionId')} · CyberCommandes`,
    loadComponent: () => import('./features/solution/solution-page').then((m) => m.SolutionPage),
  },
  {
    path: 'solutions/:solutionId/tournees/:tourneeId/feuille-de-route',
    title: 'Feuille de route · CyberCommandes',
    loadComponent: () => import('./features/feuille-de-route/feuille-de-route-page').then((m) => m.FeuilleDeRoutePage),
  },
  {
    path: 'livraisons',
    title: 'Livraisons · CyberCommandes',
    loadComponent: () => import('./features/livraisons/livraisons-page').then((m) => m.LivraisonsPage),
  },
  {
    path: 'equipe',
    title: 'Équipe · CyberCommandes',
    loadComponent: () => import('./features/equipe/equipe-page').then((m) => m.EquipePage),
  },
  // Anciennes adresses de la première version, gardées pour les favoris existants.
  { path: 'journee/:date', redirectTo: 'journees/:date' },
  { path: 'journee', redirectTo: '' },
  { path: '**', redirectTo: '' },
];

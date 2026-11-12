import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// This comment is to Test CI 2
bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));

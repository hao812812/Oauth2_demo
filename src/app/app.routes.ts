import { Routes } from '@angular/router';
import { ClientPlatform } from './client-platform/client-platform';
import { Callback } from './callback/callback';
import { Home } from './home/home';

export const routes: Routes = [
  { path: '', component: Home},
  { path: 'callback', component: Callback},
  { path:'login', component: ClientPlatform},
  { path:'home', component: Home},


];

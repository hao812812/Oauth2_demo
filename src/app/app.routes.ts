import { RegisterAccount } from './register-account/register-account';
import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Authentication } from './authentication/authentication';

export const routes: Routes = [
  {path: 'login', component: Login },
  {path:'registerAccount',component:RegisterAccount},
  {path:'auth',component:Authentication},
];

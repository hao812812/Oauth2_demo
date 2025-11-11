import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ClientPlatform } from "./client-platform/client-platform";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ClientPlatform],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('oidc_fronted_client');
}

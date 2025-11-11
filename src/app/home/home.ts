import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  constructor(private router: Router, private route: ActivatedRoute) {}

  login: boolean = false;
  mail?: string;
  name?: string;
  userId?: number;
  firstChar?: string;
  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.mail = params['userMail'];
      this.name = params['userName'];
      this.userId = params['userId'];
      this.firstChar = params['userName']?.charAt(0) || '';
      if (params['checkLogin']) {
        this.login = true;
      }
    });
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }

  logout(){

  }
}

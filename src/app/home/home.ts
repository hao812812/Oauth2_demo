import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonRes, userInfo } from '../interface/commRes';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  constructor(private router: Router, private route: ActivatedRoute, private http: HttpClient) { }

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
  logout() { }
  removeBinding() {
    const url = 'https://oauth2-demo-provider.onrender.com /api/oauth/revokedSessionId';
    const body = {};
    this.http.post<CommonRes<userInfo>>(url, body, { withCredentials: true }).subscribe((res) => {

      if (!res.success) {
        return;
      }
      alert('已解除綁定，請重新登入');
      this.login = false;
    });
  }

  goToHome() {
    this.router.navigate(['/login-home']);
  }
}

import { Component } from '@angular/core';
import { AuthorizationData, CommonRes, CommonResNoData, userInfo } from '../interface/commRes';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login-home',
  imports: [],
  templateUrl: './login-home.html',
  styleUrl: './login-home.css',
})
//使用者登入後 的主頁
export class LoginHome {
  constructor(private http: HttpClient, private router: Router) { }

  userInfo() {
    let url = 'https://oauth2-client-backend.onrender.com/client/oauth/checkToken';
    this.http.get<CommonRes<userInfo>>(url, { withCredentials: true }).subscribe((res) => {
      if (!res.success) {
        alert("請重新登入");
        this.router.navigate(['/home']);
        return;
      };
      const userId = res.data.id;
      const userMail = res.data.mail;
      const userName = res.data.name;
      this.router.navigate(['/home'], {
        queryParams: {
          userId: userId,
          userMail: userMail,
          userName: userName,
          checkLogin: true,
        },
      });
    });
  }
}

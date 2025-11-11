import { HttpClient } from '@angular/common/http';
import { App } from './../app';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonRes, userInfo } from '../interface/commRes';

@Component({
  selector: 'app-callback',
  imports: [],
  templateUrl: './callback.html',
  styleUrl: './callback.css',
})
export class Callback implements OnInit {
  constructor(private http: HttpClient, private route: ActivatedRoute,private router: Router) {}

  ngOnInit(): void {
    //發送code 請求給 授權平台B 後端
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');
    const url = 'http://localhost:8080/api/oauth/sendCode';
    const body = {
      code: code,
      state: state,
    };
    this.http.post<CommonRes<userInfo>>(url, body,{withCredentials: true }).subscribe((res) => {
      if (!res.success) {
        return;
      }
      const userId = res.data.id;
      const userMail = res.data.mail;
      const userName = res.data.name;
      this.router.navigate(['/home'], {
        queryParams: {
          userId: userId,
          userMail: userMail,
          userName: userName,
          checkLogin:true

        },
      });
    });
  }
}

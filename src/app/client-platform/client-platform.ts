import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthorizationData, CommonRes } from '../interface/commRes';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-client-platform',
  imports: [CommonModule, RouterModule],
  templateUrl: './client-platform.html',
  styleUrl: './client-platform.css',
})
export class ClientPlatform implements OnInit {
  constructor(private http: HttpClient) { };

  ngOnInit(): void {
    //先打api確認使用者是否免登入


  }

  //點擊 使用第三方登入 button
  Onbutton() {
    let body = {};
    //取得 authorizationUrl
    let url = 'https://oauth2-client-backend.onrender.com/client/oauth/authorization-url';
    this.http.post<CommonRes<AuthorizationData>>(url, body).subscribe((res) => {
      if (res.success == false) {
        alert("取得授權網址失敗，請稍後再試");
        return;
      }
      //傳送 authorizationUrl 至 B平台 後端
      const authorizationUrl = res.data.authorizationUrl;
      window.location.href = authorizationUrl;
    });
  }
}

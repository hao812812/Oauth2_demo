import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthorizationData, CommonRes } from '../interface/commRes';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-client-platform',
  imports: [CommonModule,RouterModule],
  templateUrl: './client-platform.html',
  styleUrl: './client-platform.css',
})
export class ClientPlatform implements OnInit{
  constructor( private http: HttpClient) {};

  ngOnInit(): void {
    //先打api確認使用者是否免登入


  }

  //點擊 使用第三方登入 button
  Onbutton() {
    let body = {};
    //取得 authorizationUrl
    let url = 'http://localhost:8080/api/oauth/authorization-url';
    this.http.post<CommonRes<AuthorizationData>>(url, body).subscribe((res) => {
      //傳送 authorizationUrl 至 B平台 後端
      const authorizationUrl = res.data.authorizationUrl;
       window.location.href = authorizationUrl;
    });
  }
}

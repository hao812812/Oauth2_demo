import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { apiResponse } from '../interface/memberRegister';

@Component({
  selector: 'app-authentication',
  imports: [FormsModule],
  templateUrl: './authentication.html',
  styleUrl: './authentication.css',
})
export class Authentication implements OnInit {
  constructor(private route: ActivatedRoute, private http: HttpClient) {}
  mail: string = '';
  account: string = '';
  name: string = '';
  id: string = '';
  state: string = '';
  redirectUri: string = '';

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.mail = params['mail'];
      this.account = params['account'];
      this.name = params['name'];
      this.id = params['id'];
      this.state = params['state'];
      this.redirectUri = params['redirectUri'];
    });
  }

  agreeButton() {
    //call 後端 approve api
    const authorizationUrl =
      'http://localhost:9090/api/oauth/approve?' +
      'user_id=' +
      this.id +
      '&state=' +
      this.state +
      '&redirect_uri='+this.redirectUri;
    window.location.href = authorizationUrl;
  }
}

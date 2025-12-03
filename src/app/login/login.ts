import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgbCollapseModule, NgbDate, NgbDatepickerModule } from '@ng-bootstrap/ng-bootstrap';
import { apiResponse } from '../interface/memberRegister';

@Component({
  selector: 'app-login',
  imports: [
    NgbDatepickerModule,
    CommonModule,
    NgbCollapseModule,
    RouterModule,
    ReactiveFormsModule,
    FormsModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  captchaUrl: string = '';
  state: string = '';
  redirectUri: string = '';
  accountGroup = new FormGroup({
    userAccount: new FormControl(''),
    userPassword: new FormControl(''),
    captchaInput: new FormControl(''),
  });

  constructor(private route: ActivatedRoute, private http: HttpClient, private router: Router) {}
  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.state = params['state'];
      this.redirectUri = params['redirect_uri'];
    });
    this.refreshCaptcha();
  }

  //登入
  checkAccount() {
    const url = 'http://localhost:9090/api/login';
    let body = {
      userAccount: this.accountGroup.value.userAccount,
      userPassword: this.accountGroup.value.userPassword,
    };

    this.http.post<apiResponse>(url, body,{withCredentials:true}).subscribe((data) => {
      if (!data.success) {
        alert(data.message);
        return;
      }
      const mail = data.data.mail;
      const account = data.data.account;
      const name = data.data.name;
      const id = data.data.id;
      alert('登入成功');
      this.router.navigate(['/auth'], {
        queryParams: {
          mail: mail,
          account: account,
          name: name,
          id: id,
          state: this.state,
          redirectUri: this.redirectUri,
        },
      });
    });
  }

  refreshCaptcha(): void {


    this.http
      .get('http://localhost:9090/api/captcha/generate', {
        responseType: 'blob', //二進位檔案
        withCredentials: true,
      })
      .subscribe((blob) => {
        const reader = new FileReader();
        reader.onload = () => {
          this.captchaUrl = reader.result as string;
        };
        reader.readAsDataURL(blob);
      });
  }

  //檢查驗證碼
  VerificationCode() {
    this.http
      .post<apiResponse>(
        'http://localhost:9090/api/captcha/verify',
        {
          captcha: this.accountGroup.value.captchaInput,
        },
        { withCredentials: true }
      )
      .subscribe((result) => {
        if (!result.success) {
          alert('驗證碼錯誤');
          this.accountGroup.controls.captchaInput.setValue('');
          this.refreshCaptcha();
          return;
        }
        this.checkAccount();
      });
  }
  //註冊
  register() {
    this.router.navigate(['/registerAccount'], {
      queryParams: {
        state: this.state,
      },
    });
  }
}

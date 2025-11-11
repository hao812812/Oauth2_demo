import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgbDatepickerModule, NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { apiResponse } from '../interface/memberRegister';

@Component({
  selector: 'app-register-account',
  imports: [
    NgbDatepickerModule,
    CommonModule,
    NgbCollapseModule,
    RouterModule,
    ReactiveFormsModule,
  ],
  templateUrl: './register-account.html',
  styleUrl: './register-account.css',
})
export class RegisterAccount implements OnInit {
  isCollapsed = true;
  state: string = '';
  accountGroup = new FormGroup({
    userAccount: new FormControl('', [Validators.required]),
    userMail: new FormControl('', [Validators.required, Validators.email]),
    userName: new FormControl('', [Validators.required]),
    userPassword: new FormControl('', [Validators.required, Validators.minLength(6)]),
    userPasswordCheck: new FormControl('', [Validators.required]),
  });

  constructor(private router: Router, private http: HttpClient, private route: ActivatedRoute) {}
  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      this.state = params['state'];
    });
  }

  registerButton() {
    // 先觸發所有欄位顯示錯誤
    this.accountGroup.markAllAsTouched();

    // 若表單還是無效，跳出提示
    if (this.accountGroup.invalid) {
      alert('請確認所有欄位皆已正確填寫');
      return;
    }
    const formValue = this.accountGroup.value;

    if (formValue.userPassword !== formValue.userPasswordCheck) {
      this.accountGroup.setErrors({ passwordsMismatch: true });
      return;
    }

    let body = {
      userAccount: this.accountGroup.value.userAccount,
      userMail: this.accountGroup.value.userMail,
      userPassword: this.accountGroup.value.userPassword,
      userName: this.accountGroup.value.userName,
    };
    let url = 'http://localhost:9090/api/register';
    this.http.post<apiResponse>(url, body).subscribe((data) => {
      if (!data.success) {
        alert('註冊失敗');
        return;
      }
      alert('註冊成功，請重新登入');
      this.router.navigate(['/login'], {
        queryParams: {
          state: this.state,
        },
      });
    });
  }
}

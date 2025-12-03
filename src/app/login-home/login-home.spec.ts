import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoginHome } from './login-home';

describe('LoginHome', () => {
  let component: LoginHome;
  let fixture: ComponentFixture<LoginHome>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginHome]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoginHome);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

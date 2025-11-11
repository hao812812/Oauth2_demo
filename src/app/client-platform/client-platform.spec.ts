import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClientPlatform } from './client-platform';

describe('ClientPlatform', () => {
  let component: ClientPlatform;
  let fixture: ComponentFixture<ClientPlatform>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientPlatform]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ClientPlatform);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

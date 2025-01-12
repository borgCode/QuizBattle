import {Component, OnInit} from '@angular/core';
import {AbstractControl, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {PlayerService} from "../../api/generated/services/player.service";
import {NgIf} from "@angular/common";
import {LoginStateService} from '../../core/services/login-state-service/login-state.service';
import {AlertMessageService} from '../../core/services/alert-message/alert-message.service';

@Component({
    selector: 'app-settings',
    imports: [
        ReactiveFormsModule,
        NgIf
    ],
    templateUrl: './settings.component.html',
    styleUrl: './settings.component.css'
})
export class SettingsComponent {
    isVisible = false;
    passwordForm: FormGroup;
    submitted = false;


    constructor(
        private fb: FormBuilder,
        private playerService: PlayerService,
        private loginStateService: LoginStateService,
        private alertMessageService: AlertMessageService
    ) {

        this.passwordForm = this.fb.group({
                currentPassword: ['', [
                    Validators.required,
                ]],
                newPassword: ['', [
                    Validators.required,
                    Validators.minLength(8),
                    Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,}$/)
                ]],
                confirmPassword: ['',
                    Validators.required
                ]
            }, {
                validators: this.passwordMatchValidator
            }
        );
    }

    private passwordMatchValidator(form: FormGroup) {
        const newPassword = form.get("newPassword")?.value;
        const confirmPassword = form.get("confirmPassword").value

        if (newPassword && confirmPassword) {
            return newPassword === confirmPassword ? null : {
                passwordMismatch: true
            }
        }
        return null;
    }

    get currentPassword() {
        return this.passwordForm.get('currentPassword');
    }

    get newPassword() {
        return this.passwordForm.get('newPassword');
    }

    get confirmPassword() {
        return this.passwordForm.get('confirmPassword');
    }

  submit() {
    this.submitted = true;

    if (this.passwordForm.valid) {
      const {currentPassword, newPassword, confirmPassword} = this.passwordForm.value;
      this.playerService.changePassword({
        body: {
          playerId: this.loginStateService.loggedInUser.id,
          currentPassword: currentPassword,
          newPassword: newPassword,
          confirmationPassword: confirmPassword
        }
      }).subscribe({
        next: () => {
          this.isVisible = false;
          this.passwordForm.reset();
          this.submitted = false;
          this.alertMessageService.show("Successfully changed password!", "success")
        }
      })
    }
  }
}

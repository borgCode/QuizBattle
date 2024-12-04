import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgForOf, NgIf} from '@angular/common';
import {Router} from '@angular/router';
import {AuthenticationService} from '../../services/services/authentication.service';
import {RegistrationRequest} from '../../services/models/registration-request';

@Component({
    selector: 'app-register',
    imports: [
        FormsModule,
        NgForOf,
        NgIf
    ],
    templateUrl: './register.component.html',
    styleUrl: './register.component.css'
})
export class RegisterComponent {
    registerRequest: RegistrationRequest = {username: '', displayName: '', password: ''};
    errorMsg: Array<string> = [];

    constructor(
        private router: Router,
        private authService: AuthenticationService
    ) {
    }

    register() {
        this.errorMsg = [];
        this.authService.register({
            body: this.registerRequest
        }).subscribe({
                next: () => {
                    this.router.navigate(['login']);
                },
                error: err => {
                    this.errorMsg = err.error.validationErrors;
                }
            }
        )
    }

    login() {
        this.router.navigate(['login']);
    }
}

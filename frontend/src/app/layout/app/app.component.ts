import { Component} from '@angular/core';
import { CommonModule } from '@angular/common';
import {NavbarComponent} from '../../core/components/navbar/navbar.component';
import {RouterOutlet} from '@angular/router';
import {AlertMessageComponent} from '../../core/alert-message/notification/alert-message.component';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, NavbarComponent, RouterOutlet, AlertMessageComponent],
  templateUrl: 'app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Quiz Battle';

}

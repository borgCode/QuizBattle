import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [HttpClientModule, CommonModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.css'
})
export class UsersComponent {
  users: any[] = [];

  // constructor(private backendService: UserService) {
  // }
  //
  // ngOnInit() {
  //   this.getUsers();
  // }
  //
  // getUsers() {
  //   this.backendService.getUsers().subscribe(
  //     (data) => {
  //       this.users = data;
  //
  //   });
  // }

}

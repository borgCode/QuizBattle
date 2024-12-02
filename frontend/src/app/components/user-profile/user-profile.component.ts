import {Component, OnInit} from '@angular/core';
import {User} from '../../model/user';
import {UserService} from '../../service/user.service';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-user-profile',
  imports: [],
  templateUrl: './user-profile.component.html',
  styleUrl: './user-profile.component.css'
})
export class UserProfileComponent implements OnInit{
user!: User;

constructor(
  private route: ActivatedRoute,
  private backendService: UserService
) {}

  ngOnInit() {
    const username = this.route.snapshot.paramMap.get('username')!;
    this.backendService.getUserByName(username).subscribe(
      (data) => {
        this.user = data as User;
      }
    )
  }

}

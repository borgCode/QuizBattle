import {Component, OnInit} from '@angular/core';
import {PlayerDto} from '../../../api/generated/models/player-dto';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {PlayerService} from '../../../api/generated/services/player.service';
import {PlayerCardComponent} from '../../../shared/components/player-card/player-card-component';


@Component({
  selector: 'app-edit-profile',
  imports: [
    PlayerCardComponent
  ],
  templateUrl: './edit-profile.component.html',
  styleUrl: './edit-profile.component.css'
})


export class EditProfileComponent implements OnInit {
  player!: PlayerDto;
  image: string = '';

  constructor(
    private loginStateService: LoginStateService,
    private playerService: PlayerService) {
  }


  ngOnInit() {
    this.player = this.loginStateService.loggedInUser;


    if (!this.player) {
      console.warn('No logged-in user found!');
    }
    this.image = 'data:image/jpeg;base64,' + this.player.base64Image;
  }

  protected readonly handleImageChange = handleImageChange;

  updatePlayerChanges() {

  }
}



function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (file) {
    const reader = new FileReader();
    reader.onload = (event: ProgressEvent<FileReader>): void => {
      if (event.target?.result) {
        this.image = event.target.result as string;
      }
    }
    reader.readAsDataURL(file);
  }


}


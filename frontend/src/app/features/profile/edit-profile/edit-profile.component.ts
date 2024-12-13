import {Component, OnInit} from '@angular/core';
import {PlayerDto} from '../../../api/generated/models/player-dto';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {PlayerService} from '../../../api/generated/services/player.service';
import {PlayerCardComponent} from '../../../shared/components/player-card/player-card-component';
import {firstValueFrom} from 'rxjs';


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
  selectedFile: File | null = null;
  displayName: string;
  isSaving: boolean;

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
    this.displayName = this.player.displayName;
  }


  handleImageChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0];

    if (this.selectedFile) {
      const reader = new FileReader();
      reader.onload = (event: ProgressEvent<FileReader>): void => {
        if (event.target?.result) {
          this.image = event.target.result as string;
        }
      }
      reader.readAsDataURL(this.selectedFile);
    }


  }

  async updatePlayerChanges() {
    if (this.displayName.trim() == this.player.displayName && !this.selectedFile) return;

    //TODO validate form input
    try {
      this.isSaving = true;

      if (this.selectedFile) {
        await firstValueFrom(this.playerService.uploadProfilePicture({
          playerId: this.player.id, body: {
            file: this.selectedFile
          }
        }))
      }

      if (this.displayName !== this.player.displayName) {
        await firstValueFrom(
          this.playerService.updatePlayer({
            body: {playerId: this.player.id, updateField: "DISPLAY_NAME", newDisplayName: this.displayName}
          })
        )
      }

      this.selectedFile = null;
      //TODO refresh player data


    } catch (error) {

    } finally {
      this.isSaving = false;
    }

  }
}






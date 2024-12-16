import {Component, OnInit} from '@angular/core';
import {PlayerDto} from '../../../api/generated/models/player-dto';
import {LoginStateService} from '../../../core/services/login-state-service/login-state.service';
import {PlayerService} from '../../../api/generated/services/player.service';
import {PlayerCardComponent} from '../../../shared/components/player-card/player-card-component';
import {firstValueFrom} from 'rxjs';
import {FormControl, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {NgIf} from '@angular/common';
import {Router} from '@angular/router';


@Component({
  selector: 'app-edit-profile',
  imports: [
    PlayerCardComponent,
    FormsModule,
    ReactiveFormsModule,
    NgIf
  ],
  templateUrl: './edit-profile.component.html',
  styleUrl: './edit-profile.component.css'
})


export class EditProfileComponent implements OnInit {
  player!: PlayerDto;
  image: string = '';
  selectedFile: File | null = null;
  isSaving: boolean;
  displayName: FormControl;

  constructor(
    private loginStateService: LoginStateService,
    private playerService: PlayerService,
    private router: Router) {
  }


  ngOnInit() {
    this.player = this.loginStateService.loggedInUser;


    if (!this.player) {
      console.warn('No logged-in user found!');
    }

    this.image = 'data:image/jpeg;base64,' + this.player.base64Image;

    this.displayName = new FormControl(this.player.displayName, [
      Validators.required
    ]);
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
    const displayNameValue = this.displayName.value.trim();

    if (!displayNameValue) {
      console.error("Display name cannot be blank");
      return;
    }

    if (displayNameValue === this.player.displayName && !this.selectedFile) return;

    try {
      this.isSaving = true;

      if (this.selectedFile) {
        await firstValueFrom(this.playerService.uploadProfilePicture({
          playerId: this.player.id, body: {
            file: this.selectedFile
          }
        }))
      }

      if (displayNameValue !== this.player.displayName) {
        await firstValueFrom(
          this.playerService.updatePlayer({
            body: {playerId: this.player.id, updateField: "DISPLAY_NAME", newDisplayName: displayNameValue}
          })
        )
      }

      this.selectedFile = null;

      this.playerService.getPlayerById({playerId: this.player.id}).subscribe({
        next: value => {
          this.loginStateService.loggedInUser = value;
          this.router.navigate(['/profile'])
        }
      })
    } catch (error) {
      console.log(error)
    } finally {
      this.isSaving = false;
    }

  }
}






import { Component } from '@angular/core';
import {AsyncPipe, NgIf} from "@angular/common";

@Component({
  selector: 'app-whispers-dropdown',
    imports: [
        AsyncPipe,
        NgIf
    ],
  templateUrl: './whispers-dropdown.component.html',
  styleUrl: './whispers-dropdown.component.css'
})
export class WhispersDropdownComponent {

}

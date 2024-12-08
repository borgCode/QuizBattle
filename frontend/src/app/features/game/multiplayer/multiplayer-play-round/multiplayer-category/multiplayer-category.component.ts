import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

@Component({
  selector: 'app-multiplayer-category',
  imports: [
  ],
  templateUrl: './multiplayer-category.component.html',
  styleUrl: './multiplayer-category.component.css'
})
export class MultiplayerCategoryComponent {
  @Input() categories: string[] = [];
  @Output() categorySelected = new EventEmitter<string>();
  selectedCategory: string | null = null;


  selectCategory(category: string) {
    this.selectedCategory = category;
    this.categorySelected.emit(category);
  }
}

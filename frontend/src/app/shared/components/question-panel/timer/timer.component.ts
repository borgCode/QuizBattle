import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';

@Component({
  selector: 'app-timer',
  imports: [],
  templateUrl: './timer.component.html',
  styleUrl: './timer.component.css'
})
export class TimerComponent implements OnInit, OnChanges {
  @Output() timerRanOut = new EventEmitter<void>
  @Input() userClickedOption: boolean = false;
  @Input() userClickedNext: boolean = false;
  duration: number = 60;
  timeLeft: number = 0;
  progressPercentage: number = 100;
  interval: any;
  isRunning: boolean = true;

  ngOnInit() {
    this.startTimer()
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['userClickedOption'] && changes['userClickedOption'].currentValue === true) {
      this.stopTimer();
    }
  }

  startTimer() {
    this.timeLeft = this.duration;
    this.interval = setInterval(() => {
      if (this.timeLeft > 0) {
        this.timeLeft--;
        this.progressPercentage = (this.timeLeft / this.duration) * 100;
      } else {
        this.stopTimer();
        this.timerRanOut.emit();
      }
    }, 250);
  }

  stopTimer() {
    if (this.interval) {
      clearInterval(this.interval);
      this.isRunning = false;
    }
  }

  resetTimer() {
    this.timeLeft = this.duration;
    this.progressPercentage = 100;
  }
}

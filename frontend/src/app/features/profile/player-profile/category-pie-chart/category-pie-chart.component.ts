import {Component, OnInit} from '@angular/core';
import {ArcElement, Chart, PieController} from 'chart.js';
Chart.register(PieController, ArcElement)

@Component({
  selector: 'app-category-pie-chart',
  imports: [],
  templateUrl: './category-pie-chart.component.html',
  styleUrl: './category-pie-chart.component.css'
})
export class CategoryPieChartComponent implements OnInit {
  public chart: any;


  ngOnInit() {
    this.createChart();
  }

  private createChart() {
    this.chart = new Chart("MyChart", {
      type: 'pie',
      data: {
        labels: ['Red', 'Pink', 'Green', 'Yellow', 'Orange', 'Blue',],
        datasets: [{
          label: 'My First Dataset',
          data: [300, 240, 100, 432, 253, 34],
          backgroundColor: [
            'red',
            'pink',
            'green',
            'yellow',
            'orange',
            'blue',
          ],
          hoverOffset: 4
        }],
      },
      options: {
        aspectRatio: 2.5
      }

    });
  }
}

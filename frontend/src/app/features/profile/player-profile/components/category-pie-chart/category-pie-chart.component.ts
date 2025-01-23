import {Component, Input, OnInit} from '@angular/core';
import {ArcElement, Chart, PieController, plugins} from 'chart.js';

Chart.register(PieController, ArcElement, plugins)

@Component({
  selector: 'app-category-pie-chart',
  imports: [],
  templateUrl: './category-pie-chart.component.html',
  styleUrl: './category-pie-chart.component.css'
})
export class CategoryPieChartComponent implements OnInit {
  @Input() categoryStats!: { [key: string]: any };
  mapKeys: string[] = [];
  public chart: any;


  ngOnInit() {
    this.mapKeys = Object.keys(this.categoryStats);
    this.createChart();
    console.log(this.categoryStats)
  }

  private createChart() {
    this.chart = new Chart("MyChart", {
      type: 'pie',
      data: {
        labels: this.mapKeys,
        datasets: [{
          label: 'Correct',
          data: this.mapKeys.map(key => this.categoryStats[key].correct),
          backgroundColor: [
            '#FF0000',
            '#00FF00',
            '#0000FF',
            '#FFFF00',
            '#00FFFF',
            '#FF00FF',
            '#FFA500'
          ],
          hoverOffset: 4
        }],
      },
      options: {
        maintainAspectRatio: false,
        responsive: true,
        plugins: {
          title: {
            display: true,
            text: 'Category performance',
            font: {
              size: 30
            },
            color: 'black',
          },
          legend: {
            labels: {
              font: {
                size: 16
              }
            }
          }
        }
      }

    });
  }
}

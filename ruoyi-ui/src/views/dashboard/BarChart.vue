<template>
  <div :class="className" :style="{height:height,width:width}" />
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'

const animationDuration = 3000

export default {
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '300px'
    }
  },
  data() {
    return {
      chart: null,
      topBooks: [
        { name: 'Java编程思想', value: 98 },
        { name: '深入理解计算机系统', value: 85 },
        { name: '高等数学（第七版）', value: 79 },
        { name: '数据结构与算法分析', value: 65 },
        { name: '计算机网络（谢希仁）', value: 59 },
        { name: '线性代数', value: 52 },
        { name: '离散数学', value: 48 }
      ]
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
    })
  },
  beforeDestroy() {
    if (!this.chart) {
      return
    }
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$el, 'macarons')
      
      // 按销量排序并限制显示前7本书
      const sortedBooks = [...this.topBooks].sort((a, b) => b.value - a.value).slice(0, 7);
      const bookNames = sortedBooks.map(book => book.name);
      const bookValues = sortedBooks.map(book => book.value);

      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: '{b}: {c}本'
        },
        grid: {
          top: 10,
          left: '3%',
          right: '4%',
          bottom: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: bookNames,
          axisTick: {
            alignWithLabel: true
          },
          axisLabel: {
            rotate: 30,
            fontSize: 10,
            interval: 0
          }
        },
        yAxis: {
          type: 'value',
          name: '销量（本）',
          axisTick: {
            show: false
          }
        },
        series: [{
          name: '热门图书',
          type: 'bar',
          barWidth: '60%',
          data: bookValues,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(
              0, 0, 0, 1,
              [
                {offset: 0, color: '#83bff6'},
                {offset: 0.5, color: '#188df0'},
                {offset: 1, color: '#188df0'}
              ]
            )
          },
          emphasis: {
            itemStyle: {
              color: new echarts.graphic.LinearGradient(
                0, 0, 0, 1,
                [
                  {offset: 0, color: '#2378f7'},
                  {offset: 0.7, color: '#2378f7'},
                  {offset: 1, color: '#83bff6'}
                ]
              )
            }
          },
          animationDuration
        }]
      })
    }
  }
}
</script>

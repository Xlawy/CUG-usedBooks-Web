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
      chart: null
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

      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        radar: {
          radius: '66%',
          center: ['50%', '42%'],
          splitNumber: 8,
          splitArea: {
            areaStyle: {
              color: 'rgba(127,95,132,.3)',
              opacity: 1,
              shadowBlur: 45,
              shadowColor: 'rgba(0,0,0,.5)',
              shadowOffsetX: 0,
              shadowOffsetY: 15
            }
          },
          indicator: [
            { name: '浏览量', max: 10000 },
            { name: '搜索次数', max: 8000 },
            { name: '下单量', max: 3000 },
            { name: '发布图书', max: 2000 },
            { name: '评论量', max: 5000 },
            { name: '分享量', max: 4000 }
          ]
        },
        legend: {
          left: 'center',
          bottom: '10',
          data: ['本周活跃度', '上周活跃度', '月平均活跃度']
        },
        series: [{
          type: 'radar',
          symbolSize: 0,
          areaStyle: {
            normal: {
              shadowBlur: 13,
              shadowColor: 'rgba(0,0,0,.2)',
              shadowOffsetX: 0,
              shadowOffsetY: 10,
              opacity: 1
            }
          },
          data: [
            {
              value: [5600, 4500, 1200, 800, 3200, 2100],
              name: '本周活跃度',
              itemStyle: {
                color: '#4992ff'
              }
            },
            {
              value: [5200, 4100, 1100, 720, 2900, 1800],
              name: '上周活跃度',
              itemStyle: {
                color: '#7cffb2'
              }
            },
            {
              value: [5000, 3800, 900, 650, 2700, 1500],
              name: '月平均活跃度',
              itemStyle: {
                color: '#fddd60'
              }
            }
          ],
          animationDuration: animationDuration
        }]
      })
    }
  }
}
</script>

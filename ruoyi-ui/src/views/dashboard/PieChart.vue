<template>
  <div :class="className" :style="{height:height,width:width}" />
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'
import { getCollegeBookStats } from '@/api/statistics'

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
      collegeStats: []
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
      this.fetchData()
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
    async fetchData() {
      try {
        const response = await getCollegeBookStats()
        if (response.data && response.data.success) {
          this.collegeStats = response.data.data
          this.updateChart()
        } else {
          this.$message.error('获取学院图书统计数据失败')
        }
      } catch (error) {
        console.error('获取学院图书统计数据出错:', error)
        this.$message.error('获取学院图书统计数据出错')
      }
    },
    initChart() {
      this.chart = echarts.init(this.$el, 'macarons')
      this.updateChart()
    },
    updateChart() {
      if (!this.chart) {
        return
      }

      const categoryNames = this.collegeStats.map(item => item.name)

      this.chart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c}本 ({d}%)'
        },
        legend: {
          type: 'scroll',
          orient: 'vertical',
          right: 10,
          top: 20,
          bottom: 20,
          data: categoryNames
        },
        series: [
          {
            name: '学院图书',
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['40%', '50%'],
            data: this.collegeStats,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      })
    }
  }
}
</script>

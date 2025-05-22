<template>
  <div class="chart-container">
    <div class="chart-header">
      <el-date-picker
        v-model="selectedDate"
        type="month"
        placeholder="选择月份"
        format="yyyy年MM月"
        value-format="yyyy-MM"
        @change="handleDateChange"
      />
    </div>
    <div :class="className" :style="{height:height,width:width}" />
  </div>
</template>

<script>
import * as echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'
import { parseTime } from '@/utils/ruoyi'

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
      default: '350px'
    },
    autoResize: {
      type: Boolean,
      default: true
    },
    chartData: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      chart: null,
      selectedDate: parseTime(new Date(), '{y}-{m}') // 默认选择当前月份
    }
  },
  watch: {
    chartData: {
      deep: true,
      handler(val) {
        this.setOptions(val)
      }
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
    handleDateChange(date) {
      this.$emit('dateChange', date)
    },
    initChart() {
      this.chart = echarts.init(this.$el.querySelector('.' + this.className), 'macarons')
      this.setOptions(this.chartData)
    },
    setOptions({ expectedData, actualData, xAxisData } = {}) {
      if (!xAxisData) {
        // 如果没有提供x轴数据，则生成当月的天数数组
        const year = parseInt(this.selectedDate.split('-')[0])
        const month = parseInt(this.selectedDate.split('-')[1])
        const daysInMonth = new Date(year, month, 0).getDate()
        xAxisData = Array.from({length: daysInMonth}, (_, i) => (i + 1) + '日')
      }

      this.chart.setOption({
        xAxis: {
          data: xAxisData,
          boundaryGap: false,
          axisTick: {
            show: false
          }
        },
        grid: {
          left: 10,
          right: 10,
          bottom: 20,
          top: 30,
          containLabel: true
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross'
          },
          padding: [5, 10],
          formatter: function(params) {
            let result = params[0].axisValue + '<br/>'
            params.forEach(param => {
              result += param.seriesName + ': ' + param.value + '<br/>'
            })
            return result
          }
        },
        yAxis: {
          axisTick: {
            show: false
          }
        },
        legend: {
          data: ['订单量', '销售量']
        },
        series: [{
          name: '订单量', 
          itemStyle: {
            normal: {
              color: '#FF005A',
              lineStyle: {
                color: '#FF005A',
                width: 2
              }
            }
          },
          smooth: true,
          type: 'line',
          data: expectedData,
          animationDuration: 2800,
          animationEasing: 'cubicInOut'
        },
        {
          name: '销售量',
          smooth: true,
          type: 'line',
          itemStyle: {
            normal: {
              color: '#3888fa',
              lineStyle: {
                color: '#3888fa',
                width: 2
              },
              areaStyle: {
                color: '#f3f8ff'
              }
            }
          },
          data: actualData,
          animationDuration: 2800,
          animationEasing: 'quadraticOut'
        }]
      })
    }
  }
}
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
}

.chart-header {
  padding: 10px;
  text-align: right;
}
</style>

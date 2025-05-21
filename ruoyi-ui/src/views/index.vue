<template>
  <div class="app-container home">
    <!-- 统计面板 -->
    <div class="dashboard-header">
      <div class="header-title">数据统计面板</div>
      <div class="header-actions">
        <span class="last-update-time" v-if="lastUpdateTime">最后更新: {{ lastUpdateTime }}</span>
        <el-button size="small" type="primary" icon="el-icon-refresh" :loading="isLoading" @click="refreshData">刷新数据</el-button>
      </div>
    </div>
    
    <el-alert
      v-if="hasError"
      title="数据加载失败"
      type="error"
      description="获取统计数据时发生错误，请稍后再试或联系管理员"
      show-icon
      :closable="false"
      style="margin-bottom: 20px"
    />
    
    <panel-group v-loading="isLoading" :statistics="statisticsData" @handleSetLineChartData="handleSetLineChartData" />

    <el-row :gutter="32">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <div slot="header" class="clearfix">
            <span>二手书分类统计</span>
            <el-tooltip content="展示各类图书的数量占比" placement="top">
              <i class="el-icon-info" style="margin-left: 10px;color:#909399;"></i>
            </el-tooltip>
          </div>
          <pie-chart />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <div slot="header" class="clearfix">
            <span>年度订单趋势</span>
            <el-tooltip content="展示全年各月的订单量和销售量" placement="top">
              <i class="el-icon-info" style="margin-left: 10px;color:#909399;"></i>
            </el-tooltip>
          </div>
          <line-chart :chart-data="lineChartData" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="32" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <div slot="header" class="clearfix">
            <span>热销图书排行</span>
            <el-tooltip content="展示销量最高的几本图书" placement="top">
              <i class="el-icon-info" style="margin-left: 10px;color:#909399;"></i>
            </el-tooltip>
          </div>
          <bar-chart />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover" v-loading="isLoading">
          <div slot="header" class="clearfix">
            <span>用户活跃度分析</span>
            <el-tooltip content="展示不同维度的用户活跃情况" placement="top">
              <i class="el-icon-info" style="margin-left: 10px;color:#909399;"></i>
            </el-tooltip>
          </div>
          <raddar-chart />
        </el-card>
      </el-col>
    </el-row>

    <el-divider style="margin-top: 20px;" />
    
  </div>
</template>

<script>
import PanelGroup from './dashboard/PanelGroup'
import LineChart from './dashboard/LineChart'
import PieChart from './dashboard/PieChart'
import BarChart from './dashboard/BarChart'
import RaddarChart from './dashboard/RaddarChart'
import { parseTime } from '@/utils/ruoyi'
import { getAllStatistics } from '@/api/statistics'

export default {
  name: "Index",
  components: {
    PanelGroup,
    LineChart,
    PieChart,
    BarChart,
    RaddarChart
  },
  data() {
    return {
      // 版本号
      version: "3.8.8",
      // 统计看板数据
      lineChartData: {
        expectedData: [100, 120, 161, 134, 105, 160, 165, 120, 135, 145, 130, 180],
        actualData: [120, 82, 91, 154, 162, 140, 145, 135, 142, 153, 132, 176]
      },
      statisticsData: {
        users: 0,
        orders: 0,
        books: 0,
        publishedBooks: 0
      },
      isLoading: false,
      hasError: false,
      lastUpdateTime: '',
      // 自动刷新定时器
      refreshTimer: null,
      // 数据刷新间隔（毫秒）
      refreshInterval: 5 * 60 * 1000 // 5分钟
    };
  },
  created() {
    this.fetchStatisticsData();
    // 设置定时刷新
    this.setupAutoRefresh();
  },
  beforeDestroy() {
    // 清除定时器
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank");
    },
    // 设置自动刷新
    setupAutoRefresh() {
      this.refreshTimer = setInterval(() => {
        this.fetchStatisticsData(false);
      }, this.refreshInterval);
    },
    // 手动刷新数据
    refreshData() {
      this.fetchStatisticsData(true);
    },
    handleSetLineChartData(type) {
      switch (type) {
        case 'users':
          this.lineChartData = {
            expectedData: [80, 100, 121, 104, 105, 90, 100, 80, 90, 110, 105, 120],
            actualData: [120, 90, 100, 138, 142, 130, 130, 120, 130, 140, 140, 130]
          }
          break
        case 'orders':
          this.lineChartData = {
            expectedData: [200, 192, 120, 144, 160, 130, 140, 169, 174, 178, 150, 210],
            actualData: [180, 160, 151, 106, 145, 150, 130, 171, 184, 190, 160, 180]
          }
          break
        case 'books':
          this.lineChartData = {
            expectedData: [80, 100, 121, 104, 105, 90, 100, 80, 90, 110, 105, 120],
            actualData: [120, 90, 100, 138, 142, 130, 130, 120, 130, 140, 140, 130]
          }
          break
        case 'publishedBooks':
          this.lineChartData = {
            expectedData: [130, 140, 141, 142, 145, 150, 160, 130, 130, 140, 140, 150],
            actualData: [120, 82, 91, 154, 162, 140, 130, 120, 140, 150, 130, 140]
          }
          break
      }
    },
    // 获取统计数据
    async fetchStatisticsData(showLoading = true) {
      if (showLoading) {
        this.isLoading = true;
      }
      this.hasError = false;
      
      try {
        // 使用新的统计接口获取所有统计数据
        const response = await getAllStatistics();
        
        if (response && response.data && response.data.success) {
          const statistics = response.data.statistics;
          // 更新统计数据
          this.statisticsData.users = statistics.users || 0;
          this.statisticsData.orders = statistics.orders || 0;
          this.statisticsData.books = statistics.books || 0;
          this.statisticsData.publishedBooks = statistics.publishedBooks || 0;
          
          // 更新最后数据刷新时间
          this.lastUpdateTime = parseTime(new Date());
        } else {
          this.hasError = true;
          this.$message.error('获取统计数据失败，请稍后再试');
        }
      } catch (error) {
        console.error('获取统计数据失败:', error);
        this.hasError = true;
        this.$message.error('获取统计数据失败，请稍后再试');
      } finally {
        if (showLoading) {
          this.isLoading = false;
        }
      }
    },
  }
};
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}

.dashboard {
  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .header-title {
      font-size: 20px;
      font-weight: bold;
      color: #303133;
    }
    
    .header-actions {
      display: flex;
      align-items: center;
      
      .last-update-time {
        margin-right: 15px;
        color: #909399;
        font-size: 14px;
      }
    }
  }
}

.chart-card {
  margin-bottom: 20px;
  height: 350px;
  
  .el-card__header {
    padding: 12px 20px;
    
    .clearfix {
      display: flex;
      align-items: center;
    }
  }
}
</style>


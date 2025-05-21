<template>
  <div class="dashboard-container">
    <panel-group :statistics="statisticsData" @handleSetLineChartData="handleSetLineChartData" />

    <el-row :gutter="32">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span>二手书数量统计</span>
          </div>
          <pie-chart />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span>月度订单数量</span>
          </div>
          <line-chart :chart-data="lineChartData" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="32" style="margin-top: 20px;">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span>热销图书分类</span>
          </div>
          <bar-chart />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span>用户活跃度</span>
          </div>
          <raddar-chart />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :sm="24" :lg="24">
        <el-card class="dashboard-info-card" shadow="hover">
          <h2>地大二手书交易管理后台</h2>
          <p>
            随着中国地质大学（武汉）在校生人数的不断增加，学生对教材和参考书籍的需求量大且更新频繁，导致大量二手书交易需求未被充分满足。传统的二手书交易方式（如线下交易、校内论坛、QQ群等）存在信息分散、交易安全性低、效率低下等问题，给学生的学习和经济带来不便。为了优化二手书交易流程，提升交易效率和安全性，基于2022级软件工程系智能终端软件开发课程，本团队开发了地大二手书交易管理后台。本项目旨在通过数字化手段为中国地质大学（武汉）的二手书交易平台提供高效、便捷、可信赖的管理系统，支持平台的运营和用户的交易活动。
            未来，地大二手书交易管理后台将继续优化功能，提升系统性能，扩展更多智能化服务，推动平台在更多高校中的推广和应用，致力于成为全国领先的大学生二手书交易管理系统。
          </p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import PanelGroup from './PanelGroup'
import LineChart from './LineChart'
import PieChart from './PieChart'
import BarChart from './BarChart'
import RaddarChart from './RaddarChart'
import { sendMessageToAI } from '@/api/ai'

export default {
  name: "Dashboard",
  components: {
    PanelGroup,
    LineChart,
    PieChart,
    BarChart,
    RaddarChart
  },
  data() {
    return {
      lineChartData: {
        expectedData: [100, 120, 161, 134, 105, 160, 165],
        actualData: [120, 82, 91, 154, 162, 140, 145]
      },
      statisticsData: {
        users: 0,
        orders: 0,
        books: 0,
        publishedBooks: 0
      }
    };
  },
  created() {
    this.fetchStatisticsData();
  },
  methods: {
    handleSetLineChartData(type) {
      switch (type) {
        case 'newBooks':
          this.lineChartData = {
            expectedData: [100, 120, 161, 134, 105, 160, 165],
            actualData: [120, 82, 91, 154, 162, 140, 145]
          }
          break
        case 'orders':
          this.lineChartData = {
            expectedData: [200, 192, 120, 144, 160, 130, 140],
            actualData: [180, 160, 151, 106, 145, 150, 130]
          }
          break
        case 'purchases':
          this.lineChartData = {
            expectedData: [80, 100, 121, 104, 105, 90, 100],
            actualData: [120, 90, 100, 138, 142, 130, 130]
          }
          break
        case 'visits':
          this.lineChartData = {
            expectedData: [130, 140, 141, 142, 145, 150, 160],
            actualData: [120, 82, 91, 154, 162, 140, 130]
          }
          break
      }
    },
    async fetchStatisticsData() {
      try {
        // 获取用户统计
        const usersResponse = await this.fetchStatistics('users');
        if (usersResponse && usersResponse.data && usersResponse.data.aiResult) {
          const intentResult = usersResponse.data.intentResult;
          if (intentResult && intentResult.result && intentResult.result.success) {
            this.statisticsData.users = intentResult.result.count || 0;
          }
        }
        
        // 获取订单统计
        const ordersResponse = await this.fetchStatistics('orders');
        if (ordersResponse && ordersResponse.data && ordersResponse.data.aiResult) {
          const intentResult = ordersResponse.data.intentResult;
          if (intentResult && intentResult.result && intentResult.result.success) {
            this.statisticsData.orders = intentResult.result.count || 0;
          }
        }
        
        // 获取图书统计
        const booksResponse = await this.fetchStatistics('books');
        if (booksResponse && booksResponse.data && booksResponse.data.aiResult) {
          const intentResult = booksResponse.data.intentResult;
          if (intentResult && intentResult.result && intentResult.result.success) {
            this.statisticsData.books = intentResult.result.count || 0;
          }
        }
        
        // 获取在售图书统计
        const publishedBooksResponse = await this.fetchStatistics('publishedBooks');
        if (publishedBooksResponse && publishedBooksResponse.data && publishedBooksResponse.data.aiResult) {
          const intentResult = publishedBooksResponse.data.intentResult;
          if (intentResult && intentResult.result && intentResult.result.success) {
            this.statisticsData.publishedBooks = intentResult.result.count || 0;
          }
        }
      } catch (error) {
        console.error('获取统计数据失败:', error);
      }
    },
    async fetchStatistics(statType) {
      const message = `统计${statType === 'users' ? '用户' : statType === 'orders' ? '订单' : statType === 'books' ? '图书' : '在售图书'}数量`;
      try {
        return await sendMessageToAI({
          message: message,
          history: []
        });
      } catch (error) {
        console.error(`获取${statType}统计数据失败:`, error);
        return null;
      }
    }
  }
};
</script>

<style lang="scss" scoped>
.dashboard {
  &-container {
    padding: 20px;
  }
  &-info-card {
    margin-bottom: 20px;
    
    h2 {
      margin-top: 10px;
      font-size: 26px;
      font-weight: 100;
    }
    
    p {
      margin-top: 10px;
      text-indent: 2em;
      line-height: 1.8;
    }
  }
}

.chart-card {
  margin-bottom: 20px;
  height: 350px;
}
</style> 
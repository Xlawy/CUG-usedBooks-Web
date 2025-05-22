<template>
  <div class="ai-chat-container" :class="{ 'is-collapsed': isCollapsed }" :style="containerStyle">
    <div class="chat-header" @mousedown="startDrag">
      <span class="chat-title">智能查询助手</span>
      <div class="chat-actions">
        <el-button type="text" icon="el-icon-minus" @click.stop="closeChat"></el-button>
      </div>
    </div>
    
    <div class="chat-body" v-show="!isCollapsed">
      <div class="chat-messages" ref="messageContainer">
        <div v-for="(message, index) in messages" :key="index" 
             :class="['message', message.isUser ? 'user-message' : 'ai-message']">
          <div class="message-avatar">
            <i :class="message.isUser ? 'el-icon-user' : 'el-icon-service'"></i>
          </div>
          <div class="message-content">
            <div v-if="message.isUser || !message.intentData" class="message-text" v-html="formatMessage(message.content)"></div>
            
            <!-- 示例问题列表 -->
            <div v-if="message.showExamples" class="helpful-message">
              <div class="examples-header">
                <i class="el-icon-magic-stick"></i>
                <span>您可以这样问我</span>
              </div>
              <div class="example-list">
                <div class="example-item" @click="useExampleQuery('请帮我查询作者是马克思的书籍')">
                  <div class="item-icon">
                    <i class="el-icon-search"></i>
                  </div>
                  <div class="item-content">
                    <div class="item-text">"请帮我查询作者是马克思的书籍"</div>
                  </div>
                  <div class="item-arrow">
                    <i class="el-icon-arrow-right"></i>
                  </div>
                </div>
                <div class="example-item" @click="useExampleQuery('有没有ISBN为9787111213826的书？')">
                  <div class="item-icon">
                    <i class="el-icon-document"></i>
                  </div>
                  <div class="item-content">
                    <div class="item-text">"有没有ISBN为9787111213826的书？"</div>
                  </div>
                  <div class="item-arrow">
                    <i class="el-icon-arrow-right"></i>
                  </div>
                </div>
                <div class="example-item" @click="useExampleQuery('我想看看有什么畅销书推荐')">
                  <div class="item-icon">
                    <i class="el-icon-star-on"></i>
                  </div>
                  <div class="item-content">
                    <div class="item-text">"我想看看有什么畅销书推荐"</div>
                  </div>
                  <div class="item-arrow">
                    <i class="el-icon-arrow-right"></i>
                  </div>
                </div>
                <div class="example-item" @click="useExampleQuery('系统中有多少在售图书？')">
                  <div class="item-icon">
                    <i class="el-icon-data-line"></i>
                  </div>
                  <div class="item-content">
                    <div class="item-text">"系统中有多少在售图书？"</div>
                  </div>
                  <div class="item-arrow">
                    <i class="el-icon-arrow-right"></i>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 意图识别结果 -->
            <div v-else-if="message.intentData && !message.queryResult" class="intent-data">
              <!-- <div class="intent-type">
                <el-tag size="small" :type="getIntentTagType(message.intentData.intent)">
                  {{ getIntentName(message.intentData.intent) }}
                </el-tag>
              </div> -->
              <div v-if="Object.keys(message.intentData.parameters).length > 0" class="intent-params">
                <div v-for="(value, key) in message.intentData.parameters" :key="key" class="param-item">
                  <span class="param-name">{{ formatParamName(key) }}:</span>
                  <span class="param-value">{{ value }}</span>
                </div>
              </div>
              <div v-if="message.intentData.intent === 'unknown'" class="helpful-message">
                <p>小地暂时不能帮助您解决这个问题，您可以尝试以下问题：</p>
                <ul class="ai-example-list">
                  <li><span class="example-query" @click="useExampleQuery('请帮我查询作者是马克思的书籍')">"请帮我查询作者是马克思的书籍"</span></li>
                  <li><span class="example-query" @click="useExampleQuery('有没有ISBN为9787111213826的书？')">"有没有ISBN为9787111213826的书？"</span></li>
                  <li><span class="example-query" @click="useExampleQuery('我想看看有什么畅销书推荐')">"我想看看有什么畅销书推荐"</span></li>
                  <li><span class="example-query" @click="useExampleQuery('系统中有多少在售图书？')">"系统中有多少在售图书？"</span></li>
                </ul>
              </div>
            </div>
            
            <!-- 查询结果 -->
            <div v-else-if="message.queryResult" class="query-result">
              <!-- 查询成功结果 -->
              <div v-if="message.queryResult.success" class="query-success">
                <div class="query-message">{{ message.queryResult.message }}</div>
                
                <!-- 图书查询结果 -->
                <div v-if="message.intentData.intent === 'book_search'" class="book-list">
                  <el-collapse v-if="message.queryResult.data && message.queryResult.data.length > 0">
                    <el-collapse-item v-for="(book, idx) in message.queryResult.data" :key="idx" :title="book.title">
                      <div class="book-detail">
                        <div class="book-info-row"><span class="info-label">作者:</span> {{ book.author }}</div>
                        <div class="book-info-row"><span class="info-label">出版社:</span> {{ book.publisher }}</div>
                        <div class="book-info-row"><span class="info-label">价格:</span> ¥{{ book.price }}</div>
                        <div class="book-info-row"><span class="info-label">ISBN:</span> {{ book.isbn }}</div>
                        <div class="book-description">{{ book.description }}</div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
                
                <!-- 图书详情结果 -->
                <div v-else-if="message.intentData.intent === 'book_info'" class="book-info">
                  <div v-if="message.queryResult.field" class="single-field">
                    <span class="field-name">{{ formatParamName(message.queryResult.field) }}</span>
                    <span class="field-value">{{ message.queryResult.value }}</span>
                  </div>
                  <div v-else-if="message.queryResult.data" class="book-simple-card" @click="showBookDetail(message.queryResult.data)">
                    <div class="book-cover">
                      <img :src="getBookCoverUrl(message.queryResult.data)" alt="封面" class="cover-image">
                      <div class="book-price-tag">¥{{ message.queryResult.data.price }}</div>
                    </div>
                    <div class="book-brief">
                      <div class="book-title">{{ message.queryResult.data.title }}</div>
                      <div class="book-author">{{ message.queryResult.data.author }}</div>
                    </div>
                    <div class="click-hint">
                      <i class="el-icon-view"></i>
                      <span>点击查看详情</span>
                    </div>
                  </div>
                </div>
                
                <!-- 订单状态结果 -->
                <div v-else-if="message.intentData.intent === 'order_status'" class="order-status">
                  <div v-if="message.queryResult.data" class="order-info">
                    <div v-if="Array.isArray(message.queryResult.data)">
                      <!-- 多个订单 -->
                      <el-collapse>
                        <el-collapse-item v-for="(order, idx) in message.queryResult.data" :key="idx" :title="`订单号: ${order.orderId}`">
                          <div class="order-detail">
                            <div class="order-info-row"><span class="info-label">状态:</span> {{ order.status }}</div>
                            <div class="order-info-row"><span class="info-label">金额:</span> ¥{{ order.totalAmount }}</div>
                            <div class="order-info-row"><span class="info-label">创建时间:</span> {{ order.createTime }}</div>
                          </div>
                        </el-collapse-item>
                      </el-collapse>
                    </div>
                    <div v-else>
                      <!-- 单个订单 -->
                      <div class="order-detail">
                        <div class="order-info-row"><span class="info-label">订单号:</span> {{ message.queryResult.data.orderId }}</div>
                        <div class="order-info-row"><span class="info-label">状态:</span> {{ message.queryResult.data.status }}</div>
                        <div class="order-info-row"><span class="info-label">金额:</span> ¥{{ message.queryResult.data.totalAmount }}</div>
                        <div class="order-info-row"><span class="info-label">创建时间:</span> {{ message.queryResult.data.createTime }}</div>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 图书推荐结果 -->
                <div v-else-if="message.intentData.intent === 'book_recommendation'" class="book-recommendation">
                  <div v-if="message.queryResult.data && message.queryResult.data.length > 0" class="recommended-books">

                    <el-carousel 
                      :interval="4000" 
                      type="card" 
                      height="240px"
                      :autoplay="true"
                      indicator-position="outside"
                      arrow="always">
                      <el-carousel-item v-for="book in message.queryResult.data" :key="book.id">
                        <div class="carousel-book-card" @click="showBookDetail(book)">
                          <div class="carousel-book-cover">
                            <img :src="getBookCoverUrl(book)" alt="封面" class="cover-image">
                          </div>
                          <div class="carousel-book-info">
                            <h3 class="carousel-book-title">{{ book.title }}</h3>
                          </div>
                        </div>
                      </el-carousel-item>
                    </el-carousel>
                  </div>
                </div>
                
                <!-- 统计信息结果 -->
                <div v-else-if="message.intentData.intent === 'statistics'" class="statistics-info">
                  <div v-if="message.queryResult.success" class="statistics-data">
                    <el-card class="statistics-card">
                      <div slot="header" class="statistics-header">
                        <span>{{ getStatisticsTitle(message.queryResult.statType) }}</span>
                        <span v-if="message.queryResult.timeRange" class="time-range">
                          {{ getTimeRangeText(message.queryResult.timeRange) }}
                        </span>
                      </div>
                      <div class="statistics-value">
                        <span class="value">{{ message.queryResult.count }}</span>
                        <span class="unit">{{ getStatisticsUnit(message.queryResult.statType) }}</span>
                      </div>
                    </el-card>
                  </div>
                </div>
                
                <!-- 订单创建结果 -->
                <div v-else-if="message.intentData.intent === 'order_create'" class="order-create">
                  <div v-if="message.queryResult.steps && message.queryResult.steps.length > 0" class="order-steps">
                    <el-steps direction="vertical" :active="message.queryResult.steps.length">
                      <el-step 
                        v-for="(step, index) in message.queryResult.steps" 
                        :key="index" 
                        :title="`步骤 ${index + 1}`" 
                        :description="step">
                      </el-step>
                    </el-steps>
                  </div>
                </div>

                <!-- 在售图书搜索结果 -->
                <div v-else-if="message.intentData.intent === 'published_book_search'" class="published-book-list">
                  <el-collapse v-if="message.queryResult.data && message.queryResult.data.length > 0">
                    <el-collapse-item v-for="(book, idx) in message.queryResult.data" :key="idx" :title="book.bookinfo ? book.bookinfo.title : '未知书名'">
                      <div class="book-detail">
                        <div class="book-info-row"><span class="info-label">卖家:</span> {{ book._openid ? '用户ID: ' + book._openid : '个人卖家' }}</div>
                        <div class="book-info-row"><span class="info-label">作者:</span> {{ book.bookinfo ? book.bookinfo.author : '未知' }}</div>
                        <div class="book-info-row"><span class="info-label">价格:</span> ¥{{ book.price }}</div>
                        <div class="book-info-row"><span class="info-label">成色:</span> {{ book.condition || '二手良好' }}</div>
                        <div class="book-info-row"><span class="info-label">发布时间:</span> {{ formatDate(book.creat) || '未知' }}</div>
                        <div v-if="book.notes" class="book-description">{{ book.notes }}</div>
                        <div v-if="book.bookinfo && book.bookinfo.pic" class="book-image">
                          <img :src="book.bookinfo.pic" alt="封面" class="cover-image">
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </div>
              
              <!-- 查询失败结果 -->
              <div v-else class="query-failure">
                <el-alert
                  title="查询失败"
                  type="error"
                  :description="message.queryResult.message"
                  show-icon
                  :closable="false">
                </el-alert>
              </div>
            </div>
            
            <div class="message-time">{{ message.time }}</div>
          </div>
        </div>
        <div class="typing-indicator" v-if="isLoading">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
      
      <div class="chat-input">
        <el-input
          type="textarea"
          :rows="2"
          placeholder="请输入您的查询..."
          v-model="inputMessage"
          class="custom-textarea"
          @keyup.enter.native.exact="sendMessage"
        ></el-input>
        <el-button type="primary" icon="el-icon-s-promotion" @click="sendMessage" :loading="isLoading">发送</el-button>
      </div>
    </div>

    <!-- 图书详情弹窗 -->
    <el-dialog
      title="图书详情"
      :visible.sync="bookDetailVisible"
      width="600px"
      :before-close="closeBookDetail"
      class="book-detail-dialog"
      append-to-body
      center>
      <div v-if="currentBook" class="book-detail-content">
        <div class="book-info-header">
          <div class="book-cover">
            <img :src="getBookCoverUrl(currentBook)" alt="封面" class="cover-image">
            <div class="book-price-tag">¥{{ currentBook.price }}</div>
          </div>
          <div class="book-basic-info">
            <h3 class="book-title">{{ currentBook.title }}</h3>
            <div class="book-meta">
              <div class="meta-item">
                <i class="el-icon-user"></i>
                <span>{{ currentBook.author }}</span>
              </div>
              <div class="meta-item">
                <i class="el-icon-office-building"></i>
                <span>{{ currentBook.publisher }}</span>
              </div>
              <div class="meta-item">
                <i class="el-icon-document"></i>
                <span>{{ currentBook.isbn }}</span>
              </div>
              <div class="meta-item" v-if="currentBook.pubdate">
                <i class="el-icon-date"></i>
                <span>{{ currentBook.pubdate }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="currentBookRecommendReason" class="book-recommend-reason">
          <div class="reason-title">
            <i class="el-icon-star-on"></i>
            推荐理由
          </div>
          <div class="reason-content">{{ currentBookRecommendReason }}</div>
        </div>
        <div class="book-details">
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="detail-item">
                <div class="detail-label">装帧</div>
                <div class="detail-value">{{ currentBook.binding || '平装' }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="detail-item">
                <div class="detail-label">页数</div>
                <div class="detail-value">{{ currentBook.page || '未知' }} 页</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="detail-item">
                <div class="detail-label">库存</div>
                <div class="detail-value">{{ currentBook.stock || 0 }} 本</div>
              </div>
            </el-col>
          </el-row>
        </div>
        <div v-if="currentBook.summary" class="book-summary">
          <div class="summary-title">
            <i class="el-icon-reading"></i>
            图书简介
          </div>
          <div class="summary-content">{{ currentBook.summary }}</div>
        </div>
        <div class="book-tags" v-if="currentBook.keyword">
          <div class="tags-title">
            <i class="el-icon-collection-tag"></i>
            图书标签
          </div>
          <div class="tags-content">
            <el-tag 
              v-for="(tag, index) in currentBook.keyword.split('－')" 
              :key="index"
              size="small"
              effect="light"
              class="book-tag">
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="closeBookDetail">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { sendMessageToAI, sendMessageToThirdPartyAI } from '@/api/ai'
import aiService from '@/utils/ai-service'

export default {
  name: 'AIChat',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      isCollapsed: false,
      inputMessage: '',
      isLoading: false,
      messages: [
        {
          content: '您好！我是智能查询助手，可以帮您查询图书信息、订单状态等。请输入您的问题。',
          isUser: false,
          time: this.getCurrentTime(),
          intentData: null
        },
        {
          content: '您可以这样问我：',
          isUser: false,
          time: this.getCurrentTime(),
          intentData: {
            intent: 'unknown',
            parameters: {}
          },
          queryResult: null,
          showExamples: true
        }
      ],
      // 拖动相关数据
      isDragging: false,
      position: {
        x: window.innerWidth - 370, // 窗口右侧20px，考虑宽度350px
        y: window.innerHeight - 520 // 窗口底部20px，考虑高度500px
      },
      dragOffset: {
        x: 0,
        y: 0
      },
      // AI服务配置
      aiConfig: {
        ...aiService.DEFAULT_CONFIG,
        useDirectApi: false, // 是否直接调用第三方API而不经过后端
        apiKey: '', // API密钥
      },
      // 错误处理
      hasError: false,
      errorMessage: '',
      // 意图映射
      intentMap: {
        'book_search': '图书搜索',
        'published_book_search': '在售图书搜索',
        'book_info': '图书详情',
        'order_status': '订单状态',
        'book_recommendation': '图书推荐',
        'statistics': '统计信息',
        'order_create': '创建订单',
        'unknown': '未知意图'
      },
      // 参数名称映射
      paramNameMap: {
        'bookName': '书名',
        'author': '作者',
        'isbn': 'ISBN',
        'category': '分类',
        'orderId': '订单号',
        'userId': '用户ID',
        'field': '查询字段',
        'statType': '统计类型',
        'timeRange': '时间范围'
      },
      // 图书详情弹窗
      bookDetailVisible: false,
      currentBook: null,
      currentBookRecommendReason: '', // 添加当前图书的推荐理由
    }
  },
  computed: {
    containerStyle() {
      return {
        transform: `translate(${this.position.x}px, ${this.position.y}px)`
      }
    }
  },
  watch: {
    visible(val) {
      if (!val) {
        this.isCollapsed = false;
      }
    },
    messages() {
      this.$nextTick(() => {
        this.scrollToBottom();
      });
    }
  },
  mounted() {
    document.addEventListener('mousemove', this.onDrag);
    document.addEventListener('mouseup', this.stopDrag);
    
    // 初始化位置为右下角
    this.updateInitialPosition();
    
    // 监听窗口大小变化
    window.addEventListener('resize', this.updateInitialPosition);
    
    // 获取AI配置
    this.getAIConfig();
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDrag);
    document.removeEventListener('mouseup', this.stopDrag);
    window.removeEventListener('resize', this.updateInitialPosition);
  },
  methods: {
    /**
     * 获取AI配置信息
     */
    async getAIConfig() {
      try {
        // 实际项目中可以从后端获取配置
        // const res = await getAIConfig();
        // if (res.data && res.code === 200) {
        //   this.aiConfig = { ...this.aiConfig, ...res.data };
        // }
        
        // 这里仅设置默认配置
        this.aiConfig = {
          ...this.aiConfig,
          apiKey: aiService.getApiKey()
        };
      } catch (error) {
        console.error('获取AI配置失败:', error);
      }
    },
    updateInitialPosition() {
      if (!this.isDragging) {
        // 只在没有拖动时更新初始位置
        const isMobile = window.innerWidth <= 767;
        if (isMobile) {
          this.position.x = window.innerWidth * 0.05;
          this.position.y = window.innerHeight - 470;
        } else {
          this.position.x = window.innerWidth - 370;
          this.position.y = window.innerHeight - 520;
        }
      }
    },
    startDrag(event) {
      if (event.target.closest('.chat-actions')) return;
      
      this.isDragging = true;
      
      // 记录鼠标在元素内的相对位置
      this.dragOffset.x = event.clientX - this.position.x;
      this.dragOffset.y = event.clientY - this.position.y;
      
      event.preventDefault();
    },
    onDrag(event) {
      if (!this.isDragging) return;
      
      // 使用requestAnimationFrame提高性能
      requestAnimationFrame(() => {
        // 直接计算新位置，避免复杂计算
        const newX = event.clientX - this.dragOffset.x;
        const newY = event.clientY - this.dragOffset.y;
        
        // 限制在可视区域内
        const maxX = window.innerWidth - 100;
        const maxY = window.innerHeight - 50;
        
        this.position.x = Math.max(0, Math.min(maxX, newX));
        this.position.y = Math.max(0, Math.min(maxY, newY));
      });
    },
    stopDrag() {
      this.isDragging = false;
    },
    closeChat() {
      this.$emit('update:visible', false);
    },
    getCurrentTime() {
      const now = new Date();
      return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
    },
    scrollToBottom() {
      const container = this.$refs.messageContainer;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    },
    formatMessage(content) {
      return content.replace(/\n/g, '<br>');
    },
    /**
     * 根据意图类型获取标签类型
     */
    getIntentTagType(intent) {
      const typeMap = {
        'book_search': 'primary',
        'book_info': 'success',
        'order_status': 'warning',
        'book_recommendation': 'info',
        'unknown': 'danger'
      };
      return typeMap[intent] || 'info';
    },
    /**
     * 获取意图名称
     */
    getIntentName(intent) {
      return this.intentMap[intent] || intent;
    },
    /**
     * 格式化参数名称
     */
    formatParamName(paramName) {
      return this.paramNameMap[paramName] || paramName;
    },
    /**
     * 获取统计类型标题
     */
    getStatisticsTitle(statType) {
      const titles = {
        'users': '用户总数',
        'orders': '订单总数',
        'books': '图书总数',
        'publishedBooks': '在售图书数'
      };
      return titles[statType] || '统计信息';
    },
    
    /**
     * 获取时间范围文本
     */
    getTimeRangeText(timeRange) {
      const ranges = {
        'today': '(今日)',
        'month': '(本月)',
        'year': '(今年)'
      };
      return ranges[timeRange] || '';
    },
    
    /**
     * 获取统计单位
     */
    getStatisticsUnit(statType) {
      const units = {
        'users': '位',
        'orders': '个',
        'books': '本',
        'publishedBooks': '本'
      };
      return units[statType] || '个';
    },
    
    /**
     * 格式化多行文本
     */
    formatMultiLineText(text) {
      if (!text) return '';
      return text.replace(/\n/g, '<br>');
    },
    
    /**
     * 发送消息到AI服务并获取回复
     */
    async getAIResponse(userMessage) {
      try {
        this.hasError = false;
        this.errorMessage = '';
        
        // 截断过长的消息
        const trimmedMessage = aiService.truncateMessage(userMessage);
        
        // 构建对话历史
        const chatHistory = aiService.buildChatHistory(
          this.messages, 
          this.aiConfig.systemPrompt,
          this.aiConfig.maxHistoryLength
        );
        
        let response;
        try {
          if (this.aiConfig.useDirectApi) {
            // 直接调用第三方API
            response = await sendMessageToThirdPartyAI({
              message: trimmedMessage,
              history: chatHistory,
              model: this.aiConfig.model,
              temperature: this.aiConfig.temperature,
              max_tokens: this.aiConfig.maxTokens
            });
          } else {
            // 通过后端调用
            try {
              response = await sendMessageToAI({
                message: trimmedMessage,
                history: chatHistory
              });
              
              // 在控制台打印详细日志，帮助调试
              console.log('后端返回完整数据:', response);
            } catch (apiError) {
              console.error('AI服务API调用失败，使用本地模拟响应:', apiError);
              
              // 模拟服务器返回数据结构
              const simulatedResponse = aiService.getSimulatedResponse(trimmedMessage);
              response = {
                code: 200,
                msg: '本地模拟响应',
                data: {
                  success: true,
                  model: '本地模拟模型',
                  isSimulated: true,
                  content: simulatedResponse,
                  responseTime: 0
                }
              };
            }
            
            // 现在后端返回的数据可能包含意图处理结果
            let aiResult = null;
            let intentResult = null;
            
            if (response.data) {
              if (response.data.aiResult) {
                // 新的格式，包含AI结果和意图处理结果
                aiResult = response.data.aiResult;
                intentResult = response.data.intentResult;
                console.log('AI识别结果:', aiResult);
                console.log('意图处理结果:', intentResult);
              } else {
                // 兼容旧的格式
                aiResult = response.data;
              }
            }
            
            if (aiResult) {
              // 提取AI识别的意图
              try {
                const intentData = JSON.parse(aiResult.content);
                
                // 返回意图数据和查询结果
                return {
                  intentData: intentData,
                  queryResult: intentResult && intentResult.result ? intentResult.result : null
                };
              } catch (jsonError) {
                console.error('解析意图JSON失败:', jsonError, 'AI返回的内容:', aiResult.content);
                
                // 尝试使用本地模拟响应
                try {
                  const simulatedResponse = aiService.getSimulatedResponse(trimmedMessage);
                  const simulatedIntentData = JSON.parse(simulatedResponse);
                  
                  console.warn('使用本地模拟的意图数据:', simulatedIntentData);
                  return {
                    intentData: simulatedIntentData,
                    queryResult: null,
                    responseText: aiResult.content,
                    isSimulated: true
                  };
                } catch (simulatedError) {
                  console.error('本地模拟响应也失败:', simulatedError);
                  return {
                    intentData: null,
                    queryResult: null,
                    responseText: aiResult.content || "抱歉，无法理解您的请求。"
                  };
                }
              }
            }
          }
        } catch (error) {
          // 如果API调用失败，使用本地模拟响应
          console.warn('AI API调用失败，使用本地模拟响应:', error);
          
          // 模拟响应已经是JSON字符串，尝试解析
          const simulatedResponse = aiService.getSimulatedResponse(trimmedMessage);
          try {
            const intentData = JSON.parse(simulatedResponse);
            return {
              intentData: intentData,
              queryResult: null,
              isSimulated: true
            };
          } catch (jsonError) {
            console.error('解析模拟响应JSON失败:', jsonError);
            return {
              responseText: "抱歉，无法处理您的请求。",
              intentData: null,
              queryResult: null
            };
          }
        }
      } catch (error) {
        console.error('AI服务错误:', error);
        this.hasError = true;
        this.errorMessage = error.message || '无法连接到AI服务，请稍后再试';
        return {
          responseText: '抱歉，AI服务暂时无法回应，请稍后再试。',
          intentData: null,
          queryResult: null
        };
      }
    },
    /**
     * 发送消息并获取回复
     */
    async sendMessage(e) {
      if (e && e.type === 'keyup' && e.shiftKey) {
        return;
      }
      
      if (!this.inputMessage.trim()) return;
      
      // 添加用户消息
      const userMessage = this.inputMessage.trim();
      this.messages.push({
        content: userMessage,
        isUser: true,
        time: this.getCurrentTime(),
        intentData: null,
        queryResult: null
      });
      
      this.inputMessage = '';
      this.isLoading = true;
      
      try {
        // 获取AI回复
        const aiResponse = await this.getAIResponse(userMessage);
        
        // 如果响应为null，表示超出范围，不显示回复
        if (aiResponse === null) {
          this.isLoading = false;
          return;
        }
        
        // 添加AI回复到消息列表
      this.messages.push({
          content: aiResponse.responseText || "",
        isUser: false,
          time: this.getCurrentTime(),
          intentData: aiResponse.intentData,
          queryResult: aiResponse.queryResult
        });
      } catch (error) {
        // 错误处理
        console.error('处理消息失败:', error);
        this.messages.push({
          content: '抱歉，发生了错误，无法获取回复。',
          isUser: false,
          time: this.getCurrentTime(),
          intentData: null,
          queryResult: null
        });
      } finally {
        this.isLoading = false;
      }
    },
    showBookDetail(book) {
      this.currentBook = book;
      // 从推荐理由中提取当前图书的推荐理由
      const recommendReason = this.getCurrentMessageRecommendReason();
      if (recommendReason) {
        const lines = recommendReason.split('\n');
        let bookReason = '';
        let isCurrentBook = false;
        
        for (const line of lines) {
          if (line.includes(`《${book.title}》`)) {
            isCurrentBook = true;
            continue;
          }
          if (isCurrentBook && line.trim() && !line.startsWith('《')) {
            bookReason = line.trim();
            break;
          }
        }
        this.currentBookRecommendReason = bookReason;
      }
      this.bookDetailVisible = true;
    },
    closeBookDetail() {
      this.bookDetailVisible = false;
      this.currentBook = null;
      this.currentBookRecommendReason = '';
    },
    getCurrentMessageRecommendReason() {
      // 获取当前消息的推荐理由
      const currentMessage = this.messages[this.messages.length - 1];
      if (currentMessage && 
          currentMessage.intentData && 
          currentMessage.intentData.intent === 'book_recommendation' && 
          currentMessage.queryResult && 
          currentMessage.queryResult.recommendReason) {
        return currentMessage.queryResult.recommendReason;
      }
      return '';
    },
    getBookCoverUrl(book) {
      if (!book) return require('@/assets/images/default-cover.png');
      
      // 依次检查各种可能的封面URL字段
      return book.pic
             // 如果都没有找到，返回默认封面
             require('@/assets/images/default-cover.png');
    },
    formatDate(timestamp) {
      if (!timestamp) return '未知';
      const date = new Date(timestamp);
      return isNaN(date.getTime()) ? '未知' : 
        `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
    },
    useExampleQuery(query) {
      this.inputMessage = query;
    },
    isJsonString(str) {
      try {
        JSON.parse(str);
        return true;
      } catch (e) {
        return false;
      }
    },
    parseRecommendReason(reason) {
      try {
        return JSON.parse(reason);
      } catch (e) {
        console.error('解析推荐理由失败:', e);
        return [];
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-chat-container {
  position: fixed;
  left: 0;
  top: 0;
  width: 350px;
  border-radius: 12px;
  box-shadow: 0 5px 25px rgba(0, 0, 0, 0.15);
  background-color: #fff;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  transition: transform 0.05s ease;
  max-height: 500px;
  will-change: transform;
  
  &.is-collapsed {
    height: 50px;
    max-height: 50px;
    width: 250px;
    overflow: hidden;
    box-shadow: 0 3px 15px rgba(0, 0, 0, 0.1);
    
    .chat-body {
      display: none;
    }
  }
  
  @media (max-width: 767px) {
    width: 90%;
    max-height: 400px;
  }
}

.chat-header {
  padding: 12px 15px;
  background-color: #409EFF;
  color: white;
  border-radius: 12px 12px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: move;
  user-select: none;
}

.chat-title {
  font-weight: bold;
  font-size: 16px;
}

.chat-actions {
  display: flex;
  
  .el-button {
    color: white;
    padding: 3px;
    margin-left: 5px;
    transition: color 0.2s;
    cursor: pointer;
    
    &:hover {
      color: #f0f0f0;
    }
  }
}

.chat-body {
  display: flex;
  flex-direction: column;
  height: 500px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  background-color: #f9f9f9;
  
  &::-webkit-scrollbar {
    width: 5px;
  }
  
  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 5px;
  }
  
  &::-webkit-scrollbar-track {
    background-color: rgba(0, 0, 0, 0.05);
  }
}

.message {
  display: flex;
  margin-bottom: 18px;
  
  &.user-message {
    flex-direction: row-reverse;
    
    .message-content {
      background-color: #ecf5ff;
      margin-left: 0;
      margin-right: 10px;
      border-radius: 15px 0 15px 15px;
      border: 1px solid #d9ecff;
    }
    
    .message-time {
      text-align: left;
    }
  }
  
  &.ai-message {
    .message-content {
      background-color: white;
      border: 1px solid #eee;
      border-radius: 0 15px 15px 15px;
    }
  }
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  
  i {
    font-size: 18px;
  }
  
  .user-message & {
    background-color: #409EFF;
    color: white;
  }
  
  .ai-message & {
    background-color: #67C23A;
    color: white;
  }
}

.message-content {
  max-width: 70%;
  padding: 12px;
  background-color: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  margin-left: 10px;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.5;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 5px;
  text-align: right;
}

.chat-input {
  padding: 12px;
  border-top: 1px solid #eaeaea;
  display: flex;
  align-items: flex-end;
  background-color: white;
  border-radius: 0 0 12px 12px;
  
  .el-textarea, .custom-textarea {
    margin-right: 10px;
    flex: 1;
    
    ::v-deep textarea {
      border-radius: 8px;
      resize: none;
      padding: 10px;
      font-size: 14px;
      transition: all 0.3s;
      border: 1px solid #dcdfe6;
      box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
      
      &:focus {
        border-color: #409EFF;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
      }
      
      &::placeholder {
        color: #909399;
      }
    }
  }
  
  .el-button {
    padding: 10px 16px;
    border-radius: 8px;
    font-size: 14px;
    height: 40px;
    margin-left: 8px;
    align-self: flex-end;
    transition: all 0.2s;
    
    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
    }
    
    &:active {
      transform: translateY(0);
    }
  }
}

.typing-indicator {
  display: flex;
  padding: 10px;
  margin-bottom: 15px;
  
  span {
    height: 8px;
    width: 8px;
    border-radius: 50%;
    background-color: #b6b6b6;
    margin: 0 2px;
    display: inline-block;
    animation: typing 1.4s infinite both;
    
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
  100% { transform: translateY(0); }
}

.intent-data {
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 8px;
  border-left: 3px solid #409EFF;
}

.intent-type {
  margin-bottom: 8px;
}

.intent-params {
  margin-top: 5px;
}

.param-item {
  margin: 5px 0;
  font-size: 13px;
}

.param-name {
  font-weight: bold;
  color: #606266;
  margin-right: 4px;
}

.param-value {
  color: #333;
}

.no-params {
  font-style: italic;
  color: #999;
  font-size: 13px;
  margin-top: 4px;
}

.unknown-message {
  margin-top: 8px;
  font-size: 13px;
  
  p {
    margin: 5px 0;
    
    &:first-child {
      color: #f56c6c;
    }
  }
  
  .ai-capabilities {
    font-weight: bold;
    margin-top: 10px;
    color: #409EFF;
  }
  
  .ai-example-list {
    margin: 5px 0 0 15px;
    padding: 0;
    
    li {
      margin-bottom: 5px;
      list-style-type: circle;
      color: #606266;
    }
  }
  
  .example-query {
    color: #409EFF;
    font-style: italic;
    cursor: pointer;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.chat-help {
  padding: 10px 15px;
  background-color: #fafafa;
  border-top: 1px dashed #eee;
  font-size: 12px;
}

.help-title {
  font-weight: bold;
  color: #606266;
  margin-bottom: 5px;
}

.help-examples {
  margin: 0;
  padding-left: 20px;
  color: #909399;
  
  li {
    margin-bottom: 3px;
  }
}

// 查询结果样式
.query-result {
  margin-top: 10px;
  background-color: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
}

.query-message {
  padding: 8px 10px;
  font-weight: bold;
  background-color: #f0f2f5;
  color: #606266;
}

.book-list, .book-info, .order-status, .book-recommendation {
  padding: 0 10px 10px 10px;
}

.book-detail, .order-detail {
  padding: 10px;
  background-color: white;
  border-radius: 6px;
  margin-top: 5px;
}

.book-info-row, .order-info-row {
  margin: 5px 0;
  font-size: 13px;
}

.info-label {
  font-weight: bold;
  color: #606266;
  margin-right: 5px;
}

.book-description {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.single-field {
  padding: 12px;
  display: flex;
  align-items: center;
  background-color: white;
  border-radius: 6px;
  margin-top: 5px;
}

.field-name {
  font-weight: bold;
  color: #409EFF;
  margin-right: 8px;
}

.field-value {
  font-size: 16px;
}

.query-failure {
  padding: 10px;
}

.recommended-books {
  padding: 15px;
  
  .recommendation-header {
    display: flex;
    align-items: center;
    margin-bottom: 15px;
    
    i {
      color: #f56c6c;
      margin-right: 8px;
      font-size: 18px;
    }
    
    span {
      font-size: 16px;
      font-weight: 500;
      color: #303133;
    }
  }
}

.carousel-book-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 10px;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-5px);
  }
  
  .carousel-book-cover {
    width: 120px;
    height: 160px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    margin-bottom: 12px;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .carousel-book-info {
    text-align: center;
    
    .carousel-book-title {
      font-size: 14px;
      color: #303133;
      margin: 0;
      padding: 0 10px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 150px;
    }
  }
}

.carousel-book-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 10px;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-5px);
  }
  
  .carousel-book-cover {
    width: 120px;
    height: 160px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    margin-bottom: 12px;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .carousel-book-info {
    text-align: center;
    
    .carousel-book-title {
      font-size: 14px;
      color: #303133;
      margin: 0;
      padding: 0 10px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 150px;
    }
  }
}

.book-detail-dialog {
  .book-detail-content {
    padding: 0;
  }

  .book-info-header {
    display: flex;
    margin-bottom: 24px;
  }

  .book-cover {
    width: 160px;
    height: 240px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    margin-right: 24px;
    flex-shrink: 0;
    position: relative;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .book-price-tag {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      background: rgba(245, 108, 108, 0.9);
      color: white;
      padding: 6px 10px;
      font-size: 16px;
      font-weight: bold;
      text-align: center;
    }
  }

  .book-basic-info {
    flex: 1;

    .book-title {
      margin: 0 0 20px 0;
      font-size: 22px;
      color: #303133;
      line-height: 1.4;
      font-weight: 600;
    }

    .book-meta {
      .meta-item {
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        color: #606266;
        font-size: 14px;

        i {
          margin-right: 8px;
          font-size: 16px;
          color: #909399;
          width: 20px;
          text-align: center;
        }
      }
    }
  }

  .book-details {
    background: #f8f9fa;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 24px;

    .detail-item {
      text-align: center;
      padding: 12px;
      background: white;
      border-radius: 6px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

      .detail-label {
        color: #909399;
        font-size: 13px;
        margin-bottom: 6px;
      }

      .detail-value {
        color: #303133;
        font-size: 15px;
        font-weight: 500;
      }
    }
  }

  .book-summary {
    margin-top: 24px;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 8px;

    .summary-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
      display: flex;
      align-items: center;

      i {
        margin-right: 8px;
        color: #409EFF;
      }
    }

    .summary-content {
      font-size: 14px;
      line-height: 1.8;
      color: #606266;
      text-indent: 2em;
      margin: 0;
    }
  }

  .book-tags {
    margin-top: 24px;

    .tags-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
      display: flex;
      align-items: center;

      i {
        margin-right: 8px;
        color: #409EFF;
      }
    }

    .tags-content {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .book-tag {
      margin: 0;
      background-color: #f0f2f5;
      border-color: transparent;
      color: #606266;

      &:hover {
        color: #409EFF;
        background-color: #ecf5ff;
      }
    }
  }
}

.helpful-message {
  margin: 15px 0;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
  
  .examples-header {
    display: flex;
    align-items: center;
    margin-bottom: 15px;
    color: #409EFF;
    font-size: 16px;
    font-weight: 500;
    
    i {
      margin-right: 8px;
      font-size: 20px;
    }
  }
  
  .example-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  
  .example-item {
    background: white;
    border-radius: 8px;
    padding: 12px;
    display: flex;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 1px solid #ebeef5;
    
    &:hover {
      transform: translateX(4px);
      background: #ecf5ff;
      border-color: #409EFF;
      
      .item-icon i {
        color: #409EFF;
        transform: scale(1.1);
      }
      
      .item-arrow i {
        transform: translateX(4px);
        color: #409EFF;
        opacity: 1;
      }
    }
    
    .item-icon {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: #f0f2f5;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 12px;
      flex-shrink: 0;
      
      i {
        font-size: 18px;
        color: #909399;
        transition: all 0.3s ease;
      }
    }
    
    .item-content {
      flex: 1;
      overflow: hidden;
      
      .item-text {
        font-size: 14px;
        color: #606266;
        line-height: 1.4;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
    
    .item-arrow {
      margin-left: 12px;
      flex-shrink: 0;
      
      i {
        font-size: 16px;
        color: #c0c4cc;
        opacity: 0.5;
        transition: all 0.3s ease;
      }
    }
  }
}

.book-recommend-reason {
  margin: 20px 0;
  padding: 15px;
  background: #fdf6ec;
  border-radius: 8px;
  border-left: 4px solid #e6a23c;

  .reason-title {
    display: flex;
    align-items: center;
    font-size: 16px;
    font-weight: 600;
    color: #b88230;
    margin-bottom: 10px;

    i {
      margin-right: 8px;
      font-size: 18px;
    }
  }

  .reason-content {
    color: #666;
    line-height: 1.6;
    font-size: 14px;
    text-indent: 2em;
  }
}
</style> 
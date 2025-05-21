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
            <div v-else class="intent-data">
              <div class="intent-type">
                <el-tag size="small" :type="getIntentTagType(message.intentData.intent)">
                  {{ getIntentName(message.intentData.intent) }}
                </el-tag>
              </div>
              <div v-if="Object.keys(message.intentData.parameters).length > 0" class="intent-params">
                <div v-for="(value, key) in message.intentData.parameters" :key="key" class="param-item">
                  <span class="param-name">{{ formatParamName(key) }}:</span>
                  <span class="param-value">{{ value }}</span>
                </div>
              </div>
              <div v-else class="no-params">
                未识别到有效参数
              </div>
              <div v-if="message.intentData.intent === 'unknown'" class="unknown-message">
                未能识别查询意图，请尝试更明确的问题
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
      
      <div class="chat-help">
        <p class="help-title">你可以询问这些问题：</p>
        <ul class="help-examples">
          <li>《Java编程思想》这本书有库存吗？</li>
          <li>查询订单号12345的状态</li>
          <li>推荐几本计算机类的书籍</li>
          <li>《时间简史》的价格是多少？</li>
        </ul>
      </div>
    </div>
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
        'book_info': '图书详情',
        'order_status': '订单状态',
        'book_recommendation': '图书推荐',
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
        'field': '查询字段'
      }
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
            response = await sendMessageToAI({
              message: trimmedMessage,
              history: chatHistory
            });
            
            // 适配若依框架返回数据格式
            // 在控制台打印详细日志，帮助调试
            console.log('后端返回完整数据:', response);
            
            // 检查response数据结构
            if (response && response.data) {
              console.log('提取的data数据:', response.data);
              // 若依框架返回的数据是在data字段里的
              response = response.data;
            }
          }
          
          // 处理响应 - 现在返回的是JSON字符串
          if (response && response.content) {
            console.log('从响应中提取的内容:', response.content);
            
            try {
              // 尝试解析JSON响应
              const intentData = JSON.parse(response.content);
              console.log('解析后的意图数据:', intentData);
              
              // 如果解析成功且格式正确，直接返回意图数据
              if (intentData && intentData.intent) {
                // 如果意图为unknown且不是关于图书或订单的查询，则不返回响应
                if (intentData.intent === 'unknown') {
                  if (!userMessage.includes('图书') && !userMessage.includes('书') && 
                      !userMessage.includes('订单') && !userMessage.includes('推荐')) {
                    return null; // 不返回任何响应
                  }
                }
                return {
                  responseText: "", // 不再需要文本响应
                  intentData: intentData
                };
              }
            } catch (jsonError) {
              console.error('解析意图JSON失败:', jsonError);
              // 如果解析失败，可能不是有效的JSON，忽略并返回原始内容
            }
            
            // 如果不是有效的JSON或解析失败，仍然返回纯文本响应（兼容旧版）
            return {
              responseText: aiService.formatAIResponse(response.content),
              intentData: null
            };
          } else {
            console.error('无法从响应中提取内容，原始响应:', response);
            throw new Error(response?.message || '获取AI回复失败，无法解析响应数据');
          }
        } catch (error) {
          // 如果API调用失败，使用本地模拟响应
          console.warn('AI API调用失败，使用本地模拟响应:', error);
          
          // 模拟响应已经是JSON字符串，尝试解析
          const simulatedResponse = aiService.getSimulatedResponse(trimmedMessage);
          try {
            const intentData = JSON.parse(simulatedResponse);
            return {
              responseText: "",
              intentData: intentData
            };
          } catch (jsonError) {
            console.error('解析模拟响应JSON失败:', jsonError);
            return {
              responseText: "抱歉，无法处理您的请求。",
              intentData: null
            };
          }
        }
      } catch (error) {
        console.error('AI服务错误:', error);
        this.hasError = true;
        this.errorMessage = error.message || '无法连接到AI服务，请稍后再试';
        return {
          responseText: '抱歉，AI服务暂时无法回应，请稍后再试。',
          intentData: null
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
        intentData: null
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
          intentData: aiResponse.intentData
        });
      } catch (error) {
        // 错误处理
        console.error('处理消息失败:', error);
        this.messages.push({
          content: '抱歉，发生了错误，无法获取回复。',
          isUser: false,
          time: this.getCurrentTime(),
          intentData: null
        });
      } finally {
        this.isLoading = false;
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
  color: #f56c6c;
  margin-top: 8px;
  font-size: 13px;
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
</style> 
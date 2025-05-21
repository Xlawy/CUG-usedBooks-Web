<template>
  <div class="ai-chat-container" :class="{ 'is-collapsed': isCollapsed }" :style="containerStyle">
    <div class="chat-header" @mousedown="startDrag">
      <span class="chat-title">AI助手</span>
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
            <div class="message-text" v-html="formatMessage(message.content)"></div>
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
          placeholder="请输入问题..."
          v-model="inputMessage"
          class="custom-textarea"
          @keyup.enter.native.exact="sendMessage"
        ></el-input>
        <el-button type="primary" icon="el-icon-s-promotion" @click="sendMessage" :loading="isLoading">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script>
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
          content: '您好！我是AI助手，有什么可以帮助您的？',
          isUser: false,
          time: this.getCurrentTime()
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
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDrag);
    document.removeEventListener('mouseup', this.stopDrag);
    window.removeEventListener('resize', this.updateInitialPosition);
  },
  methods: {
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
    async sendMessage(e) {
      if (e && e.type === 'keyup' && e.shiftKey) {
        return;
      }
      
      if (!this.inputMessage.trim()) return;
      
      this.messages.push({
        content: this.inputMessage,
        isUser: true,
        time: this.getCurrentTime()
      });
      
      const userMessage = this.inputMessage;
      this.inputMessage = '';
      this.isLoading = true;
      
      setTimeout(() => {
        this.simulateAIResponse(userMessage);
        this.isLoading = false;
      }, 1000);
    },
    simulateAIResponse(message) {
      let response = '';
      
      if (message.includes('图书') || message.includes('书籍')) {
        response = '您好，校园二手书交易平台支持多种图书分类和交易方式。您可以在发布管理中查看和管理图书信息。';
      } else if (message.includes('订单') || message.includes('交易')) {
        response = '校园二手书交易订单分为多种状态：在售、买家已付款、交易完成等。您可以在订单管理中查看详情。';
      } else if (message.includes('用户') || message.includes('账户')) {
        response = '用户管理功能位于系统管理菜单下，您可以在那里管理用户权限和信息。';
      } else if (message.includes('帮助') || message.includes('使用说明')) {
        response = '校园二手书交易平台管理系统使用指南：\n1. 基础信息管理：维护图书基本信息\n2. 发布管理：管理用户发布的二手书\n3. 订单管理：跟踪交易订单状态\n4. 系统管理：用户、角色和权限设置';
      } else {
        response = '我是校园二手书交易平台的AI助手，可以回答关于图书管理、订单处理、用户管理等方面的问题。请问有什么可以帮您的？';
      }
      
      this.messages.push({
        content: response,
        isUser: false,
        time: this.getCurrentTime()
      });
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
  height: 450px;
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
</style> 
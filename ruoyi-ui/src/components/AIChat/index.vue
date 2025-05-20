<template>
  <div class="ai-chat-container" :class="{ 'is-collapsed': isCollapsed }">
    <div class="chat-header" @click="toggleCollapse">
      <span class="chat-title">AI助手</span>
      <div class="chat-actions">
        <el-button type="text" icon="el-icon-minus" @click.stop="toggleCollapse"></el-button>
        <el-button type="text" icon="el-icon-close" @click.stop="closeChat"></el-button>
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
          @keyup.enter.native="sendMessage"
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
      ]
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
  methods: {
    toggleCollapse() {
      this.isCollapsed = !this.isCollapsed;
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
      // 简单的文本格式化，可以根据需要扩展为Markdown渲染等
      return content.replace(/\n/g, '<br>');
    },
    async sendMessage(e) {
      // 如果是按Enter键发送，并且同时按了Shift键，则不发送
      if (e && e.type === 'keyup' && e.shiftKey) {
        return;
      }
      
      if (!this.inputMessage.trim()) return;
      
      // 添加用户消息
      this.messages.push({
        content: this.inputMessage,
        isUser: true,
        time: this.getCurrentTime()
      });
      
      const userMessage = this.inputMessage;
      this.inputMessage = '';
      this.isLoading = true;
      
      // 模拟AI响应，实际项目中应调用后端API
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
  bottom: 20px;
  right: 20px;
  width: 350px;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  background-color: #fff;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  max-height: 500px;
  
  &.is-collapsed {
    max-height: 45px;
  }
}

.chat-header {
  padding: 10px;
  background-color: #409EFF;
  color: white;
  border-radius: 8px 8px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.chat-title {
  font-weight: bold;
}

.chat-actions {
  display: flex;
  
  .el-button {
    color: white;
    padding: 3px;
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
  background-color: #f8f8f8;
}

.message {
  display: flex;
  margin-bottom: 15px;
  
  &.user-message {
    flex-direction: row-reverse;
    
    .message-content {
      background-color: #e1f3ff;
      margin-left: 0;
      margin-right: 10px;
      border-radius: 15px 0 15px 15px;
    }
    
    .message-time {
      text-align: left;
    }
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  
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
  padding: 10px;
  border-radius: 0 15px 15px 15px;
  background-color: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  margin-left: 10px;
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  text-align: right;
}

.chat-input {
  padding: 10px;
  border-top: 1px solid #eee;
  display: flex;
  align-items: flex-end;
  
  .el-textarea {
    margin-right: 10px;
  }
}

.typing-indicator {
  display: flex;
  padding: 10px;
  
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
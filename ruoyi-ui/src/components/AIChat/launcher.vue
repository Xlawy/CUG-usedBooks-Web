<template>
  <div>
    <div class="ai-chat-launcher" @click="toggleChat" v-if="!chatVisible">
      <i class="el-icon-message"></i>
    </div>
    <ai-chat :visible.sync="chatVisible"></ai-chat>
  </div>
</template>

<script>
import AIChat from './index'

export default {
  name: 'AIChatLauncher',
  components: {
    AIChat
  },
  data() {
    return {
      chatVisible: false
    }
  },
  methods: {
    toggleChat() {
      this.chatVisible = true
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-chat-launcher {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 2000;
  transition: all 0.3s;
  
  &:hover {
    transform: scale(1.1);
    box-shadow: 0 4px 20px 0 rgba(0, 0, 0, 0.15);
  }
  
  i {
    font-size: 24px;
  }
}
</style> 
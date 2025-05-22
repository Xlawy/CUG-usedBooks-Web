<p align="center">
	<img alt="logo" src="https://oscimg.oschina.net/oscnet/up-d3d0a9303e11d522a06cd263f3079027715.png">
</p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">CUG二手书管理系统</h1>
<h4 align="center">基于RuoYi v3.8.9框架的校园二手书交易平台</h4>
<p align="center">
	<a href="https://gitee.com/y_project/RuoYi-Vue/stargazers"><img src="https://gitee.com/y_project/RuoYi-Vue/badge/star.svg?theme=dark"></a>
	<a href="https://gitee.com/y_project/RuoYi-Vue"><img src="https://img.shields.io/badge/RuoYi-v3.8.9-brightgreen.svg"></a>
	<a href="https://gitee.com/y_project/RuoYi-Vue/blob/master/LICENSE"><img src="https://img.shields.io/github/license/mashape/apistatus.svg"></a>
</p>

## 项目简介

本项目是基于RuoYi框架开发的校园二手书交易平台，采用微信小程序 + 后台管理系统的架构设计。项目致力于为校园师生提供一个便捷、安全的二手书交易环境。

* 前端采用Vue、Element UI，提供现代化的用户界面
* 后端采用Spring Boot、Spring Security、Redis & Jwt
* 微信端集成云开发能力，实现小程序端数据管理
* 权限认证使用Jwt，支持多终端认证
* 支持动态权限菜单，灵活的权限控制
* 集成代码生成器，提高开发效率

## 项目架构

```
CUG-usedBooks-Web
├── ruoyi-ui              // 前端项目（基于Vue）
├── ruoyi-admin           // 后台服务入口
├── ruoyi-framework       // 框架核心
├── ruoyi-system         // 系统功能
├── ruoyi-common         // 通用工具
├── ruoyi-generator      // 代码生成
├── ruoyi-quartz         // 定时任务
└── sql                  // 数据库文件
```

## 技术选型

### 后端技术栈

- Spring Boot：简化Spring应用的初始搭建以及开发过程
- Spring Security：认证和授权框架
- MyBatis：ORM框架  
- JWT：JWT登录支持  
- Redis：缓存框架
- Quartz：作业调度框架  
- Maven：依赖管理工具
- 微信云开发：提供云函数、云数据库等能力

### 前端技术栈

- Vue：前端框架
- Element UI：前端UI框架
- Axios：前端HTTP框架
- Vue Router：路由框架
- Vuex：状态管理框架

## 核心功能

### 基础功能
1. 用户管理：系统用户的维护与管理
2. 角色管理：角色菜单权限分配
3. 菜单管理：系统菜单配置
4. 部门管理：机构部门维护
5. 字典管理：系统字典维护
6. 参数管理：系统参数配置

### 二手书交易功能
1. 图书管理
   - 图书信息维护
   - 图书分类管理
   - 图书状态管理
   - ISBN码管理

2. 交易管理
   - 订单管理
   - 交易流程管理
   - 支付管理
   - 退款管理

3. 用户服务
   - 用户反馈处理
   - 消息通知
   - 用户评价管理

### 系统监控
1. 操作日志：系统操作记录
2. 登录日志：用户登录记录
3. 系统监控：服务器及数据库监控
4. 缓存监控：Redis缓存监控

## 小程序端功能

1. 用户功能
   - 微信授权登录
   - 个人信息管理
   - 收藏夹
   - 购物车

2. 图书功能
   - 图书搜索
   - 图书详情
   - 图书分类浏览
   - 新书推荐

3. 交易功能
   - 发布二手书
   - 购买二手书
   - 订单管理
   - 支付功能

## 环境部署

### 开发环境

- JDK >= 1.8
- MySQL >= 5.7
- Redis >= 3.0
- Node >= 12
- npm >= 6.0

### 安装步骤

1. 数据库配置
```bash
# 创建数据库并执行SQL脚本
create database ry-vue;
sql/ry_20250417.sql
sql/quartz.sql
sql/wx_cloud_config.sql
```

2. 后端配置
```bash
# 修改数据库连接
application-druid.yml
# 修改Redis配置
application.yml
# 修改微信小程序配置
wx_cloud_config.sql
```

3. 前端安装
```bash
# 进入前端项目目录
cd ruoyi-ui
# 安装依赖
npm install
# 启动服务
npm run dev
```

## 演示图

[在这里添加系统的截图]

## 交流与反馈

- 文档地址：[待补充]
- 官方社区：[待补充]
- 交流群：[待补充]

## 特别鸣谢

- 感谢[RuoYi](https://gitee.com/y_project/RuoYi-Vue)提供的优秀开源项目
- 感谢所有为这个项目做出贡献的开发者

## 版权信息

Copyright © 2024 CUG-usedBooks-Web All Rights Reserved


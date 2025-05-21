<p align="center">
	<img alt="logo" src="https://oscimg.oschina.net/oscnet/up-d3d0a9303e11d522a06cd263f3079027715.png">
</p>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">RuoYi v3.8.9</h1>
<h4 align="center">基于SpringBoot+Vue前后端分离的Java快速开发框架</h4>
<p align="center">
	<a href="https://gitee.com/y_project/RuoYi-Vue/stargazers"><img src="https://gitee.com/y_project/RuoYi-Vue/badge/star.svg?theme=dark"></a>
	<a href="https://gitee.com/y_project/RuoYi-Vue"><img src="https://img.shields.io/badge/RuoYi-v3.8.9-brightgreen.svg"></a>
	<a href="https://gitee.com/y_project/RuoYi-Vue/blob/master/LICENSE"><img src="https://img.shields.io/github/license/mashape/apistatus.svg"></a>
</p>

## 平台简介


* 前端采用Vue、Element UI。
* 后端采用Spring Boot、Spring Security、Redis & Jwt。
* 权限认证使用Jwt，支持多终端认证系统。
* 支持加载动态权限菜单，多方式轻松权限控制。
* 高效率开发，使用代码生成器可以一键生成前后端代码。
* 提供了技术栈（[Vue3](https://v3.cn.vuejs.org) [Element Plus](https://element-plus.org/zh-CN) [Vite](https://cn.vitejs.dev)）版本[RuoYi-Vue3](https://gitcode.com/yangzongzhuan/RuoYi-Vue3)，保持同步更新。
* 提供了单应用版本[RuoYi-Vue-fast](https://gitcode.com/yangzongzhuan/RuoYi-Vue-fast)，Oracle版本[RuoYi-Vue-Oracle](https://gitcode.com/yangzongzhuan/RuoYi-Vue-Oracle)，保持同步更新。
* 不分离版本，请移步[RuoYi](https://gitee.com/y_project/RuoYi)，微服务版本，请移步[RuoYi-Cloud](https://gitee.com/y_project/RuoYi-Cloud)
* 阿里云折扣场：[点我进入](http://aly.ruoyi.vip)，腾讯云秒杀场：[点我进入](http://txy.ruoyi.vip)&nbsp;&nbsp;

## 内置功能

1.  用户管理：用户是系统操作者，该功能主要完成系统用户配置。
2.  部门管理：配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。
3.  岗位管理：配置系统用户所属担任职务。
4.  菜单管理：配置系统菜单，操作权限，按钮权限标识等。
5.  角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
6.  字典管理：对系统中经常使用的一些较为固定的数据进行维护。
7.  参数管理：对系统动态配置常用参数。
8.  通知公告：系统通知公告信息发布维护。
9.  操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
10.  登录日志：系统登录日志记录查询包含登录异常。
11.  在线用户：当前系统中活跃用户状态监控。
12.  定时任务：在线（添加、修改、删除)任务调度包含执行结果日志。
13.  代码生成：前后端代码的生成（java、html、xml、sql）支持CRUD下载 。
14.  系统接口：根据业务代码自动生成相关的api接口文档。
15.  服务监控：监视当前系统CPU、内存、磁盘、堆栈等相关信息。
16.  缓存监控：对系统的缓存信息查询，命令统计等。
17.  在线构建器：拖动表单元素生成相应的HTML代码。
18.  连接池监视：监视当前系统数据库连接池状态，可进行分析SQL找出系统性能瓶颈。

根据代码中识别的意图类型，以下是可用于测试的语句：

### 图书搜索 (book_search)
- [ ] "我想找一本有关Java编程的书"
- [x] "请帮我查询作者是马克思的书籍"
- [x] "有没有ISBN为9787111213826的书？"
- [ ] "查一下计算机类的教材"
- [x] "我需要一本关于数据结构的书，有吗？"

### 已发布图书搜索 (published_book_search)
- [ ] "现在有哪些在售的二手书？"
- [ ] "查询所有可以购买的计算机专业书籍"
- [ ] "有人在卖《高等数学》吗？"
- [x] "显示所有二手书列表"
- [ ] "有什么最近新上架的书？"

搜索在售的具体书籍找不到

### 图书详情 (book_info)
- [ ] "《Java编程思想》这本书多少钱？"
- [x] "告诉我《算法导论》的作者是谁"
- [ ] "《离散数学》有库存吗？"
- [x] "我想了解《高等数学》的出版社信息"
- [x] "《时间简史》这本书的ISBN是什么？"

**查询书籍的售价，结果是书籍原本的定价**

### 订单状态 (order_status)
- [ ] "我的订单12345现在是什么状态？"
- [ ] "查询我购买的《Java编程思想》订单进展"
- [ ] "用户10001的所有订单信息"
- [ ] "我最近买的书发货了吗？"
- [ ] "帮我查一下订单号为12346的状态"

**查询订单结果是书籍的详细信息**

### 图书推荐 (book_recommendation)
- [ ] "推荐几本计算机类的书籍"
- [ ] "有什么数学方面的好书推荐？"
- [ ] "给我推荐一些文学类的书"
- [ ] "计算机学院的教材有哪些推荐？"
- [x] "我想看看有什么畅销书推荐"

**不能推荐具体类别的书籍**

### 统计信息 (statistics)
- [x] "平台上总共有多少本书？"
- [x] "当前注册用户数量是多少？"
- [ ] "今天有多少订单成交？"
- [x] "系统中有多少在售图书？"
- [ ] "告诉我本月的销售统计"

### 创建订单 (order_create)
- [ ] "我想购买《Java编程思想》这本书"
- [x] "如何下单购买《高等数学》？"
- [x] "帮我创建一个订单，买《算法导论》"
- [x] "我要买这本《离散数学》，怎么操作？"
- [x] "购买《数据结构》的流程是什么？"

您可以使用这些测试语句来验证系统的意图识别功能，看看它能否正确地将用户输入映射到相应的意图，并返回期望的结果。

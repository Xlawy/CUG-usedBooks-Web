# CUG-usedBooks-Web 代码质量检测与评审方案

## 一、前期准备

### 1.1 环境配置
- [ ] Java JDK 8+
- [ ] Maven 3.6+
- [ ] Node.js 14+
- [ ] SonarQube 9.x
- [ ] Git

### 1.2 工具安装
```bash
# 安装前端依赖
cd ruoyi-ui
npm install -g eslint stylelint
npm install

# 安装SonarQube Scanner
npm install -g sonarqube-scanner
```

## 二、自动化代码检查

### 2.1 后端代码检查

#### Maven项目配置
在`pom.xml`中添加以下插件：
```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.9.1.2184</version>
</plugin>
```

#### SonarQube配置
创建`sonar-project.properties`：
```properties
sonar.projectKey=CUG-usedBooks-Web
sonar.projectName=CUG-usedBooks-Web
sonar.sources=.
sonar.java.binaries=**/target/classes
sonar.java.source=1.8
sonar.sourceEncoding=UTF-8
sonar.language=java
sonar.java.libraries=**/target/classes
```

### 2.2 前端代码检查

#### ESLint配置
在`ruoyi-ui`目录下的`.eslintrc.js`中：
```javascript
module.exports = {
  extends: [
    'plugin:vue/recommended',
    'eslint:recommended'
  ],
  rules: {
    'vue/max-attributes-per-line': 'off',
    'vue/singleline-html-element-content-newline': 'off',
    'vue/multiline-html-element-content-newline': 'off'
  }
}
```

#### StyleLint配置
在`ruoyi-ui`目录下创建`.stylelintrc.js`：
```javascript
module.exports = {
  extends: ['stylelint-config-standard'],
  rules: {
    'no-empty-source': null,
    'selector-class-pattern': null
  }
}
```

## 三、人工代码评审清单

### 3.1 架构评审
- [ ] 项目整体架构是否合理
  - 检查模块划分
  - 检查依赖关系
  - 检查分层结构
- [ ] 是否符合设计原则
  - 单一职责原则
  - 开闭原则
  - 依赖倒置原则
  - 接口隔离原则
  - 里氏替换原则

### 3.2 代码质量评审
- [ ] 代码规范性
  - 命名规范（类名、方法名、变量名）
  - 代码格式（缩进、空格、换行）
  - 注释完整性
- [ ] 代码健壮性
  - 异常处理
  - 边界条件处理
  - 空值处理
- [ ] 代码可维护性
  - 方法长度
  - 类的大小
  - 代码重复度
  - 圈复杂度

### 3.3 安全评审
- [ ] 身份认证
  - 登录安全
  - 密码加密
  - Session管理
- [ ] 授权控制
  - 权限管理
  - 角色管理
  - 数据权限
- [ ] 安全防护
  - SQL注入防护
  - XSS防护
  - CSRF防护
  - 敏感信息加密

### 3.4 性能评审
- [ ] 数据库性能
  - SQL语句优化
  - 索引使用
  - 连接池配置
- [ ] 系统性能
  - 缓存使用
  - 并发处理
  - 资源使用
- [ ] 前端性能
  - 资源加载
  - 页面渲染
  - API调用

## 四、评审流程

### 4.1 自动化检查
1. 运行后端代码检查
```bash
mvn clean verify sonar:sonar
```

2. 运行前端代码检查
```bash
cd ruoyi-ui
npm run lint
npm run stylelint
```

### 4.2 人工评审流程
1. 代码审查会议准备
   - 确定参与人员
   - 准备评审材料
   - 设定评审范围

2. 代码审查执行
   - 逐模块评审
   - 记录问题
   - 讨论解决方案

3. 问题跟踪
   - 建立问题清单
   - 分配责任人
   - 设定修复期限

4. 复查确认
   - 验证修复结果
   - 确认问题解决
   - 总结经验教训

## 五、输出文档

### 5.1 评审报告
- 评审概况
- 问题清单
- 改进建议
- 后续行动

### 5.2 质量指标
- 代码覆盖率
- Bug密度
- 技术债务
- 安全漏洞数
- 性能指标

## 六、持续改进

### 6.1 短期计划
- 修复高优先级问题
- 完善自动化测试
- 优化开发流程

### 6.2 长期计划
- 建立代码规范
- 完善质量体系
- 技术栈升级

## 七、时间规划

| 阶段 | 时间估计 | 主要工作 |
|------|----------|----------|
| 准备阶段 | 2天 | 环境搭建、工具配置 |
| 自动化检查 | 3天 | 运行工具、收集数据 |
| 人工评审 | 5天 | 代码审查、问题讨论 |
| 问题修复 | 7天 | 解决问题、验证结果 |
| 总结报告 | 3天 | 撰写报告、经验总结 |

## 八、注意事项

1. 评审过程中保持客观公正
2. 关注代码质量而不是代码风格
3. 及时记录和跟踪问题
4. 保持良好的团队沟通
5. 注重知识分享和经验总结 
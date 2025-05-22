# SonarQube代码质量分析指南

## 一、环境准备

### 1.1 安装SonarQube
1. 下载SonarQube
   - 访问 [SonarQube官网](https://www.sonarqube.org/downloads/)
   - 下载Community版本（免费版）
   - 解压到本地目录，如：`D:\sonarqube`

2. 配置数据库（推荐使用PostgreSQL）
   ```sql
   CREATE DATABASE sonarqube;
   CREATE USER sonarqube WITH ENCRYPTED PASSWORD 'sonarqube';
   GRANT ALL PRIVILEGES ON DATABASE sonarqube TO sonarqube;
   ```

3. 配置SonarQube
   编辑`conf/sonar.properties`：
   ```properties
   sonar.jdbc.username=sonarqube
   sonar.jdbc.password=sonarqube
   sonar.jdbc.url=jdbc:postgresql://localhost/sonarqube
   sonar.web.port=9000
   ```

4. 启动SonarQube服务
   ```bash
   # Windows
   D:\sonarqube\bin\windows-x86-64\StartSonar.bat
   ```

### 1.2 安装SonarScanner
1. 下载[SonarScanner](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner/)
2. 解压到本地目录，如：`D:\sonar-scanner`
3. 添加环境变量：
   - 将`D:\sonar-scanner\bin`添加到Path环境变量

## 二、项目配置

### 2.1 创建SonarQube项目
1. 访问SonarQube管理界面：`http://localhost:9000`
2. 默认登录凭据：
   - 用户名：admin
   - 密码：admin
3. 创建新项目：
   - Projects -> Create Project
   - 项目名称：CUG-usedBooks-Web
   - 项目key：CUG-usedBooks-Web

### 2.2 配置项目属性
在项目根目录创建`sonar-project.properties`：
```properties
# 必须的配置
sonar.projectKey=CUG-usedBooks-Web
sonar.projectName=CUG-usedBooks-Web
sonar.projectVersion=1.0

# 源代码目录
sonar.sources=ruoyi-admin/src/main/java,ruoyi-framework/src/main/java,ruoyi-system/src/main/java,ruoyi-common/src/main/java,ruoyi-quartz/src/main/java,ruoyi-generator/src/main/java
sonar.java.binaries=**/target/classes
sonar.java.source=1.8

# 前端代码目录
sonar.javascript.node=C:/Program Files/nodejs/node.exe
sonar.sources.frontend=ruoyi-ui/src

# 编码
sonar.sourceEncoding=UTF-8

# 排除目录
sonar.exclusions=**/test/**,**/target/**,**/*.js,**/*.css,**/*.html

# 测试覆盖率配置
sonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml
sonar.junit.reportPaths=**/target/surefire-reports
```

## 三、执行分析

### 3.1 编译项目
```bash
# 清理并编译项目
mvn clean install -DskipTests
```

### 3.2 运行SonarQube分析
```bash
# 使用Maven插件运行
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=your_token

# 或使用SonarScanner运行
sonar-scanner
```

## 四、分析结果评估

### 4.1 质量门禁检查
检查以下关键指标是否满足要求：
- [ ] 代码覆盖率 > 80%
- [ ] 重复代码 < 3%
- [ ] 技术债务比率 < 5%
- [ ] Bug数量 = 0
- [ ] 安全漏洞 = 0
- [ ] 代码异味 < 10个

### 4.2 重点关注问题
1. Blocker/Critical级别问题
   - 安全漏洞
   - 严重Bug
   - 性能问题

2. 代码异味
   - 过长的方法
   - 过于复杂的类
   - 重复代码块

3. 安全热点
   - SQL注入风险
   - 密码加密
   - 敏感数据处理

### 4.3 问题修复优先级
1. P0（必须修复）
   - 所有Blocker问题
   - 安全漏洞
   - 严重性能问题

2. P1（重要）
   - Critical级别问题
   - 主要功能的代码异味
   - 重要模块的覆盖率问题

3. P2（常规）
   - Major级别问题
   - 一般代码异味
   - 次要功能的优化

## 五、持续集成配置

### 5.1 配置Jenkins集成
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn sonar:sonar'
                }
            }
        }
        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }
}
```

### 5.2 配置代码提交检查
在Git hooks中添加pre-commit检查：
```bash
#!/bin/sh
# 运行SonarQube本地分析
mvn sonar:sonar -Dsonar.analysis.mode=preview
```

## 六、注意事项

1. 定期更新
   - 保持SonarQube版本更新
   - 更新分析规则集
   - 更新质量配置文件

2. 团队协作
   - 建立代码审查制度
   - 定期进行团队代码质量会议
   - 共享最佳实践

3. 持续改进
   - 定期回顾质量报告
   - 调整质量门禁标准
   - 优化分析配置

4. 常见问题解决
   - 内存配置优化
   - 分析超时处理
   - 误报处理方法 
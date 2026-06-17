# JellyStudy 部署说明

## 环境要求

- JDK 11+
- Maven 3.6+
- Nacos 2.2.3+ (本地部署)
- MongoDB 4.0+ (Docker部署)

## 启动步骤

### 1. 启动Nacos

```bash
# Windows
startup.cmd -m standalone

# Linux/Mac
sh startup.sh -m standalone
```

Nacos访问地址: http://localhost:8848/nacos

### 2. 启动MongoDB (Docker)

```bash
docker run -d -p 27017:27017 --name jellystudy-mongo mongo:latest
```

### 3. 启动Zipkin (可选)

```bash
# 使用Docker启动Zipkin
docker run -d -p 9411:9411 --name jellystudy-zipkin openzipkin/zipkin

# 或使用Java启动
# 下载 zipkin-server.jar
# java -jar zipkin-server.jar
```

Zipkin UI地址: http://localhost:9411/zipkin/

### 4. 配置大模型API (可选)

编辑 `jellystudy-evaluation/src/main/resources/application.properties`:

```properties
# 配置大模型提供商
llm.provider=openai              # 可选: openai, zhipu, doubao, qwen, mock
llm.api-key=your-api-key-here    # 替换为您的API Key
llm.base-url=                    # 可选: 自定义API地址
llm.model=gpt-3.5-turbo          # 模型名称
llm.temperature=0.7              # 温度参数
llm.max-tokens=1024              # 最大token数
```

**支持的大模型提供商:**

| 提供商 | provider值 | 默认模型 | 说明 |
|--------|-----------|---------|------|
| OpenAI | openai | gpt-3.5-turbo | 需要OpenAI API Key |
| 智谱AI | zhipu | glm-4 | 需要智谱API Key |
| 豆包 | doubao | ep-20240822001 | 需要字节跳动API Key |
| 通义千问 | qwen | qwen-turbo | 需要阿里云API Key |
| Mock | mock | - | 模拟模式，无需API Key |

### 5. 启动JellyStudy服务

使用提供的启动脚本:

```bash
# Windows
start-all-services.bat

# 或手动启动每个服务
startup.cmd -m standalone
cd jellystudy-knowledge && mvn spring-boot:run
cd jellystudy-evaluation && mvn spring-boot:run
cd jellystudy-ai && mvn spring-boot:run
cd jellystudy-question && mvn spring-boot:run
cd jellystudy-studyplan && mvn spring-boot:run
cd jellystudy-companion && mvn spring-boot:run
```
netstat -ano | Select-String ":808[0-4]\s" | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique | ForEach-Object { Stop-Process -Id $_ -Force }

## 服务端口

| 服务 | 端口 | 描述 |
|------|------|------|
| jellystudy-knowledge | 8081 | 知识点服务 |
| jellystudy-evaluation | 8082 | 智能评估服务 |
| jellystudy-ai | 8083 | AI服务 |
| jellystudy-question | 8080 | 问答服务 |

## Dubbo端口

| 服务 | Dubbo端口 |
|------|-----------|
| jellystudy-knowledge | 20881 |
| jellystudy-evaluation | 20883 |
| jellystudy-ai | 20884 |
| jellystudy-question | 20882 |

## API接口

### 问答服务

- POST /api/questions - 创建问题
- GET /api/questions - 获取所有问题
- GET /api/questions/{id} - 获取单个问题
- POST /api/questions/{questionId}/answers - 添加回答
- GET /api/questions/hot - 获取热门问题
- GET /api/questions/search?keyword=xxx - 搜索问题

### 评估服务

通过Dubbo调用:
- evaluateQuestion - 评估问题(规则引擎)
- evaluateAnswer - 评估答案(规则引擎)
- evaluateQuestionWithLLM - 评估问题(大模型)
- evaluateAnswerWithLLM - 评估答案(大模型)
- saveQuestionEvaluation - 保存问题评估
- saveAnswerEvaluation - 保存答案评估

## 调用链监控

所有微服务已集成Zipkin调用链监控：

1. **启动Zipkin**
   ```bash
   docker run -d -p 9411:9411 --name jellystudy-zipkin openzipkin/zipkin
   ```

2. **访问Zipkin UI**
   打开浏览器访问: http://localhost:9411/zipkin/

3. **已配置的参数**
   - spring.sleuth.sampler.probability=1.0 (采样率100%)
   - spring.zipkin.base-url=http://localhost:9411/
   - 所有4个微服务都已配置好

## 配置说明

所有服务配置文件位于 `src/main/resources/application.properties`:

- MongoDB配置: spring.data.mongodb.*
- Dubbo配置: dubbo.*
- Nacos配置: dubbo.registry.address=nacos://localhost:8848
- 大模型配置: llm.*
- Zipkin配置: spring.zipkin.*, spring.sleuth.*



netstat -ano | Select-String ":808[0-3]\s" | ForEach-Object { ($_ -split '\s+')[-1] } | Sort-Object -Unique | ForEach-Object { Stop-Process -Id $_ -Force }

docker stop zipkin
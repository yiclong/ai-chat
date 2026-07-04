# ai-chat

基于 Spring Boot 的 AI 聊天应用，支持流式响应和多模型切换。

## 技术栈

- Java 8
- Spring Boot 2.4.0
- OkHttp 4.9.3 (SSE 流式请求)
- Thymeleaf
- WebSocket

## 功能特性

- 支持多种 AI 模型切换
- SSE 流式响应
- WebSocket 实时通信
- 可自定义模型参数

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.x

### 配置

修改 `src/main/resources/application.yml`：

```yaml
openai:
  api-key: your-api-key
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  model: qwen-turbo
```

### 运行

```bash
mvn spring-boot:run
```

访问 http://localhost:9001

## API 接口

### 发送聊天消息

```
POST /api/chat/message
Content-Type: application/json

{
  "message": "你好",
  "model": "qwen-plus"
}
```

参数说明：
- `message` (必填): 聊天消息内容
- `model` (可选): 指定使用的模型，不传则使用默认模型

响应：SSE 流式文本

### 获取可用模型列表

```
GET /api/chat/models
```

响应示例：
```json
[
  {
    "id": "qwen-turbo",
    "object": "model",
    "created": 1234567890,
    "owned_by": "system"
  },
  {
    "id": "qwen-plus",
    "object": "model",
    "created": 1234567890,
    "owned_by": "system"
  }
]
```

### 获取配置信息

```
GET /api/chat/config
```

响应示例：
```json
{
  "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "model": "qwen-turbo"
}
```

## 项目结构

```
src/main/java/com/ycl/aichat/
├── config/                 # 配置类
│   ├── WebSocketConfig.java
│   └── WebSocketServer.java
├── controller/             # 控制器
│   ├── ChatController.java
│   └── SystemController.java
├── dto/                    # 数据传输对象
│   ├── ChatMessageRequest.java
│   ├── ChatRequest.java
│   └── Message.java
├── service/                 # 服务层
│   └── ChatService.java
└── AiChatApplication.java   # 启动类
```

## License

MIT
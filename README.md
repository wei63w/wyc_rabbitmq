# wyc_rabbitmq

基于 Spring Boot、RabbitMQ 和 MySQL 的订单可靠消息示例。项目使用 **Outbox（本地消息表）** 模式：订单与待发送消息在同一数据库事务中写入，定时任务在事务提交后投递 RabbitMQ，消费者通过消息 ID 实现幂等处理。

## 流程

1. 创建订单时，在同一个本地事务内写入订单和 `outbox` 待发送记录。
2. `OutboxTask` 每 5 秒扫描待发送记录，将消息投递到 `order_exchange`，路由键为 `order.create`。
3. `order_consume_queue` 的消费者手动 ACK；重复投递时根据消息 ID 跳过业务处理。
4. 投递或后续处理失败时，Outbox 记录重试计数，下一轮任务重新投递。

## 本地运行

### 前置条件

- JDK 21
- Maven（或使用项目内的 Maven Wrapper）
- MySQL 8，创建 `test` 数据库并执行 `sql_bak/sql.sql`
- RabbitMQ，默认配置为 `127.0.0.1:5672`、虚拟主机 `/`

请按本地环境修改 `src/main/resources/application.yaml` 中的数据库和 RabbitMQ 连接配置。仓库内配置仅供本地开发示例使用，发布到实际环境时应通过环境变量或密钥管理服务提供凭据。

启动：

```bash
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

## Canal 本地监听配置

项目可使用 Canal 监听 MySQL 的 `test.t_outbox` 新增记录，再异步投递 RabbitMQ。

### 创建 Canal Server 容器

在 Docker Desktop（MySQL 运行在本机）中，使用 `host.docker.internal` 访问 MySQL。不要将空目录挂载到 `/home/admin/canal-server/conf`，否则会覆盖镜像自带的 `canal.properties` 和 `logback.xml`，造成 Canal 无法启动。

```powershell
docker rm -f canal-server

docker run -d --name canal-server `
  -p 11111:11111 `
  -v /data/canal/logs:/home/admin/canal-server/logs `
  -e canal.instance.master.address=host.docker.internal:3306 `
  -e canal.instance.dbUsername=canal `
  -e canal.instance.dbPassword=Canal@123456 `
  -e 'canal.instance.filter.regex=test\.t_outbox' `
  -e canal.user=canal `
  -e canal.passwd=E3619321C1A937C46A0D8BD1DAC39F93B27D4458 `
  canal/canal-server:v1.1.7
```

`canal.instance.dbUsername` 和 `canal.instance.dbPassword` 是 Canal Server 连接 MySQL 的账号；`canal.user` 与 `canal.passwd` 则是 Java 客户端连接 Canal Server 的账号。Java 侧默认使用明文 `canal/canal`；其中 `E3619321C1A937C46A0D8BD1DAC39F93B27D4458` 是服务端所需的 `canal` 密码摘要。

### 创建并授权 MySQL 用户

使用 MySQL `root` 账号执行以下命令。Canal 需要读取表结构并订阅 binlog，缺少 `performance_schema` 的读取权限会导致 instance 无法启动。

```sql
CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED BY 'Canal@123456';

GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT
ON *.* TO 'canal'@'%';

FLUSH PRIVILEGES;
```

MySQL 还需要启用 binlog，并设置 `binlog_format=ROW` 和非零的 `server_id`。完成配置后，使用 `docker exec canal-server ps -ef` 确认存在 Canal Java 进程，再启动本项目。

## `messageId` 消费报错说明

若消费者出现以下异常：

```text
Missing header 'messageId' for method parameter type [class java.lang.String]
```

原因是发送端调用 `MessageProperties#setMessageId(msgId)` 写入的是 RabbitMQ 的标准消息属性。Spring AMQP 在消费端将它映射为 `amqp_messageId`，而不是自定义头 `messageId`。

消费者应使用 Spring 提供的常量：

```java
@Header(AmqpHeaders.MESSAGE_ID) String msgId
```

不要使用：

```java
@Header("messageId") String msgId
```

该调整只改变当前监听方法的参数绑定；不会改变发送端 `setMessageId(msgId)` 的行为，也不会影响消息体、交换机、路由键或已有的幂等逻辑。已在异常消息头中确认存在 `amqp_messageId`。

> 参数解析发生在监听方法执行之前。因此头名称不匹配时，方法内的 `try/catch` 和 ACK/NACK 逻辑不会运行，消息可能被重新投递。修正绑定名称后，消息才能进入幂等处理。

## 关键代码

- `MqSendUtil`：设置并投递消息 ID。
- `OutboxTask`：扫描 Outbox 并在事务提交后投递消息。
- `OrderConsumer`：手动 ACK 与基于消息 ID 的消费幂等。

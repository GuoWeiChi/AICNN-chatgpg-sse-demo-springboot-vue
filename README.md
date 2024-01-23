本项目用SpringBoot和vue实现了仿ChatGPT官网聊天的打字机效果，克隆项目到本地可以直接运行。主要应用的技术有:SpringBoot、Vue、Reactive、WebFlux、EventSource等，学习和练手的好项目。**实现效果如下**：

![5](imgs/5.gif)

使用本技术实现的项目：[aicnn.cn](https://aicnn.cn)，欢迎大家体验

准备好了吗，<font color="red">let's get it!</font> 

## 项目运行

springboot安装依赖，并设置对应的sd-key即可：

![image-20240123101054604](imgs/image-20240123101054604.png)

api key 的获取方式如下：

+ 第一步：打开[aicnn.cn](https://aicnn.cn)
+ 第二步：进入设置页面
+ 第三步：点击创建新的秘钥
+ 第四步：复制密钥值，替换上面代码中的`sk-*******`,替换后的代码如下所示：`                .header("Authorization", "Bearer sk-1234567890123456789")`

![image-20240123080314630](imgs/image-20240123080314630.png)

前端项目采用vue3实现，

在项目中,使用如下命令运行项目，即可运行前端:

```bash
yarn install
yarn serve
```

![image-20240123101428427](imgs/image-20240123101428427.png)

## 在Spring Boot中实现SSE

### 设置SSE

在Spring Boot中实现SSE并不复杂。其核心在于使用Spring框架的`webflux`。使用webflux类能够创建一个持久的连接，使服务器能够向客户端发送多个事件，而无需每次都建立新的连接。

先在pom中引入相关依赖：

```xml
<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

然后需要在Controller中创建一个方法来返回`Flux`实例。这个方法将被映射到特定的URL，客户端将使用这个URL来接收事件。

```java
package cn.aicnn.chatssespringboot.controller;

import cn.aicnn.chatssespringboot.dto.AIAnswerDTO;
import cn.aicnn.chatssespringboot.service.GptServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;


@RestController
public class ChatController {

    //用于流式请求第三方的实现类
    @Resource
    GptServiceImpl gptService;

    //通过stream返回流式数据
    @GetMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AIAnswerDTO>> getStream(@RequestParam("messages")String messages) {
        return gptService.doChatGPTStream(messages)//实现类发送消息并获取返回结果
                .map(aiAnswerDTO -> ServerSentEvent.<AIAnswerDTO>builder()//进行结果的封装，再返回给前端
                        .data(aiAnswerDTO)
                        .build()
                )
                .onErrorResume(e -> Flux.empty());//发生异常时发送空对象
    }

}

```

### 处理连接和事件

一旦有客户端连接到这个URL，可以通过调用`GptServiceImpl`实例的`doChatGPTStream`方法来发送事件。这些事件可以是简单的字符串消息，也可以是更复杂的数据结构，如JSON对象。记住，SSE的设计初衷是轻量级和简单，所以你发送的每个事件都应当是独立的和自包含的。

`GptServiceImpl`的实现方式如下，也是springboot后端实现的重点

```java
package cn.aicnn.chatssespringboot.service;

import cn.aicnn.chatssespringboot.dto.AIAnswerDTO;
import cn.aicnn.chatssespringboot.dto.ChatRequestDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.*;


/**
 * @author aicnn.cn
 * @date 2023/2/13
 * @description: aicnn.cn
 **/
@Service
public class GptServiceImpl {
    //webflux的client
    private WebClient webClient;

    //用于读取第三方的返回结果
    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void postConstruct() {
        this.webClient = WebClient.builder()//创建webflux的client
                .baseUrl("https://api.aicnn.cn/v1")//填写对应的api地址
                .defaultHeader("Content-Type", "application/json")//设置默认请求类型
                .build();
    }

    //请求stream的主题
    public Flux<AIAnswerDTO> doChatGPTStream(String requestQuestion) {

        //构建请求对象
        ChatRequestDTO chatRequestDTO = new ChatRequestDTO();
        chatRequestDTO.setModel("gpt-3.5-turbo");//设置模型
        chatRequestDTO.setStream(true);//设置流式返回

        ChatRequestDTO.ReqMessage message = new ChatRequestDTO.ReqMessage();//设置请求消息，在此可以加入自己的prompt
        message.setRole("user");//用户消息
        message.setContent(requestQuestion);//用户请求内容
        ArrayList<ChatRequestDTO.ReqMessage> messages = new ArrayList<>();
        messages.add(message);
        chatRequestDTO.setMessages(messages);//设置请求消息


        //构建请求json
        String paramJson = JSONUtil.toJsonStr(chatRequestDTO);;

        //使用webClient发送消息
        return this.webClient.post()
                .uri("/chat/completions")//请求uri
                .header("Authorization", "Bearer sk-**************")//设置成自己的key，获得key的方式可以在下文查看
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)//设置流式响应
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(paramJson))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(result -> handleWebClientResponse(result));//接收到消息的处理方法
    }

    private Flux<AIAnswerDTO> handleWebClientResponse(String resp) {
        if (StrUtil.equals("[DONE]",resp)){//[DONE]是消息结束标识
            return Flux.empty();
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(resp);
            AIAnswerDTO result = objectMapper.treeToValue(jsonNode, AIAnswerDTO.class);//将获得的结果转成对象
            if (CollUtil.size(result.getChoices())  > 0 && !Objects.isNull(result.getChoices().get(0)) &&
                    !StrUtil.isBlank(result.getChoices().get(0).delta.getError())){//判断是否有异常
                throw new RuntimeException(result.getChoices().get(0).delta.getError());
            }
            return Flux.just(result);//返回获得的结果
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
```

在这里，我们请求的api地址设置成https://api.aicnn.cn/v1，并且设置对应的api key: Authorization,api key 的获取方式如下：

+ 第一步：打开[aicnn.cn](https://aicnn.cn)
+ 第二步：进入设置页面
+ 第三步：点击创建新的秘钥
+ 第四步：复制密钥值，替换上面代码中的`sk-*******`,替换后的代码如下所示：`                .header("Authorization", "Bearer sk-1234567890123456789")`

![image-20240123080314630](imgs/image-20240123080314630.png)

### 处理异常和断开连接

在使用SSE时，处理异常和断开连接也非常重要。确保在客户端断开连接或发生异常时，正确地关闭`webflux`实例。这有助于避免资源泄露和其他潜在问题。

### 一些实用的提示

- **超时管理**：SSE连接可能因为超时而被关闭。确保妥善处理这种情况。
- **错误处理**：适当地处理可能发生的异常，如网络问题或客户端断开连接。
- **资源清理**：在连接结束时清理资源，确保应用的健康和性能。

**至此，我们就完成了SpringBoot的SSE后端开发。**

## Vue前端对SSE的处理

### 在Vue中接收SSE

在Vue应用中接收SSE消息是相对直截了当的。需要做的基本上就是在Vue组件中创建一个新的`EventSource`实例，并指向你的Spring Boot应用中设置的SSE URL,本文使用EventSource作为示例，也可以选择axios或@microsoft/fetch-event-source发送post请求的SSE请求，使用另外两种的好处是可以控制header，携带token信息，以便于控制权限。

```js
        this.eventSource = new EventSource('http://127.0.0.1:8080/completions?messages='+this.inputText);
```

一旦建立了连接，就可以定义各种事件监听器来处理从服务器接收到的消息。在Vue中，这通常涉及到更新组件的数据属性，这些属性又通过Vue的响应式系统自动更新UI。

```js
sendSSEMessage() {
      // 只有当eventSource不存在时才创建新的EventSource连接
      if (!this.eventSource) {
        this.messages.push({text: this.inputText, isMine: true});
        this.messages.push({text: "", isMine: false});

        // 创建新的EventSource连接
        this.eventSource = new EventSource('http://127.0.0.1:8080/completions?messages='+this.inputText);

        // 设置消息接收的回调函数
        this.eventSource.onmessage = (event) => {
          const data = JSON.parse(event.data);
          this.messages[this.messages.length - 1].text += data.choices[0].delta.content;
        };

        // 可选：监听错误事件，以便在出现问题时能够重新连接或处理错误
        this.eventSource.onerror = (event) => {
          console.error("EventSource failed:", event);
          this.eventSource.close(); // 关闭出错的连接
          this.eventSource = null; // 重置eventSource变量，允许重建连接
        };
      }
    }
```

### 保证流畅的用户体验

当处理实时数据时，保证一个流畅且不中断的用户体验至关重要。在Vue中，这意味着需要确保UI的更新是平滑和高效的。幸运的是，Vue的响应式系统会处理大部分重活，但你仍需要注意不要进行不必要的大规模DOM操作或数据处理。

### 异常处理和重连

处理连接中断或其他异常也是至关重要的。你可能需要在失去连接时尝试重新连接，或者至少提醒用户当前的连接状态。这可以通过监听`EventSource`的错误事件并采取适当的行动来实现。

### 小结

将SSE与Vue结合使用，可以为用户提供一个富有动态性和实时性的界面。无论是实时消息、通知，还是实时数据流，SSE都能让Vue应用更加生动和实用。

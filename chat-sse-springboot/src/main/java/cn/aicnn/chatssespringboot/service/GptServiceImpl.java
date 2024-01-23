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

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
 * @author Guowei Chi
 * @date 2023/2/13
 * @description: https://aigptx.top/pay
 **/
@Service
public class GptServiceImpl {

    private WebClient webClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void postConstruct() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.aicnn.cn/v1")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Flux<AIAnswerDTO> doChatGPTStream(String requestQuestion) {

        ChatRequestDTO chatRequestDTO = new ChatRequestDTO();
        chatRequestDTO.setModel("gpt-3.5-turbo");
        chatRequestDTO.setStream(true);

        ChatRequestDTO.ReqMessage message = new ChatRequestDTO.ReqMessage();
        message.setRole("user");
        message.setContent(requestQuestion);
        ArrayList<ChatRequestDTO.ReqMessage> messages = new ArrayList<>();
        messages.add(message);
        chatRequestDTO.setMessages(messages);


        //构建请求json
        String paramJson = JSONUtil.toJsonStr(chatRequestDTO);;


        return this.webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer sk-asfsafsfsfdewr3wrfsffffsadfsafsd")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(paramJson))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(result -> handleWebClientResponse(result));
    }

    private Flux<AIAnswerDTO> handleWebClientResponse(String resp) {
        if (StrUtil.equals("[DONE]",resp)){
            return Flux.empty();
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(resp);
            AIAnswerDTO result = objectMapper.treeToValue(jsonNode, AIAnswerDTO.class);
            if (CollUtil.size(result.getChoices())  > 0 && !Objects.isNull(result.getChoices().get(0)) &&
                    !StrUtil.isBlank(result.getChoices().get(0).delta.getError())){
                throw new RuntimeException(result.getChoices().get(0).delta.getError());
            }
            return Flux.just(result);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}

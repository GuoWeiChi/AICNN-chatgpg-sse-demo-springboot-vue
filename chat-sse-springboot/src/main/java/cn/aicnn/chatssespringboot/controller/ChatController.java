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

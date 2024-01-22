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

    @Resource
    GptServiceImpl gptService;

    @GetMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AIAnswerDTO>> getStream(@RequestParam("messages")String messages) {
        return gptService.doChatGPTStream(messages)
                .map(aiAnswerDTO -> ServerSentEvent.<AIAnswerDTO>builder()
                        .data(aiAnswerDTO)
                        .build()
                )
                .onErrorResume(e -> Flux.empty());
    }

}

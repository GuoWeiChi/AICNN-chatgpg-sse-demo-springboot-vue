package cn.aicnn.chatssespringboot.dto;

import lombok.Data;

import java.util.List;

/**
 * @author Guowei Chi
 * @date 2023/2/14
 * @description:
 **/
@Data
public class ChatRequestDTO {
    private String model;

    private List<ReqMessage> messages;

    private Boolean stream;

    @Data
    public static class ReqMessage{
        private String role;
        private String content;
    }

}

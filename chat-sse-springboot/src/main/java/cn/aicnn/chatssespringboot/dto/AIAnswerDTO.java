package cn.aicnn.chatssespringboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Guowei Chi
 * @date 2023/2/14
 * @description:
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIAnswerDTO {
    /**
     * 对话id
     */
    private String id;
    /**
     * 聊天object 例如chat.completion.chunk
     */
    private String object;
    /**
     * 创建id 例如：1705764591
     */
    private int created;
    /**
     * 使用的模型
     */
    private String model;
    /**
     * gpt回答内容
     */
    private List<ChoicesDTO> choices = new ArrayList<>();

    /**
     * 完成原因
     */
    private String finish_reason;
    /**
     * 错误原因
     */
    public String error;

    /***
     * 消耗的token数量
     */
    private AIAnswerUsageDTO usage;

    /**
     * 回答的内容
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChoicesDTO {
        /**
         * 索引
         */
        public int index;
        /**
         * 角色以及回答的内容
         */
        public Delta delta;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        /**
         * 角色
         */
        public String role;
        /**
         * 回复内容
         */
        public String content="";
        /**
         * 错误原因
         */
        public String error="";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AIAnswerUsageDTO {
        /**
         * 提示词消耗的token数
         */
        public int prompt_tokens;
        /**
         * 对话消耗的token数
         */
        public int completion_tokens;
        /**
         * 总token数
         */
        public int total_tokens;
    }
}

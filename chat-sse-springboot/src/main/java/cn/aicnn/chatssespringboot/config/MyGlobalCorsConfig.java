package cn.aicnn.chatssespringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyGlobalCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 对所有的路径允许跨域请求
                .allowedOrigins("*") // 允许来自任何源的请求
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的请求方法
                .allowedHeaders("*") // 允许的请求头
                .allowCredentials(false) // 是否允许证书（cookies），根据需要设置
                .maxAge(3600); // 预检请求的缓存时间（秒）
    }
}
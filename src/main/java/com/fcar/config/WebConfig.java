package com.fcar.config;

import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PhoneNumberRequiredInterceptor phoneNumberRequiredInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(phoneNumberRequiredInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Phục vụ ảnh upload từ thư mục uploads/ (đường dẫn tuyệt đối, trùng với nơi controller lưu file)
        String uploadsPath = Paths.get("uploads").toAbsolutePath().normalize().toUri().toString();
        if (!uploadsPath.endsWith("/")) uploadsPath += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath);
    }
}


package fr.uga.miage.l3.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof JacksonJsonHttpMessageConverter) {
                JacksonJsonHttpMessageConverter jsonConverter = (JacksonJsonHttpMessageConverter) converter;
                List<MediaType> supported = new ArrayList<>(jsonConverter.getSupportedMediaTypes());
                System.out.println("Supported :" + supported.toString());
                // accept GeoJSON responses which use content-type application/geo+json
                supported.add(MediaType.valueOf("application/geo+json;charset=UTF-8"));
                jsonConverter.setSupportedMediaTypes(supported);
            }
        }
    }
}

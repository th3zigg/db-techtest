package com.db.dataplatform.techtest.server.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class ServerMapperConfiguration {

    @Bean
    public ModelMapper createModelMapperBean() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true);

        return modelMapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
         return Jackson2ObjectMapperBuilder.json().build();
    }
}

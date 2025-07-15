package br.com.ccs.rinha.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.text.DateFormat;

public class ObjectMapperFactory {

    public static final ObjectMapper instance = produceObjectMapper();

    private ObjectMapperFactory() {
    }

    public static ObjectMapper getInstance() {
        return instance;
    }

    private static ObjectMapper produceObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new JavaTimeModule())
                .setDateFormat(DateFormat.getDateTimeInstance());
    }

}

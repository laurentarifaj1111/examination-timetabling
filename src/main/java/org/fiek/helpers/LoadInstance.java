package org.fiek.helpers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fiek.models.Instance;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


public class LoadInstance {

    public static Instance instance(String name) throws IOException {
        String fileName;
        if (!name.contains("json")) {
            fileName = String.join(".", name, "json");
        } else {
            fileName = name;
        }
        InputStream inputStream= Objects.requireNonNull(LoadInstance.class.getClassLoader()
                .getResourceAsStream("InstancesJSON/" + fileName));
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper.readValue(inputStream, Instance.class);
    }
}

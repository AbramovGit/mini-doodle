package com.abramovgit.minidoodle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI miniDoodleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Doodle API")
                        .version("v1")
                        .description("""
                                Create a user, declare FREE slots, then book a slot as a meeting.
                                Booking changes its slot to BUSY; cancelling the meeting returns it to FREE.
                                All timestamps use UTC ISO-8601 format. Availability treats time outside
                                declared FREE slots as BUSY. Pagination is zero-based.
                                """));
    }
}

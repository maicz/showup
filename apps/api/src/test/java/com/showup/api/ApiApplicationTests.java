package com.showup.api;

import com.showup.api.event.EventRepository;
import com.showup.api.event.EventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void flywayMigrationSeedsEvents(@Autowired EventRepository events) {
        assertThat(events.findAllByStatusOrderByStartsAtAsc(EventStatus.PUBLISHED))
                .extracting(event -> event.getTitle())
                .contains("Spring Boot Meetup", "Angular Signals Workshop");
    }
}

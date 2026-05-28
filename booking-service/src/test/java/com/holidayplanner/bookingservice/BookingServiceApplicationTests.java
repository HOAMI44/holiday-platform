package com.holidayplanner.bookingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.holidayplanner.bookingservice.client.EventServiceClient;

@SpringBootTest
@ActiveProfiles("test")
class BookingServiceApplicationTests {

    @MockBean
    EventServiceClient eventServiceClient;

    @Test
    void contextLoads() {
    }
}

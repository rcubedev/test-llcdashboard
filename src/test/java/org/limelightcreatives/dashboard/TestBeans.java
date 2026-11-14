package org.limelightcreatives.dashboard;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Tests use in-memory Google Sheets and a clock they can set instead of the real ones. */
@TestConfiguration(proxyBeanMethods = false)
public class TestBeans {

    @Bean
    @Primary
    TestClock testClock() {
        return new TestClock();
    }

    @Bean
    @Primary
    FakeSpreadsheets fakeSpreadsheets() {
        return new FakeSpreadsheets();
    }
}

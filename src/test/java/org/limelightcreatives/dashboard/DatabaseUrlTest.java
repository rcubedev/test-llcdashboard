package org.limelightcreatives.dashboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlTest {

    @Test
    void neonStyleAddressesAreSplitUp() {
        var properties = DatabaseUrl.toDataSourceProperties(
                "postgresql://limelight_owner:p%40ss%2Fword@ep-cool-name-123.ap-southeast-2.aws.neon.tech/limelight?sslmode=require&channel_binding=require");
        assertThat(properties).containsEntry("spring.datasource.url",
                        "jdbc:postgresql://ep-cool-name-123.ap-southeast-2.aws.neon.tech/limelight?sslmode=require&channel_binding=require")
                .containsEntry("spring.datasource.username", "limelight_owner")
                .containsEntry("spring.datasource.password", "p@ss/word");
    }

    @Test
    void portsAndPostgresSchemeWork() {
        assertThat(DatabaseUrl.toDataSourceProperties("postgres://u:p@localhost:5433/auth"))
                .containsEntry("spring.datasource.url", "jdbc:postgresql://localhost:5433/auth");
    }
}

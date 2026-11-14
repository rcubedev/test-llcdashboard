package org.limelightcreatives.dashboard;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Idle connections must close, or Neon never sleeps (and Neon's free compute hours run out). */
class ConnectionPoolTest extends DatabaseTest {

    @Autowired DataSource dataSource;

    @Test
    void idleConnectionsClose() {
        var pool = (HikariDataSource) dataSource;
        assertThat(pool.getMinimumIdle()).isZero();
        assertThat(pool.getIdleTimeout()).isLessThanOrEqualTo(60_000);
        assertThat(pool.getMaximumPoolSize()).isLessThanOrEqualTo(5);
    }
}

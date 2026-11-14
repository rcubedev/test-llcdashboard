package org.limelightcreatives.dashboard.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonTest {

    @Test
    void initials() {
        assertThat(new Person("1", "a@b", "Sam Lee", "STAFF").initials()).isEqualTo("SL");
        assertThat(new Person("1", "a@b", "sam van der berg", "STAFF").initials()).isEqualTo("SV");
        assertThat(new Person("1", "a@b", null, "STAFF").initials()).isEqualTo("?");
    }

    @Test
    void staffAreAdminsAndStaff() {
        assertThat(new Person("1", "a@b", "Sam", "ADMIN").isStaff()).isTrue();
        assertThat(new Person("1", "a@b", "Sam", "STAFF").isStaff()).isTrue();
        assertThat(new Person("1", "a@b", "Sam", "PARTICIPANT").isStaff()).isFalse();
    }
}

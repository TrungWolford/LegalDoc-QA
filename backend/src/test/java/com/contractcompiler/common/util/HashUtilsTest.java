package com.contractcompiler.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilsTest {

    @Test
    void sha256ProducesConsistentHex() {
        String hash = HashUtils.sha256("hello");
        assertThat(hash).hasSize(64);
        assertThat(hash).isEqualTo(HashUtils.sha256("hello")); // deterministic
    }

    @Test
    void sha256DifferentInputsDifferentHash() {
        assertThat(HashUtils.sha256("a")).isNotEqualTo(HashUtils.sha256("b"));
    }

    @Test
    void sha256ByteArrayMatchesString() {
        assertThat(HashUtils.sha256("test".getBytes())).isEqualTo(HashUtils.sha256("test"));
    }
}

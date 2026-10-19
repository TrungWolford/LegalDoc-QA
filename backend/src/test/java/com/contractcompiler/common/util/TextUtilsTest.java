package com.contractcompiler.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

    @Test
    void normalizeUnicodeConvertsSmartQuotes() {
        String input = "\u201CHello\u201D \u2018World\u2019";
        assertThat(TextUtils.normalizeUnicode(input)).isEqualTo("\"Hello\" 'World'");
    }

    @Test
    void normalizeUnicodeConvertsDashes() {
        assertThat(TextUtils.normalizeUnicode("2024\u20132025")).isEqualTo("2024-2025");
        assertThat(TextUtils.normalizeUnicode("term\u2014definition")).isEqualTo("term-definition");
    }

    @Test
    void normalizeUnicodeCollapsesWhitespace() {
        assertThat(TextUtils.normalizeUnicode("hello   world")).isEqualTo("hello world");
    }

    @Test
    void normalizeUnicodeHandlesNull() {
        assertThat(TextUtils.normalizeUnicode(null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "'REF_BROKEN|2.3|Section 7.4', 'REF_BROKEN|2.3|Section 7.4'",
    })
    void fingerprintIsDeterministic(String input1, String input2) {
        String fp1 = TextUtils.computeFingerprint("REF_BROKEN", "2.3", "Section 7.4");
        String fp2 = TextUtils.computeFingerprint("REF_BROKEN", "2.3", "Section 7.4");
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void fingerprintIsStableAcrossCosmeticEdits() {
        String fp1 = TextUtils.computeFingerprint("REF_BROKEN", "2.3", "Section  7.4");
        String fp2 = TextUtils.computeFingerprint("REF_BROKEN", "2.3", "Section 7.4");
        // Extra space is stripped in normalization
        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    void fingerprintChangesOnSemanticEdit() {
        String fp1 = TextUtils.computeFingerprint("REF_BROKEN", "2.3", "Section 7.4");
        String fp2 = TextUtils.computeFingerprint("REF_BROKEN", "2.3", "Section 8.1");
        assertThat(fp1).isNotEqualTo(fp2);
    }
}

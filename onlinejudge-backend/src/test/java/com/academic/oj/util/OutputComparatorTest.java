package com.academic.oj.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OutputComparatorTest {

    @Test
    void shouldIgnoreLineEndSpacesAndTrailingBlankLines() {
        String expected = "1 2 3\n4 5 6\n";
        String actual = "1 2 3  \n4 5 6\t\n\n";

        Assertions.assertTrue(OutputComparator.equalsIgnorePresentation(expected, actual));
    }

    @Test
    void shouldKeepLeadingSpacesMeaningful() {
        String expected = " 42\n";
        String actual = "42\n";

        Assertions.assertFalse(OutputComparator.equalsIgnorePresentation(expected, actual));
    }

    @Test
    void shouldNormalizeMixedLineEndings() {
        String expected = "a\nb\nc\n";
        String actual = "a\r\nb\rc\n";

        Assertions.assertTrue(OutputComparator.equalsIgnorePresentation(expected, actual));
    }
}


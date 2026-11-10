/*
 * Unit tests for NutriScoreSpectrumView.
 * Tests grade normalization, null handling, and active grade inspection.
 */

package com.foodsense;

import com.foodsense.ui.NutriScoreSpectrumView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NutriScoreSpectrumViewTest {

    @ParameterizedTest
    @ValueSource(strings = {"a", "A", "b", "B", "c", "C", "d", "D", "e", "E"})
    void setGrade_normalizesValidGrades(String grade) {
        NutriScoreSpectrumView view = new NutriScoreSpectrumView(grade);
        assertEquals(grade.toUpperCase(), view.getGrade());
    }

    @Test
    void setGrade_handlesNullAndBlank() {
        NutriScoreSpectrumView view = new NutriScoreSpectrumView("A");
        assertEquals("A", view.getGrade());

        view.setGrade(null);
        assertNull(view.getGrade());

        view.setGrade("   ");
        assertNull(view.getGrade());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "Z", "123", "F", "not-a-grade", "a b"})
    void setGrade_treatsInvalidGradesAsUnavailable(String invalidGrade) {
        NutriScoreSpectrumView view = new NutriScoreSpectrumView("A");
        assertEquals("A", view.getGrade());

        view.setGrade(invalidGrade);
        assertNull(view.getGrade());
        assertEquals("Nutri-Score not available", view.getToolTipText());
    }

    @Test
    void setGrade_setsDescriptiveToolTip() {
        NutriScoreSpectrumView view = new NutriScoreSpectrumView();
        view.setGrade("b");
        assertEquals("Nutri-Score: Grade B", view.getToolTipText());

        view.setGrade(null);
        assertEquals("Nutri-Score not available", view.getToolTipText());
    }
}

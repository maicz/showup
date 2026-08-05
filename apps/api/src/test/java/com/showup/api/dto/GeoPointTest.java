package com.showup.api.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoPointTest {

    @Test
    void aValidCoordinatePairIsAccepted() {
        GeoPoint point = new GeoPoint(44.4268, 26.1025);
        assertThat(point.latitude()).isEqualTo(44.4268);
        assertThat(point.longitude()).isEqualTo(26.1025);
    }

    @Test
    void latitudeMustStayWithinPlusOrMinusNinety() {
        assertThatThrownBy(() -> new GeoPoint(-90.1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(90.1, 0)).isInstanceOf(IllegalArgumentException.class);
        // The boundary itself is valid.
        assertThat(new GeoPoint(90, 0).latitude()).isEqualTo(90);
        assertThat(new GeoPoint(-90, 0).latitude()).isEqualTo(-90);
    }

    @Test
    void longitudeMustStayWithinPlusOrMinusOneEighty() {
        assertThatThrownBy(() -> new GeoPoint(0, -180.1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(0, 180.1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new GeoPoint(0, 180).longitude()).isEqualTo(180);
        assertThat(new GeoPoint(0, -180).longitude()).isEqualTo(-180);
    }
}

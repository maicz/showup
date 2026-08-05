package com.showup.api.util;

import com.showup.api.dto.GeoPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Converts between the DTO-facing {@link GeoPoint} and the JTS {@link Point} that Hibernate
 * Spatial maps {@code geography(Point, 4326)} columns to. Kept out of entities and DTOs so
 * neither has to depend on the other's shape.
 */
public final class GeoPoints {

    private static final int WGS84_SRID = 4326;
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    private GeoPoints() {
    }

    public static Point toJts(GeoPoint point) {
        if (point == null) {
            return null;
        }
        Point jts = FACTORY.createPoint(new Coordinate(point.longitude(), point.latitude()));
        jts.setSRID(WGS84_SRID);
        return jts;
    }

    public static GeoPoint fromJts(Point point) {
        return point == null ? null : new GeoPoint(point.getY(), point.getX());
    }
}

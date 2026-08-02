package com.showup.api.venue;

import com.showup.api.shared.GeoPoint;
import com.showup.api.shared.GeoPoints;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    @Mapping(target = "addressLine1", source = "address.addressLine1")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "region", source = "address.region")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "country", source = "address.country")
    @Mapping(target = "location", source = "location", qualifiedByName = "pointToGeoPoint")
    VenueSummary toSummary(Venue venue);

    @org.mapstruct.Named("pointToGeoPoint")
    default GeoPoint pointToGeoPoint(Point point) {
        return GeoPoints.fromJts(point);
    }

    @org.mapstruct.Named("geoPointToPoint")
    default Point geoPointToPoint(GeoPoint point) {
        return GeoPoints.toJts(point);
    }
}

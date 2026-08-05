package com.showup.api.mapper;

import com.showup.api.dto.GeoPoint;
import com.showup.api.dto.GroupDetail;
import com.showup.api.dto.GroupMemberSummary;
import com.showup.api.dto.GroupSummary;
import com.showup.api.dto.TopicSummary;
import com.showup.api.entity.Group;
import com.showup.api.entity.GroupMembership;
import com.showup.api.util.GeoPoints;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = TopicMapper.class)
public interface GroupMapper {

    GroupSummary toSummary(Group group);

    @Mapping(target = "location", source = "location", qualifiedByName = "pointToGeoPoint")
    @Mapping(target = "topics", ignore = true)
    GroupDetail toDetailWithoutTopics(Group group);

    default GroupDetail toDetail(Group group, List<TopicSummary> topics) {
        GroupDetail base = toDetailWithoutTopics(group);
        return new GroupDetail(
                base.id(), base.urlname(), base.name(), base.description(), base.category(),
                base.city(), base.country(), base.location(), base.timeZone(), base.visibility(),
                base.joinPolicy(), base.memberCount(), base.ratingAverage(), base.ratingCount(),
                base.foundedAt(), base.status(), topics);
    }

    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "displayName", source = "member.displayName")
    @Mapping(target = "photoUrl", source = "member.photoUrl")
    GroupMemberSummary toMemberSummary(GroupMembership membership);

    @Named("pointToGeoPoint")
    default GeoPoint pointToGeoPoint(Point point) {
        return GeoPoints.fromJts(point);
    }

    @Named("geoPointToPoint")
    default Point geoPointToPoint(GeoPoint point) {
        return GeoPoints.toJts(point);
    }
}

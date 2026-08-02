package com.showup.api.event;

import com.showup.api.group.GroupMapper;
import com.showup.api.venue.VenueMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {GroupMapper.class, VenueMapper.class})
public interface EventSeriesMapper {

    EventSeriesDetail toDetail(EventSeries series);
}

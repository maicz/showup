package com.showup.api.mapper;

import com.showup.api.dto.EventSeriesDetail;
import com.showup.api.entity.EventSeries;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {GroupMapper.class, VenueMapper.class})
public interface EventSeriesMapper {

    EventSeriesDetail toDetail(EventSeries series);
}

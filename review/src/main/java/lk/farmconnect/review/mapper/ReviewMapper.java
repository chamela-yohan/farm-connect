package lk.farmconnect.review.mapper;

import lk.farmconnect.review.dto.ReviewResponse;
import lk.farmconnect.review.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.name")
    ReviewResponse toResponse(Review review);
}
package lk.farmconnect.product.mapper;

import lk.farmconnect.product.dto.ProductResponse;
import lk.farmconnect.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "farmerId", source = "farmer.id")
    @Mapping(target = "farmerName", source = "farmer.name")
    //  Safely extract Lat/Lon from JTS Point without causing NullPointerExceptions
    @Mapping(target = "lat", expression = "java(product.getFarmer().getLocation() != null ? product.getFarmer().getLocation().getY() : null)")
    @Mapping(target = "lon", expression = "java(product.getFarmer().getLocation() != null ? product.getFarmer().getLocation().getX() : null)")
    ProductResponse toResponse(Product product);
}
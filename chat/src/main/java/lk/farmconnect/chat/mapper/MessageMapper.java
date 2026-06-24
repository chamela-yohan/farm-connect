package lk.farmconnect.chat.mapper;

import lk.farmconnect.chat.dto.MessageResponse;
import lk.farmconnect.chat.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.name")
    MessageResponse toResponse(Message message);
}
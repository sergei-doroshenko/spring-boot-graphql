package org.sdoroshenko.dataloader.repository;

import org.sdoroshenko.dataloader.model.Message;
import org.springframework.data.repository.CrudRepository;

public interface MessageRepository extends CrudRepository<Message, Long> {
}

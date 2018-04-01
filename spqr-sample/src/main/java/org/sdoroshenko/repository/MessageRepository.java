package org.sdoroshenko.repository;

import org.sdoroshenko.model.Message;
import org.springframework.data.repository.CrudRepository;

public interface MessageRepository extends CrudRepository<Message, Long> {
}

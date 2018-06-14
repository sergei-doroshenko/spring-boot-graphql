package org.sdoroshenko.spqr.repository;

import org.sdoroshenko.spqr.model.Message;
import org.springframework.data.repository.CrudRepository;

public interface MessageRepository extends CrudRepository<Message, Long> {
}

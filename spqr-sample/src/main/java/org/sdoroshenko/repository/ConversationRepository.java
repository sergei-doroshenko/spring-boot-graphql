package org.sdoroshenko.repository;

import org.sdoroshenko.model.Conversation;
import org.sdoroshenko.model.Message;
import org.springframework.data.repository.CrudRepository;

public interface ConversationRepository extends CrudRepository<Conversation, Long> {
}

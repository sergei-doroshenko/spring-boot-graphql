package org.sdoroshenko.spqr.repository;

import org.sdoroshenko.spqr.model.Conversation;
import org.springframework.data.repository.CrudRepository;

public interface ConversationRepository extends CrudRepository<Conversation, Long> {
}

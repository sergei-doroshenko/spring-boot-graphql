package org.sdoroshenko.dataloader.repository;

import org.sdoroshenko.dataloader.model.Conversation;
import org.springframework.data.repository.CrudRepository;

public interface ConversationRepository extends CrudRepository<Conversation, Long> {
}

package com.edulearn.discussion.repository;

import com.edulearn.discussion.entity.Message;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface MessageRepository extends MongoRepository<Message, String> {

    // Find all messages in a discussion (thread)
    List<Message> findByDiscussionId(String discussionId);

    // Sort: accepted first, then by upvotes, then by time
    List<Message> findByDiscussionIdOrderByIsAcceptedDescUpvoteCountDescCreatedAtAsc(String discussionId);

    // Find messages by author
    List<Message> findByAuthorId(String authorId);

    // Count messages in a discussion
    int countByDiscussionId(String discussionId);

    // Find accepted answer
    Optional<Message> findByDiscussionIdAndIsAcceptedTrue(String discussionId);
}
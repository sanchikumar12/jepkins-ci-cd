package com.edulearn.discussion.entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "messages")
@Data
public class Message {

    @Id
    private String id;

    @Indexed
    private String discussionId;

    private String authorId;

    private String body;

    // For nested replies
    @Indexed
    private String parentId; // null = top-level reply

    private boolean isAccepted;

    // 🔥 Replace UpvoteRecord table
    private List<String> upvotedUserIds;

    // Optional fast counter
    private int upvoteCount;

    private LocalDateTime createdAt;
}
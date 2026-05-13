package com.edulearn.discussion.entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@Data
@Document(collection = "discussions")
public class Discussion {

    @Id
    private String id;

    private String courseId;
    private String lessonId; // optional

    private String authorId;

    private String title;
    private String body;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional optimizations
    private int replyCount;
    private String acceptedReplyId;

}
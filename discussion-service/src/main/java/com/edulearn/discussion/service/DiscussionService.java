package com.edulearn.discussion.service;


import com.edulearn.discussion.entity.Discussion;
import com.edulearn.discussion.entity.Message;

import java.util.*;

public interface DiscussionService {
    // Thread operations
    Discussion createThread(Discussion thread);
    List<Discussion> getThreadsByCourse(String courseId);
  
    Optional<Discussion> getThreadById(Integer threadId);
  
    void deleteThread(Integer threadId);

    List<Discussion> getThreadsByAuthor(Integer authorId);
    int getThreadCount(Integer courseId);

    // Reply operations
    Message postReply(Message reply);
    List<Message> getRepliesByThread(String threadId);
    Optional<Message> getReplyById(Integer replyId);
   
    void deleteReply(Integer replyId);
    Message upvoteReply(Integer replyId, Integer studentId);
     void acceptReply(Integer replyId);
    void unacceptReply(Integer replyId);
    List<Message> getRepliesByAuthor(Integer authorId);
}


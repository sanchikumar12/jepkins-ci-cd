package com.edulearn.discussion.controller;

import com.edulearn.discussion.entity.Discussion;
import com.edulearn.discussion.entity.Message;
import com.edulearn.discussion.service.DiscussionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discussion")
@CrossOrigin(origins = "*") // Adjust based on your security needs
public class DiscussionController {

    @Autowired
    private DiscussionService discussionService;

    // ==================== THREAD ENDPOINTS ====================

    @PostMapping("/threads")
    public ResponseEntity<Discussion> createThread(@RequestBody Discussion thread) {
        return new ResponseEntity<>(discussionService.createThread(thread), HttpStatus.CREATED);
    }

    @GetMapping("/threads/course/{courseId}")
    public ResponseEntity<List<Discussion>> getThreadsByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(discussionService.getThreadsByCourse(Integer.toString(courseId)));
    }
    
    @GetMapping("/threads/test")
    public String get() {
    	return "Hellow";
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<Discussion> getThreadById(@PathVariable Integer threadId) {
        return discussionService.getThreadById(threadId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<Void> deleteThread(@PathVariable Integer threadId) {
        discussionService.deleteThread(threadId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/threads/author/{authorId}")
    public ResponseEntity<List<Discussion>> getThreadsByAuthor(@PathVariable Integer authorId) {
        return ResponseEntity.ok(discussionService.getThreadsByAuthor(authorId));
    }

    @GetMapping("/threads/count/{courseId}")
    public ResponseEntity<Integer> getThreadCount(@PathVariable Integer courseId) {
        return ResponseEntity.ok(discussionService.getThreadCount(courseId));
    }

    // ==================== MESSAGE (REPLY) ENDPOINTS ====================

    @PostMapping("/replies")
    public ResponseEntity<Message> postReply(@RequestBody Message reply) {
        return new ResponseEntity<>(discussionService.postReply(reply), HttpStatus.CREATED);
    }

    @GetMapping("/threads/{threadId}/replies")
    public ResponseEntity<List<Message>> getRepliesByThread(@PathVariable String threadId) {
        return ResponseEntity.ok(discussionService.getRepliesByThread(threadId));
    }

    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(@PathVariable Integer replyId) {
        discussionService.deleteReply(replyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/replies/{replyId}/upvote/{studentId}")
    public ResponseEntity<Message> upvoteReply(
            @PathVariable Integer replyId, 
            @PathVariable Integer studentId) {
        try {
            return ResponseEntity.ok(discussionService.upvoteReply(replyId, studentId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/replies/{replyId}/accept")
    public ResponseEntity<Void> acceptReply(@PathVariable Integer replyId) {
        discussionService.acceptReply(replyId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/replies/{replyId}/unaccept")
    public ResponseEntity<Void> unacceptReply(@PathVariable Integer replyId) {
        discussionService.unacceptReply(replyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/replies/author/{authorId}")
    public ResponseEntity<List<Message>> getRepliesByAuthor(@PathVariable Integer authorId) {
        return ResponseEntity.ok(discussionService.getRepliesByAuthor(authorId));
    }
}
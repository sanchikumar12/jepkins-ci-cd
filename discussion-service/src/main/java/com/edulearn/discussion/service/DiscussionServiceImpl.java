package com.edulearn.discussion.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.edulearn.discussion.entity.Discussion;
import com.edulearn.discussion.entity.Message;
import com.edulearn.discussion.repository.MessageRepository;
import com.edulearn.discussion.repository.ThreadRepository;

@Service
	public class DiscussionServiceImpl implements DiscussionService {

	    @Autowired
	    private MessageRepository messagerepo ;


	  
		@Override
		public Message postReply(Message reply) {
			// 1. Initialize MongoDB specific defaults
	        reply.setCreatedAt(LocalDateTime.now());
	        reply.setAccepted(false);
	        reply.setUpvoteCount(0);
	        reply.setUpvotedUserIds(new ArrayList<>());

	        Message savedReply = messagerepo.save(reply);

	       
	        discussionRepository.findById(reply.getDiscussionId()).ifPresent(discussion -> {
	            discussion.setReplyCount(discussion.getReplyCount() + 1);
	            discussion.setUpdatedAt(LocalDateTime.now());
	            discussionRepository.save(discussion);
	        });

	        return savedReply;
	    }
		

		@Override
		public List<Message> getRepliesByThread(String threadId) {
			
			return messagerepo.findByDiscussionIdOrderByIsAcceptedDescUpvoteCountDescCreatedAtAsc(threadId);
		}

		@Override
		public Optional<Message> getReplyById(Integer replyId) {
			
			return messagerepo.findById(String.valueOf(replyId));
		}

		@Override
		public void deleteReply(Integer replyId) {
			String idStr = String.valueOf(replyId);
	        
	        messagerepo.findById(idStr).ifPresent(reply -> {
	            // 1. Decrement the counter in the parent Discussion
	            discussionRepository.findById(reply.getDiscussionId()).ifPresent(discussion -> {
	                discussion.setReplyCount(Math.max(0, discussion.getReplyCount() - 1));
	                
	                // If this was the accepted reply, clear that reference in the Discussion
	                if (idStr.equals(discussion.getAcceptedReplyId())) {
	                    discussion.setAcceptedReplyId(null);
	                }
	                discussionRepository.save(discussion);
	            });

	            // 2. Remove the actual message document
	            messagerepo.deleteById(idStr);
	        });
			
		}

		@Override
		public Message upvoteReply(Integer replyId, Integer studentId) {
			Message message = messagerepo.findById(String.valueOf(replyId))
	                .orElseThrow(() -> new RuntimeException("Message not found"));

	        String userId = String.valueOf(studentId);

	        // check if user already upvoted using the internal List
	        if (message.getUpvotedUserIds() != null && message.getUpvotedUserIds().contains(userId)) {
	            throw new RuntimeException("You have already upvoted this reply");
	        }

	        if (message.getUpvotedUserIds() == null) {
	            message.setUpvotedUserIds(new ArrayList<>());
	        }

	        // Add user to list and update the fast counter
	        message.getUpvotedUserIds().add(userId);
	        message.setUpvoteCount(message.getUpvotedUserIds().size());

	        return messagerepo.save(message);
		}

		@Override
		public void acceptReply(Integer replyId) {
			Message reply = messagerepo.findById(String.valueOf(replyId))
	                .orElseThrow(() -> new RuntimeException("Reply not found"));

	        // 1. Reset any other accepted reply in the same thread
	        messagerepo.findByDiscussionIdAndIsAcceptedTrue(reply.getDiscussionId())
	                .ifPresent(oldAccepted -> {
	                    oldAccepted.setAccepted(false);
	                    messagerepo.save(oldAccepted);
	                });

	        // 2. Set this one as accepted
	        reply.setAccepted(true);
	        messagerepo.save(reply);

	        // 3. Update the Discussion document for quick status lookups
	        discussionRepository.findById(reply.getDiscussionId()).ifPresent(discussion -> {
	            discussion.setAcceptedReplyId(reply.getId());
	            discussionRepository.save(discussion);
	        });
			
		}

		@Override
		public void unacceptReply(Integer replyId) {
			messagerepo.findById(String.valueOf(replyId)).ifPresent(reply -> {
	            reply.setAccepted(false);
	            messagerepo.save(reply);

	            // Update the Discussion document reference
	            discussionRepository.findById(reply.getDiscussionId()).ifPresent(discussion -> {
	                if (reply.getId().equals(discussion.getAcceptedReplyId())) {
	                    discussion.setAcceptedReplyId(null);
	                    discussionRepository.save(discussion);
	                }
	            });
	        });
			
		}

		@Override
		public List<Message> getRepliesByAuthor(Integer authorId) {
			// TODO Auto-generated method stub
			return messagerepo.findByAuthorId(String.valueOf(authorId));
		}
		
		
		//TradRelated all methods ..............

		@Autowired
	    private ThreadRepository discussionRepository;

	    @Autowired
	    private MessageRepository messageRepository;

	    @Override
	    public Discussion createThread(Discussion thread) {
	        // 1. Core Data Fields (Passed from Controller)
	        // thread.getCourseId(), thread.getAuthorId(), thread.getTitle(), etc.

	        // 2. Metadata & Audit Initialization
	        LocalDateTime now = LocalDateTime.now();
	        thread.setCreatedAt(now);
	        thread.setUpdatedAt(now);

	        // 3. State Initialization
	        thread.setReplyCount(0);
	        thread.setAcceptedReplyId(null);

	        // 4. Persist to MongoDB
	        return discussionRepository.save(thread);
	    }

	    @Override
	    public List<Discussion> getThreadsByCourse(String courseId) {
	        // Convert Integer param to String to match the MongoDB Entity field
	        return discussionRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
	    }

	    @Override
	    public Optional<Discussion> getThreadById(Integer threadId) {
	        // Find using the String representation of the ID
	        return discussionRepository.findById(String.valueOf(threadId));
	    }

	    @Override
	    public void deleteThread(Integer threadId) {
	        String idStr = String.valueOf(threadId);

	        // 1. Manual Cascade: Delete all messages linked to this discussion
	        // Since MongoDB has no foreign keys, we use your MessageRepository here
	        List<Message> threadMessages = messageRepository.findByDiscussionId(idStr);
	        messageRepository.deleteAll(threadMessages);

	        // 2. Delete the discussion document
	        discussionRepository.deleteById(idStr);
	    }

	    @Override
	    public List<Discussion> getThreadsByAuthor(Integer authorId) {
	        // Assuming your DiscussionRepository has findByAuthorId
	        return discussionRepository.findByAuthorId(authorId);
	    }

	    @Override
	    public int getThreadCount(Integer courseId) {
	        // Useful for dashboard/stats
	        return (int) discussionRepository.countByCourseId(courseId);
	    }

	
}


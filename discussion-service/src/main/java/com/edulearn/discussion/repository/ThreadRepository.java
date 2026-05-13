package com.edulearn.discussion.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.edulearn.discussion.entity.Discussion;

import java.util.List;

@Repository 
public interface ThreadRepository extends MongoRepository<Discussion, String> {

	List<Discussion> findByCourseId(Integer courseId);

	// Parameter must match the Entity field type (String)
    List<Discussion> findByCourseIdOrderByCreatedAtDesc(String courseId);

    List<Discussion> findByAuthorId(Integer authorId);

    // If you need the count for a specific course
    long countByCourseId(Integer courseId);
}
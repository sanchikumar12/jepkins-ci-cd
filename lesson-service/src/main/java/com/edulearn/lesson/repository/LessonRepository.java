package com.edulearn.lesson.repository;



import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.edulearn.lesson.entity.Lesson;

@Repository
public interface LessonRepository extends MongoRepository<Lesson, String> {

    // Matches 'private String courseId'
    List<Lesson> findByCourseId(Integer courseId);

    // Matches 'private String courseId' and 'private Integer orderIndex'
    List<Lesson> findByCourseIdOrderByOrderIndex(Integer courseId);

    // Matches 'private Boolean isPreview'
    List<Lesson> findByIsPreview(Boolean isPreview);

    // Matches 'private String courseId'
    int countByCourseId(Integer courseId);
}


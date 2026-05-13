package com.edulearn.lesson.service;



import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import java.util.List;

public interface LessonService {
    Lesson addLesson(Lesson lesson);

    List<Lesson> getLessonsByCourse(Integer courseId);

    Lesson getLessonById(String lessonId, Integer studentId);

    Lesson updateLesson(String lessonId, Lesson lesson);

    void deleteLesson(String lessonId);

    void reorderLessons(Integer courseId, List<String> lessonIds);

    List<Lesson> getPreviewLessons(Integer courseId);

    // --- MONGODB SPECIFIC ADJUSTMENTS ---
    
    // We need to know which lesson to add the resource to
    Resource addResource(String lessonId, Resource resource);

    // We now use the lessonId and a unique identifier (like resource name or URL) 
    // because resources are no longer in their own table with a unique DB ID
    void removeResource(String lessonId, String resourceName); 
}
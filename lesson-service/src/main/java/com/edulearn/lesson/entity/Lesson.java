package com.edulearn.lesson.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lessons")
public class Lesson {

    @Id
    private String lessonId; // Primary Key

    @Field("course_id")
    private Integer courseId;

    private String title;

    // A Lesson can have multiple videos, each with its own resources
    private List<Video> video; 

    @Field("duration_minutes")
    private Integer durationMinutes;

    @Field("order_index")
    private Integer orderIndex;

    private String description;

    @Field("is_preview")
    private Boolean isPreview = false;
}
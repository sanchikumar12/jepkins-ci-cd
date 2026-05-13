package com.edulearn.lesson.entity;




import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    private String title;

    @Field("vid_url")
    private String vidUrl;

    @Field("duration_seconds")
    private Integer durationSeconds;

    // Resources are now nested inside the Video object
    private List<Resource> resources;
}
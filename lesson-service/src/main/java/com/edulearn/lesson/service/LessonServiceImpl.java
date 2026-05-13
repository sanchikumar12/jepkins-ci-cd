package com.edulearn.lesson.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import com.edulearn.lesson.entity.Video;
import com.edulearn.lesson.repository.LessonRepository;

@Service
public class LessonServiceImpl implements LessonService {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${enrollment.service.url:http://localhost:8084/api/v1}")
    private String enrollmentServiceUrl;

    @Override
    public Lesson addLesson(Lesson lesson) {
        // Initialize video list if it's null to avoid NullPointerException
        if (lesson.getVideo() == null) {
            lesson.setVideo(new ArrayList<>());
        }
        return lessonRepository.save(lesson);
    }

    @Override
    public List<Lesson> getLessonsByCourse(Integer courseId) {
        return lessonRepository.findByCourseIdOrderByOrderIndex(courseId);
    }

    @Override
    public Lesson getLessonById(String lessonId, Integer studentId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found with ID: " + lessonId));

        if (!lesson.getIsPreview()) {
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            boolean isEnrolled = checkEnrollmentViaRest(studentId, lesson.getCourseId(), token);

            if (!isEnrolled) {
                throw new AccessDeniedException("Please enroll to access this lesson");
            }
        }
        return lesson;
    }

   

 
    @Override
    public void reorderLessons(Integer courseId, List<String> lessonIds) {
        for (int i = 0; i < lessonIds.size(); i++) {
            String id = lessonIds.get(i);
            Lesson lesson = lessonRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Lesson not found with ID: " + id));
            lesson.setOrderIndex(i);
            lessonRepository.save(lesson);
        }
    }

   

   

    @Override
    public List<Lesson> getPreviewLessons(Integer courseId) {
        return lessonRepository.findByCourseIdOrderByOrderIndex(courseId)
                .stream()
                .filter(Lesson::getIsPreview)
                .toList();
    }

    private boolean checkEnrollmentViaRest(Integer studentId, Integer courseId, String token) {
        try {
            String url = enrollmentServiceUrl + "/enrollments/check?studentId=" + studentId + "&courseId=" + courseId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            var response = restTemplate.getForEntity(url, EnrollmentCheckResponse.class, entity);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getData();
        } catch (Exception e) {
            return false;
        }
    }

    private static class EnrollmentCheckResponse {
        private boolean data;
        public boolean getData() { return data; }
        public void setData(boolean data) { this.data = data; }
    }

	@Override
	public Lesson updateLesson(String lessonId, Lesson lesson) {
	    Lesson existingLesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found with ID: " + lessonId));

        existingLesson.setTitle(lesson.getTitle());
        existingLesson.setDescription(lesson.getDescription());
        existingLesson.setDurationMinutes(lesson.getDurationMinutes());
        existingLesson.setIsPreview(lesson.getIsPreview());
        existingLesson.setOrderIndex(lesson.getOrderIndex());
        
        // In Mongo, we often update the whole nested list at once
        if (lesson.getVideo() != null) {
            existingLesson.setVideo(lesson.getVideo());
        }

        return lessonRepository.save(existingLesson);
	}

	@Override
	public void deleteLesson(String lessonId) {
		// TODO Auto-generated method stub
		 lessonRepository.deleteById(lessonId);
	}

	@Override
	public Resource addResource(String lessonId, Resource resource) {
		 Lesson lesson = lessonRepository.findById(lessonId)
	                .orElseThrow(() -> new RuntimeException("Lesson not found"));

	        // Assuming resources belong to the FIRST video in the list for this logic
	        // Or you can modify your logic to target a specific video title/index
	        if (lesson.getVideo() != null && !lesson.getVideo().isEmpty()) {
	            Video firstVideo = lesson.getVideo().get(0);
	            if (firstVideo.getResources() == null) {
	                firstVideo.setResources(new ArrayList<>());
	            }
	            firstVideo.getResources().add(resource);
	            lessonRepository.save(lesson);
	        } else {
	            throw new RuntimeException("Cannot add resource: No video exists for this lesson yet.");
	        }
	        return resource;
	}

	@Override
	public void removeResource(String lessonId, String resourceName) {
		 Lesson lesson = lessonRepository.findById(lessonId)
	                .orElseThrow(() -> new RuntimeException("Lesson not found"));

	        // Traverse nested lists to remove resource by name
	        if (lesson.getVideo() != null) {
	            lesson.getVideo().forEach(v -> {
	                if (v.getResources() != null) {
	                    v.getResources().removeIf(r -> r.getName().equalsIgnoreCase(resourceName));
	                }
	            });
	            lessonRepository.save(lesson);
	        }
		
	}
}
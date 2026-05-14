package com.edulearn.lesson.controller;

import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import com.edulearn.lesson.entity.Video;
import com.edulearn.lesson.service.LessonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Lesson Controller Tests")
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LessonService lessonService;

    @Autowired
    private ObjectMapper objectMapper;

    private Lesson testLesson;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        testResource = new Resource("Java Cheat Sheet", "PDF", "https://example.com/cheatsheet.pdf");
        testLesson = new Lesson(
                "lesson-1",
                5,
                "Java Basics",
                List.of(new Video("Intro", "https://example.com/video.mp4", 1800, List.of(testResource))),
                30,
                0,
                "Learn Java fundamentals",
                false
        );
    }

    @Test
    @DisplayName("Should create lesson")
    void testAddLesson() throws Exception {
        when(lessonService.addLesson(any(Lesson.class))).thenReturn(testLesson);

        mockMvc.perform(post("/api/v1/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLesson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lesson created successfully"))
                .andExpect(jsonPath("$.data.lessonId").value("lesson-1"))
                .andExpect(jsonPath("$.data.title").value("Java Basics"));

        verify(lessonService, times(1)).addLesson(any(Lesson.class));
    }

    @Test
    @DisplayName("Should get lessons by course")
    void testGetLessonsByCourse() throws Exception {
        when(lessonService.getLessonsByCourse(5)).thenReturn(List.of(testLesson));

        mockMvc.perform(get("/api/v1/lessons/course/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Java Basics"));

        verify(lessonService, times(1)).getLessonsByCourse(5);
    }

    @Test
    @DisplayName("Should get lesson by ID")
    void testGetLessonById() throws Exception {
        when(lessonService.getLessonById("lesson-1", 1)).thenReturn(testLesson);

        mockMvc.perform(get("/api/v1/lessons/lesson-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lessonId").value("lesson-1"));

        verify(lessonService, times(1)).getLessonById("lesson-1", 1);
    }

    @Test
    @DisplayName("Should update lesson")
    void testUpdateLesson() throws Exception {
        Lesson updatedLesson = new Lesson("lesson-1", 5, "Updated Title", List.of(), 45, 0, "Updated", false);
        when(lessonService.updateLesson(eq("lesson-1"), any(Lesson.class))).thenReturn(updatedLesson);

        mockMvc.perform(put("/api/v1/lessons/lesson-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedLesson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lesson updated successfully"))
                .andExpect(jsonPath("$.data.title").value("Updated Title"));

        verify(lessonService, times(1)).updateLesson(eq("lesson-1"), any(Lesson.class));
    }

    @Test
    @DisplayName("Should delete lesson")
    void testDeleteLesson() throws Exception {
        doNothing().when(lessonService).deleteLesson("lesson-1");

        mockMvc.perform(delete("/api/v1/lessons/lesson-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lesson deleted successfully"));

        verify(lessonService, times(1)).deleteLesson("lesson-1");
    }

    @Test
    @DisplayName("Should reorder lessons")
    void testReorderLessons() throws Exception {
        List<String> lessonIds = List.of("lesson-3", "lesson-1", "lesson-2");
        doNothing().when(lessonService).reorderLessons(5, lessonIds);

        mockMvc.perform(put("/api/v1/lessons/reorder/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lessons reordered successfully"));

        verify(lessonService, times(1)).reorderLessons(5, lessonIds);
    }

    @Test
    @DisplayName("Should add resource")
    void testAddResource() throws Exception {
        when(lessonService.addResource(eq("lesson-1"), any(Resource.class))).thenReturn(testResource);

        mockMvc.perform(post("/api/v1/lessons/lesson-1/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testResource)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Resource added successfully"))
                .andExpect(jsonPath("$.data.name").value("Java Cheat Sheet"));

        verify(lessonService, times(1)).addResource(eq("lesson-1"), any(Resource.class));
    }

    @Test
    @DisplayName("Should remove resource")
    void testRemoveResource() throws Exception {
        doNothing().when(lessonService).removeResource("lesson-1", "Java Cheat Sheet");

        mockMvc.perform(delete("/api/v1/lessons/lesson-1/resources/Java Cheat Sheet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Resource removed successfully"));

        verify(lessonService, times(1)).removeResource("lesson-1", "Java Cheat Sheet");
    }

    @Test
    @DisplayName("Should get preview lessons")
    void testGetPreviewLessons() throws Exception {
        Lesson previewLesson = new Lesson("lesson-2", 5, "Preview", List.of(), 15, 1, "Preview", true);
        when(lessonService.getPreviewLessons(5)).thenReturn(List.of(previewLesson));

        mockMvc.perform(get("/api/v1/lessons/preview/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Preview"));

        verify(lessonService, times(1)).getPreviewLessons(5);
    }
}

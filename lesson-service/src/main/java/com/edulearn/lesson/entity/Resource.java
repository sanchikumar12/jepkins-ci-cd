package com.edulearn.lesson.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    private String name;
    private String type; // e.g., "PDF", "ZIP", "Link"
    private String url;
}
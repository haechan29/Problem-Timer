package kr.co.problemtimertest.model;

import androidx.annotation.Nullable;

public class Book {

    // 필드
    private Long id;
    private Integer school;
    private Integer subject;
    private String name;

    // 생성자
    public Book(Long id, Integer school, Integer subject, String name) {
        this.id = id;
        this.school = school;
        this.subject = subject;
        this.name = name;
    }

    // equals()
    @Override
    public boolean equals(@Nullable Object obj) {

        if (obj instanceof Book) {

            return id.equals(((Book) obj).id)
                    && school.equals(((Book) obj).school)
                    && subject.equals(((Book) obj).subject)
                    && name.equals(((Book) obj).name);
        }

        return false;
    }

    // 게터
    public Long getId() {
        return id;
    }

    public Integer getSchool() {
        return school;
    }

    public Integer getSubject() {
        return subject;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", school=" + school +
                ", subject=" + subject +
                ", name='" + name + '\'' +
                '}';
    }
}
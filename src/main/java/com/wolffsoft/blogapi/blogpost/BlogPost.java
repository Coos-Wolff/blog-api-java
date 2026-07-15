package com.wolffsoft.blogapi.blogpost;

import com.wolffsoft.blogapi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "blog_post")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlogPost {

    @Id
    @Generated(event = EventType.INSERT)
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 250, unique = true)
    private String title;

    @Column(name = "subtitle", nullable = false, length = 250)
    private String subtitle;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "body", columnDefinition = "text", nullable = false)
    private String body;

    @Column(name = "img_url", nullable = false, length = 250)
    private String imgUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlogPost blogPost)) {
            return false;
        }
        return id != null && id.equals(blogPost.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

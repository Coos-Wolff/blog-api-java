package com.wolffsoft.blogapi.user;

import com.wolffsoft.blogapi.blogpost.BlogPost;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.generator.EventType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Generated(event = EventType.INSERT)
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "password", nullable = false)
    private String password;

    @Generated(event = EventType.INSERT)
    @ColumnDefault("false")
    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter(AccessLevel.NONE)
    private Set<BlogPost> blogPosts = new HashSet<>();

    public void addBlogPost(BlogPost blogPost) {
        blogPosts.add(blogPost);
        blogPost.setAuthor(this);
    }

    public void removeBlogPost(BlogPost blogPost) {
        blogPosts.remove(blogPost);
        blogPost.setAuthor(null);
    }

    public static User create(String email, String name, String hashedPassword) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(hashedPassword);
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

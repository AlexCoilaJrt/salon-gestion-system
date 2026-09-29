package pe.com.salon.salongestionapi.auth.service.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RegisterResponse {

    public RegisterResponse() {
    }

    public RegisterResponse(Long id, String username, String email, String firstName, String lastName,
            Set<String> roles, String message) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.message = message;
    }

    public static class RegisterResponseBuilder {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private Set<String> roles;
        private String message;

        public RegisterResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RegisterResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public RegisterResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public RegisterResponseBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public RegisterResponseBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public RegisterResponseBuilder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public RegisterResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public RegisterResponse build() {
            return new RegisterResponse(id, username, email, firstName, lastName, roles, message);
        }
    }

    public static RegisterResponseBuilder builder() {
        return new RegisterResponseBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private String message;
}

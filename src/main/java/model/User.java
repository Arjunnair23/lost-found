package model;

public class User {
    private String id, username, email, role, passwordHash;
    public User() {}
    public User(String id, String username, String email, String role, String passwordHash) { this.id=id; this.username=username; this.email=email; this.role=role; this.passwordHash=passwordHash; }
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
}
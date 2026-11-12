package repository;
import model.User; import storage.JsonStore; import java.util.*;
public class JsonUserRepository implements UserRepository {
 private static final Object REGISTRATION_LOCK=new Object(); private final JsonStore store; public JsonUserRepository(){this(new JsonStore());} public JsonUserRepository(JsonStore s){store=s;}
 public Optional<User> findByUsername(String n){return store.users().stream().filter(u->u.getUsername().equalsIgnoreCase(n)).findFirst();}
 public Optional<User> findById(String id){return store.users().stream().filter(u->u.getId().equals(id)).findFirst();}
 public User create(User user){synchronized(REGISTRATION_LOCK){List<User> all=store.users();if(all.stream().anyMatch(u->u.getUsername().equalsIgnoreCase(user.getUsername())))throw new IllegalArgumentException("Duplicate username");all.add(user);store.saveUsers(all);return user;}}
}
package repository;
import model.User; import java.util.*;
public interface UserRepository { Optional<User> findByUsername(String username); Optional<User> findById(String id); User create(User user); }
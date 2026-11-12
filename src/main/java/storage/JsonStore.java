package storage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.*;
import java.io.*; import java.nio.channels.*; import java.nio.file.*; import java.util.*;

/** Single-instance JSON storage. Use a transactional database for multi-instance deployments. */
public final class JsonStore {
 private static final Object LOCK=new Object(); private static final ObjectMapper M=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
 private final Path dir;
 public JsonStore(){this(resolveDirectory());}
 private static Path resolveDirectory(){String configured=System.getenv("LOST_FOUND_DATA_DIR"); return (configured==null||configured.isBlank()?Path.of(System.getProperty("user.home"),".campus-lost-found"):Path.of(configured)).toAbsolutePath().normalize();}
 public JsonStore(Path directory){dir=directory.toAbsolutePath().normalize();try{Files.createDirectories(dir);}catch(IOException e){throw new IllegalStateException("Cannot initialize JSON storage",e);}}
 public Path directory(){return dir;}
 public List<User> users(){return read("users.json",new TypeReference<List<User>>(){});}
 public List<Report> lost(){return read("lost_items.json",new TypeReference<List<Report>>(){});}
 public List<Report> found(){return read("found_items.json",new TypeReference<List<Report>>(){});}
 public void saveUsers(List<User> v){validateUsers(v);write("users.json",v);}
 public void saveLost(List<Report> v){validateReports(v,"LOST");write("lost_items.json",v);}
 public void saveFound(List<Report> v){validateReports(v,"FOUND");write("found_items.json",v);}
 private <T> List<T> read(String name,TypeReference<List<T>> type){synchronized(LOCK){Path p=dir.resolve(name), bak=dir.resolve(name+".bak"); if(!Files.exists(p)){if(Files.exists(bak))return recover(p,bak,type);write(name,new ArrayList<>());return new ArrayList<>();} try{return M.readValue(p.toFile(),type);}catch(Exception e){if(Files.exists(bak))return recover(p,bak,type);throw new IllegalStateException("Malformed JSON in "+p+"; data was preserved",e);}}}
 private <T> List<T> recover(Path p,Path bak,TypeReference<List<T>> type){try{List<T> vals=M.readValue(bak.toFile(),type);Files.copy(bak,p,StandardCopyOption.REPLACE_EXISTING);return vals;}catch(Exception e){throw new IllegalStateException("Both JSON data and backup are invalid; data was preserved",e);}}
 private void write(String name,Object value){synchronized(LOCK){Path lockPath=dir.resolve(".storage.lock"), target=dir.resolve(name), temp=dir.resolve(name+".tmp"); try(FileChannel ch=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE); FileLock ignored=ch.lock()){byte[] bytes=M.writerWithDefaultPrettyPrinter().writeValueAsBytes(value); Files.write(temp,bytes,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE); if(Files.exists(target))Files.copy(target,dir.resolve(name+".bak"),StandardCopyOption.REPLACE_EXISTING); try{Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}catch(IOException e){try{Files.deleteIfExists(temp);}catch(IOException ignored){} throw new IllegalStateException("Unable to save JSON data",e);}}}
 private void validateUsers(List<User> v){Set<String>s=new HashSet<>();for(User u:v){if(u==null||u.getId()==null||u.getUsername()==null||u.getPasswordHash()==null||!s.add(u.getUsername().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Invalid or duplicate user data");}}
 private void validateReports(List<Report> v,String expected){Set<String>s=new HashSet<>();for(Report r:v){if(r==null||r.getId()==null||r.getOwnerId()==null||!expected.equals(r.getType())||r.getTitle()==null||r.getCategory()==null||r.getLocation()==null||!s.add(r.getId()))throw new IllegalArgumentException("Invalid report data");}}
}
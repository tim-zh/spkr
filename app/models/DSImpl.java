package models;

import com.mongodb.MongoClient;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.Mapper;

import java.util.Collections;

public class DSImpl extends DatastoreImpl {
  public DSImpl(Morphia morphia, MongoClient mongoClient, String dbName) {
    super(morphia, mongoClient, dbName);
  }

  public DSImpl(Morphia morphia, Mapper mapper, MongoClient mongoClient, String dbName) {
    super(morphia, mapper, mongoClient, dbName);
  }

  @SafeVarargs
  public final <T> Iterable<Key<T>> persist(T... entities) {
    if (entities.length > 1)
      return super.save(entities);
    else
      return Collections.singletonList(super.save(entities[0]));
  }
}

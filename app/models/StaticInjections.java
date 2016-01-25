package models;

import com.google.inject.Inject;

import javax.inject.Named;
import java.text.SimpleDateFormat;

public class StaticInjections {
  @Inject
  public static Dao dao;
  @Inject
  @Named("default")
  public static SimpleDateFormat dateFormat;
}

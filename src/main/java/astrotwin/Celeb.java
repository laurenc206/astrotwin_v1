package astrotwin;

import java.time.LocalDateTime;

public class Celeb extends Person{
    String accuracy;
    String bio;
    String picture;

    public Celeb(String name, LocalDateTime birthDateTime, String town, String country, String accuracy) throws Exception {
        super(name, birthDateTime, town, country);
        this.accuracy = accuracy;

    }

    public Celeb(String name, LocalDateTime birthDateTime, AtlasModel location, String accuracy) throws Exception {
        super(name, birthDateTime, location);
        this.accuracy = accuracy;

    }   
}

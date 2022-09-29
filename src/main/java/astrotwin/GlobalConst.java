package astrotwin;

import java.util.*;


public class GlobalConst {
    public static final int CURRENT_YEAR = 2022;
    public static final boolean USE_NETWORK = true;
    public static final String CHART_URL = "https://alabe.com/cgi-bin/chart/astrobot.cgi?INPUT1=&INPUT2=&GENDER=%s&MONTH=%s&DAY=%s&YEAR=%s&HOUR=%s&MINUTE=%s&AMPM=%s&TOWN=%s&COUNTRY=%s&STATE=%s&INPUT9=&Submit=Submit";

    public static final String CELEB_CHART = "https://www.astro.com/astro-databank/%s";

    public static final int CHART_LENGTH = 69;

    public static final int NUM_SIGN = 12;
    public static final int NUM_ELEM = 4;
    public static final int NUM_MODE = 3;
    
    public static final int CHART_NODE_IDX = 13;
    public static final int NORTH_IDX = 10;
    public static final int ASC_IDX = 11;

    public static final int PLANET_IDX = 26;
    public static final int ZODIAC_IDX = 42;
    public static final int ELEM_IDX = 42;
    public static final int MODE_IDX = 48;

    public static final List<String> MONTHS = new ArrayList<>(Arrays.asList("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"));

    // for windows =
    // public static final String ASTROLOG_FPATH = "\\src\\main\\java\\astrotwin\\Astrolog\\astrolog.exe";
    // for mac = 
    public static final String ASTROLOG_FPATH = "/src/main/java/astrotwin/Astrolog/astrolog";
    
    public static final String DEFAULT_NAME = "default";

    public static final double ZODIAC_MULT = 1;
    public static final double ELEMENT_MULT = .5;
    public static final double MODE_MULT = .5;
    public static final double HOUSE_MULT = 2;
    
    public static final int PROMPT_LEN = 25;
    public static final int MULT_COL_LEN = 30;

    public static final List<String> MATCH_TYPES = new ArrayList<>(Arrays.asList("Zodiac", "Element", "Mode"));

    private GlobalConst() {
        throw new IllegalStateException("GlobalConstant class");
    }


}

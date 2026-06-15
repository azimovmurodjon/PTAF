package com.ptaf.ui.mobilebrowser;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Resolves tester-friendly mobile-browser profile names from YAML. */
public final class MobileBrowserProfileRepository {
    private static final String ROOT = "mobile_browser_profiles";
    private MobileBrowserProfileRepository() { throw new IllegalStateException("Utility class"); }
    public static boolean isMobileBrowserProfile(String profileName) { return findByName(profileName).isPresent(); }
    public static Optional<MobileBrowserProfile> findByName(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) return Optional.empty();
        Map<String, Object> profiles = MobileBrowserYamlReader.getMap(ROOT); String target = normalize(profileName);
        for (Map.Entry<String, Object> e : profiles.entrySet()) if (normalize(e.getKey()).equals(target) && e.getValue() instanceof Map) return Optional.of(fromYaml(e.getKey(), cast(e.getValue())));
        return Optional.empty();
    }
    public static Map<String, MobileBrowserProfile> getAllProfiles() { Map<String, MobileBrowserProfile> r = new LinkedHashMap<>(); for (Map.Entry<String, Object> e : MobileBrowserYamlReader.getMap(ROOT).entrySet()) if (e.getValue() instanceof Map) r.put(e.getKey(), fromYaml(e.getKey(), cast(e.getValue()))); return r; }
    @SuppressWarnings("unchecked") private static Map<String, Object> cast(Object v) { return (Map<String, Object>) v; }
    private static MobileBrowserProfile fromYaml(String name, Map<String, Object> d) {
        int vw = intVal(d, "viewport_width", 390), vh = intVal(d, "viewport_height", 844);
        return new MobileBrowserProfile(name, str(d,"browser_engine","chromium"), vw, vh, intVal(d,"screen_width",vw), intVal(d,"screen_height",vh), dbl(d,"device_scale_factor",3.0), bool(d,"is_mobile",true), bool(d,"has_touch",true), str(d,"user_agent",""), str(d,"platform","Mobile"), str(d,"device_category","phone"), str(d,"orientation","portrait"));
    }
    private static String normalize(String v) { return v == null ? "" : v.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
    private static String str(Map<String,Object> m,String k,String d){Object v=m.get(k);return v==null?d:String.valueOf(v);} private static int intVal(Map<String,Object>m,String k,int d){try{return m.get(k)==null?d:Integer.parseInt(String.valueOf(m.get(k)));}catch(Exception e){return d;}} private static double dbl(Map<String,Object>m,String k,double d){try{return m.get(k)==null?d:Double.parseDouble(String.valueOf(m.get(k)));}catch(Exception e){return d;}} private static boolean bool(Map<String,Object>m,String k,boolean d){Object v=m.get(k);return v==null?d:Boolean.parseBoolean(String.valueOf(v));}
}

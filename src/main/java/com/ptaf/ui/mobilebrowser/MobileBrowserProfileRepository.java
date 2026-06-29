package com.ptaf.ui.mobilebrowser;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Utility repository that reads mobile browser profile definitions from a YAML source
 * and exposes them as tester-friendly {@link MobileBrowserProfile} objects.
 *
 * <p>Profiles are read from a YAML map under the top-level key "mobile_browser_profiles".
 * Each profile must be a map of properties (viewport_width, viewport_height, is_mobile, etc.).
 *
 * <p>This class is a pure utility holder (private constructor) with static lookup methods:
 * - isMobileBrowserProfile(name): quick boolean existence check (case/whitespace insensitive)
 * - findByName(name): returns an Optional with a parsed MobileBrowserProfile when found
 * - getAllProfiles(): returns a map of all profiles keyed by their YAML names
 *
 * <p>Important notes for testers:
 * - Lookups are normalized: case is ignored, leading/trailing whitespace collapsed,
 *   and internal whitespace sequences are reduced to a single space.
 * - Missing or malformed numeric/boolean values in the YAML are tolerated and
 *   replaced with sensible defaults (see {@link #fromYaml} for the specific defaults).
 */
public final class MobileBrowserProfileRepository {
    /**
     * Root key in the YAML file where mobile browser profiles are defined.
     * The YAML reader is expected to return a Map keyed by profile names.
     */
    private static final String ROOT = "mobile_browser_profiles";

    /**
     * Private constructor to prevent instantiation of this utility class.
     * Throws IllegalStateException if invoked through reflection or other means.
     */
    private MobileBrowserProfileRepository() { throw new IllegalStateException("Utility class"); }

    /**
     * Checks whether the given profile name corresponds to a defined mobile browser profile.
     *
     * <p>This method performs a normalized name lookup (case-insensitive, collapses whitespace).
     *
     * @param profileName the human-friendly profile name to check; may be null or blank
     * @return true if a corresponding profile exists in the YAML, false otherwise
     */
    public static boolean isMobileBrowserProfile(String profileName) { return findByName(profileName).isPresent(); }

    /**
     * Finds a mobile browser profile by name and, if present, converts it into a
     * {@link MobileBrowserProfile} object.
     *
     * <p>The lookup is robust:
     * - Null or empty profile names immediately return Optional.empty().
     * - Profile names are normalized before comparison (case and whitespace insensitive).
     * - Only entries that map to nested YAML maps are considered valid profiles.
     *
     * <p>For testers: call this method when you need the full parsed profile (viewport,
     * user agent, device category, etc.). If the profile is missing or malformed, an empty
     * Optional is returned rather than throwing an exception.
     *
     * @param profileName human-friendly profile name to find
     * @return Optional containing the parsed MobileBrowserProfile when found; otherwise Optional.empty()
     */
    public static Optional<MobileBrowserProfile> findByName(String profileName) {
        // Guard against null/empty input early.
        if (profileName == null || profileName.trim().isEmpty()) return Optional.empty();

        // Retrieve raw profiles map from YAML. Keys are profile names; values should be maps.
        Map<String, Object> profiles = MobileBrowserYamlReader.getMap(ROOT); String target = normalize(profileName);
        // Iterate through YAML entries and compare normalized keys to the normalized target.
        for (Map.Entry<String, Object> e : profiles.entrySet()) if (normalize(e.getKey()).equals(target) && e.getValue() instanceof Map) return Optional.of(fromYaml(e.getKey(), cast(e.getValue())));
        // No matching profile found.
        return Optional.empty();
    }

    /**
     * Reads all mobile browser profiles from the YAML and returns them as a LinkedHashMap
     * preserving the YAML iteration order.
     *
     * <p>Only entries whose values are maps are converted into {@link MobileBrowserProfile}.
     * Other entries in the YAML under the same key are ignored.
     *
     * @return map of profile name -> MobileBrowserProfile for all valid profiles
     */
    public static Map<String, MobileBrowserProfile> getAllProfiles() { Map<String, MobileBrowserProfile> r = new LinkedHashMap<>(); for (Map.Entry<String, Object> e : MobileBrowserYamlReader.getMap(ROOT).entrySet()) if (e.getValue() instanceof Map) r.put(e.getKey(), fromYaml(e.getKey(), cast(e.getValue()))); return r; }

    /**
     * Helper unchecked cast from Object to Map<String, Object>. The code ensures the value
     * is a Map before calling this method, so this suppresses the unchecked warning.
     *
     * @param v object expected to be a map
     * @return the object cast to Map<String, Object>
     */
    @SuppressWarnings("unchecked") private static Map<String, Object> cast(Object v) { return (Map<String, Object>) v; }

    /**
     * Construct a MobileBrowserProfile from a YAML map of properties.
     *
     * <p>When reading values from the YAML map, this method supplies reasonable defaults
     * for missing or malformed properties. This behavior ensures the test suite or UI
     * will not crash due to minor YAML issues.
     *
     * <p>Default values used:
     * - viewport_width: 390
     * - viewport_height: 844
     * - screen_width: defaults to viewport_width
     * - screen_height: defaults to viewport_height
     * - device_scale_factor: 3.0
     * - is_mobile: true
     * - has_touch: true
     * - browser_engine: "chromium"
     * - user_agent: "" (empty string)
     * - platform: "Mobile"
     * - device_category: "phone"
     * - orientation: "portrait"
     *
     * @param name YAML profile key (kept as the profile's name)
     * @param d map of YAML properties for the profile
     * @return new MobileBrowserProfile populated from YAML values or defaults
     */
    private static MobileBrowserProfile fromYaml(String name, Map<String, Object> d) {
        // Read viewport dimensions with defaults.
        int vw = intVal(d, "viewport_width", 390), vh = intVal(d, "viewport_height", 844);
        // Build MobileBrowserProfile with safe fallbacks for each expected property.
        return new MobileBrowserProfile(name, str(d,"browser_engine","chromium"), vw, vh, intVal(d,"screen_width",vw), intVal(d,"screen_height",vh), dbl(d,"device_scale_factor",3.0), bool(d,"is_mobile",true), bool(d,"has_touch",true), str(d,"user_agent",""), str(d,"platform","Mobile"), str(d,"device_category","phone"), str(d,"orientation","portrait"));
    }

    /**
     * Normalizes a profile name for robust matching:
     * - trims leading/trailing whitespace
     * - collapses any sequence of whitespace characters into a single space
     * - converts to lower-case using Locale.ROOT
     *
     * <p>This ensures lookups are human-friendly: "  iPhone 12  Pro " and "iphone 12 pro"
     * will match the same profile.
     *
     * @param v input string to normalize (may be null)
     * @return normalized string (never null)
     */
    private static String normalize(String v) { return v == null ? "" : v.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }

    // Utility accessors with defaults and safe parsing.
    // Each of these reads a raw value from the YAML map and attempts to convert it to the target type.
    // If the key is missing or conversion fails, the supplied default is returned.

    /**
     * Reads a string property from the map, returning the default if not present.
     */
    private static String str(Map<String,Object> m,String k,String d){Object v=m.get(k);return v==null?d:String.valueOf(v);}

    /**
     * Reads an int property from the map. If the value is missing or cannot be parsed as an int,
     * the provided default is returned.
     */
    private static int intVal(Map<String,Object>m,String k,int d){try{return m.get(k)==null?d:Integer.parseInt(String.valueOf(m.get(k)));}catch(Exception e){return d;}}

    /**
     * Reads a double property from the map. If the value is missing or cannot be parsed as a double,
     * the provided default is returned.
     */
    private static double dbl(Map<String,Object>m,String k,double d){try{return m.get(k)==null?d:Double.parseDouble(String.valueOf(m.get(k)));}catch(Exception e){return d;}}

    /**
     * Reads a boolean property from the map. If the value is missing, the provided default is returned.
     * Parsing uses Boolean.parseBoolean on the stringified value.
     */
    private static boolean bool(Map<String,Object>m,String k,boolean d){Object v=m.get(k);return v==null?d:Boolean.parseBoolean(String.valueOf(v));}
}

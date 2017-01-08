
package chatty.util.api;

import chatty.Chatty;
import chatty.gui.HtmlColors;
import chatty.util.api.CheerEmoticon.CheerEmoticonUrl;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class CheerEmoticonManager extends CachedManager {
    
    private static final Logger LOGGER = Logger.getLogger(CheerEmoticonManager.class.getName());

    public static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60 * 60 * 24;
    private static final String FILE = Chatty.getCacheDirectory() + "cheer_emoticons";
    
    private final TwitchApiResultListener listener;
    
    public CheerEmoticonManager(TwitchApiResultListener listener) {
        super(FILE, CACHED_EMOTICONS_EXPIRE_AFTER, "cheer emoticons");
        this.listener = listener;
    }
    
    @Override
    protected boolean handleData(String data) {
        Set<CheerEmoticon> result = parse(data);
        System.out.println(result);
        if (result == null || result.isEmpty()) {
            return false;
        }
        listener.receivedCheerEmoticons(result);
        return true;
    }
    
    private static Set<CheerEmoticon> parse(String json) {
        Set<CheerEmoticon> result = new HashSet<>();
        JSONParser parser = new JSONParser();
        try {
            JSONObject root = (JSONObject)parser.parse(json);
            JSONArray cheers = (JSONArray)root.get("actions");
            for (Object entry : cheers) {
                if (entry instanceof JSONObject) {
                    Set<CheerEmoticon> parsedEntry = getEntry((JSONObject)entry);
                    if (parsedEntry != null) {
                        result.addAll(parsedEntry);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error parsing Cheers: "+ex);
            return null;
        }
        return result;
    }
    
    private static Set<CheerEmoticon> getEntry(JSONObject entry) {
        try {
            Set<CheerEmoticon> result = new HashSet<>();
            String prefix = (String)entry.get("prefix");
            JSONArray tiers = (JSONArray)entry.get("tiers");
            for (Object o : tiers) {
                CheerEmoticon tierResult = getTier(prefix, (JSONObject)o);
                System.out.println(tierResult);
                if (tierResult != null) {
                    result.add(tierResult);
                }
            }
            return result;
        } catch (Exception ex) {
            LOGGER.warning("Error parsing Cheer: "+ex);
        }
        return null;
    }
    
    private static CheerEmoticon getTier(String prefix, JSONObject tier) {
        int min_bits = ((Number)tier.get("min_bits")).intValue();
        Color color = HtmlColors.decode((String)tier.get("color"));
        Set<CheerEmoticonUrl> urls = new HashSet<>();
        JSONObject images = (JSONObject)tier.get("images");
        addUrls(images, urls, "dark", "animated");
        addUrls(images, urls, "dark", "static");
        addUrls(images, urls, "light", "animated");
        addUrls(images, urls, "light", "static");
        
        return CheerEmoticon.create(prefix, min_bits, color, urls);
    }
    
    private static void addUrls(JSONObject data, Set<CheerEmoticonUrl> urls,
            String background, String state) {
        JSONObject states = (JSONObject)data.get(background);
        JSONObject scales = (JSONObject)states.get(state);
        String[] s = new String[]{"1", "2"};
        for (String scale : s) {
            String url = (String) scales.get(scale);
            urls.add(new CheerEmoticonUrl(url, background, state, scale));
        }
    }
    
}

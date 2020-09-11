package io.disruptedsystems.libdtn.common.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Tag class implements the Taggable interface.
 *
 * @author Lucien Loiseau on 31/10/18.
 */
public abstract class Tag implements Taggable {

    private Map<String, Object> attachement = new HashMap<>();

    @Override
    public void tag(String tag) {
        tag(tag, null);
    }

    @Override
    public Set<String> getAllTags() {
        return attachement.entrySet()
                .stream()
                .filter(e -> e.getValue()==null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public void tag(String key, Object o) {
        attachement.putIfAbsent(key, o);
    }

    @Override
    public void removeTag(String key) {
        attachement.remove(key);
    }

    @Override
    public boolean isTagged(String tag) {
        return attachement.containsKey(tag);
    }

    @Override
    public <T> T getTagAttachment(String key) {
        return (T) attachement.get(key);
    }
}

package io.disruptedsystems.libdtn.core.processor;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.junit.Test;

import java.util.Map;

/**
 * @author Lucien Loiseau on 03/09/20.
 */
public class PatriciaTrieTest {

    @Test
    public void testPrefixMatching() {

        PatriciaTrie<String> table = new PatriciaTrie<>();
        table.put("dtn://aa/1/2/3/4", "dtn://test/1/2/3/4");
        table.put("dtn://aa/1/2/3/", "dtn://test/1/2/3/");
        table.put("dtn://aa/1/2/", "dtn://test/1/2/");
        table.put("dtn://aa/1/", "dtn://test/1/");
        table.put("dtn://ab/1/2/3/4", "dtn://test/1/2/3/4");
        table.put("dtn://ab/1/2/3/", "dtn://test/1/2/3/");
        table.put("dtn://ab/1/2/", "dtn://test/1/2/");
        table.put("dtn://ab/1/", "dtn://test/1/");
        table.put("dtn://bb/1/2/3/4", "dtn://test/1/2/3/4");
        table.put("dtn://bb/1/2/3/", "dtn://test/1/2/3/");
        table.put("dtn://bb/1/2/", "dtn://test/1/2/");
        table.put("dtn://bb/1/", "dtn://test/1/");

        System.err.println("---01");
        for(String key : table.prefixMap("dtn://ab/1/200").keySet()) {
            System.err.println(key);
        }
        System.err.println("---02");
        for(String key : table.headMap("dtn://ab/1/200").keySet()) {
            System.err.println(key);
        }
        System.err.println("---03");
        for(String key : table.tailMap("dtn://ab/1/200").keySet()) {
            System.err.println(key);
        }
    }
}

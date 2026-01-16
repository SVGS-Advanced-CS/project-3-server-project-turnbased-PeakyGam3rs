package com.svgs.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.svgs.dtos.Qu;
import com.svgs.dtos.Show;
import com.svgs.helper.JTimer;
import com.svgs.model.Round;

// this class handles logic for JLoader, it was getting messy (and then got more messy, whatever)
// i tried to maintain db by nuking corrupted data, but something broke and now i just wipe every time \_('')_/
public class JLoader {
    private final static JLoader instance = new JLoader();
    private final JDBWriter jdbw;
    private final JDBReader jdbr;

    private JLoader() {
        jdbw = JDBWriter.getInstance();
        jdbr = new JDBReader(jdbw.getConn()); // prevents db locking confusion
        jdbw.setJDBR(jdbr);
    }

    public JDBReader getJDBR() {
        return jdbr;
    }

    public static JLoader getInstance() {
        return instance;
    }

    // loads a source json to its DTO array
    public void loadDB() {
        Rectified.load200kSet();
        // create arrays or hashmaps for sql heirarchy
        // it feels dumb to unique check a sorted dataset but a good practice ig
        String[] cats = Rectified.getCats();
        Show[] shows = Rectified.getShows();
        Round[] rounds = Rectified.getRounds();
        Qu[] questions = Rectified.getQuestions();

        try {
            jdbw.setAutoCommit(false);

            // gotta wipe the db b4 loading, tons of junk data that wont get auto deleted
            System.out.println("[JLOAD_INFO] Wiping database prior to load...");
            jdbw.wipeDatabase();

            System.out.printf("[JLOAD_INFO] Dataset sizes - cats: %d, shows: %d, rounds: %d, questions: %d%n",
                    cats.length, shows.length, rounds.length, questions.length);

            JTimer jt = new JTimer("[JLOAD_TIMING]");
            try {
                ensureCategories(cats);
                jt.reset("ensureCategories");

                ensureShows(shows);
                jt.reset("ensureShows");

                ensureRounds(rounds);
                jt.reset("ensureRounds");

                ensureQuestions(questions);
                jt.reset("ensureQuestions");

                nukeMissingQuestions(questions);
                jt.reset("nukeMissingQuestions");
                completeCheck();
            } catch (Exception e) {
                System.out.println("[JLOAD_FATAL] Failed to validate database:");
                System.out.println("\tException - " + e.getMessage());
                System.exit(-1);
            }

            jt.reset();
            jdbw.commit();
            jdbw.setAutoCommit(true);
            jt.og("total load time");
            
            jdbr.printQCount();

        } catch (SQLException e) {
            System.out.println("[JLOAD_FATAL] (1) SQL Connection error at JLoader.loadDB.");
            System.out.println("\tException - " + e.getMessage());
            try {
                jdbw.rollback();
            } catch (SQLException sq) {
                System.out.println("[JLOAD_FATAL] (2) Failed to roll back at JLoader.loadDB.");
                System.out.println("\tException - " + sq.getMessage());
            } finally {
                System.exit(-1);
            }
        }

    }

    private void completeCheck() {
        jdbr.markCategories();
    }

    
    // TODO: find and clean redundancies
    private void ensureShows(Show[] sh) {
        HashMap<Integer, Show> showMap = new HashMap<>();
        Stream.of(sh).forEach(n -> showMap.put(n.show_id, n));

        if (showMap.keySet().size() < 100) {
            System.out.printf("[JLOAD_WARN] Abnormally small amount of shows (%d) loaded.%n", showMap.keySet().size());
        }

        try {
            // find shows in DB and in source using ID sets (O(n))
            List<Show> fromdb = jdbw.getShows();
            java.util.Set<Integer> dbIds = new java.util.HashSet<>(fromdb.size());
            for (Show s : fromdb)
                dbIds.add(s.show_id);

            // shows to remove: present in DB but not in source
            List<Show> toNuke = new ArrayList<>();
            for (Show s : fromdb) {
                if (!showMap.containsKey(s.show_id))
                    toNuke.add(s);
            }

            // shows to add: present in source but not in DB
            List<Show> toAdd = new ArrayList<>();
            for (Show s : showMap.values()) {
                if (!dbIds.contains(s.show_id))
                    toAdd.add(s);
            }

            if (!toNuke.isEmpty())
                jdbw.nukeShows(toNuke);
            if (!toAdd.isEmpty())
                jdbw.addShows(toAdd);

            // build the list of shows that are in both for checking
            List<Show> inBoth = new ArrayList<>();
            for (Show s : fromdb) {
                if (showMap.containsKey(s.show_id))
                    inBoth.add(s);
            }
            checkShows(new ArrayList<>(showMap.values()), inBoth);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // round_id is only for mapping questions
    // TODO: add this kinda comment for other load* methods
    // PKEY INTEGER round_id | FKEY INTEGER show_id | TEXT round_name
    private void ensureRounds(Round[] ros) {
        if (ros.length < 100) {
            System.out.println("[JLOAD_WARN] Abnormally small round count " + ros.length);
        }

        // add new rounds
        jdbw.addRounds(ros);
    }

    

    // PKEY INTEGER question_id | FKEY INTEGER round_id | STRING question_text |
    // STRING answer_text | STRING value
    private void ensureQuestions(Qu[] qus) {
        jdbw.addQuestions(qus);
    }

    // Delete questions that exist in DB but are not present in the loaded JSON
    private void nukeMissingQuestions(Qu[] qus) {
        if (qus == null) return;

            // build source signature set: question|answer|value|show_id
            Set<String> src = new HashSet<>(qus.length * 2);
            for (var q : qus) {
                src.add(sigOf(q.question, q.answer, q.value, q.show_id));
            }

            try (java.sql.Statement st = jdbw.getConn().createStatement();
                    java.sql.ResultSet rs = st.executeQuery("SELECT q.question_id, q.question_text, q.answer_text, q.value, r.show_id FROM questions q JOIN rounds r ON q.round_id = r.round_id")) {
                java.util.List<Integer> toDelete = new java.util.ArrayList<>();
                int scanned = 0;
                while (rs.next()) {
                    scanned++;
                    int qid = rs.getInt(1);
                    String qt = rs.getString(2);
                    String at = rs.getString(3);
                    String val = rs.getString(4);
                    int sid = rs.getInt(5);
                    String sig = sigOf(qt, at, val, sid);
                    if (!src.contains(sig)) {
                        toDelete.add(qid);
                        if (toDelete.size() <= 10) {
                            System.out.printf("[JLOAD_DEBUG] queued for deletion question_id=%d sig=%s%n", qid, sig);
                        }
                    }
                }
                if (!toDelete.isEmpty()) {
                    System.out.printf("[JLOAD_INFO] Deleting %d questions not in json (scanned=%d)%n", toDelete.size(), scanned);
                    int[] arr = toDelete.stream().mapToInt(Integer::intValue).toArray();
                    jdbw.deleteQuestions(arr);
                } else {
                    System.out.printf("[JLOAD_INFO] No wack questions found (scanned=%d)%n", scanned);
                }
            } catch (Exception e) {
                System.out.println("[JLOAD_WARN] Failed to nuke missing questions: " + e.getMessage());
            }
    }

    private static String sigOf(String q, String a, String v, int sid) {
        if (q == null) q = "";
        if (a == null) a = "";
        if (v == null) v = "";
        // normalize whitespace and trim
        q = q.trim().replaceAll("\\s+", " ");
        a = a.trim().replaceAll("\\s+", " ");
        v = v.trim().replaceAll("\\s+", " ");
        // found this dope unicode thiny online, lowk clean ngl
        return q + "\u0001" + a + "\u0001" + v + "\u0001" + sid;
    }

    private void ensureCategories(String[] cats) {
        List<String> sourceCats = new ArrayList<>(Arrays.asList(cats)); // known cats from source

        // matches need to be exact, normalizing DOES NOT WORK!
        java.util.Set<String> sourceSet = new java.util.HashSet<>(sourceCats.size());
        for (String s : sourceCats) sourceSet.add(s);

        // get DB categories and build set for exact matching
        java.util.Collection<String> dbCats = jdbr.getCats().values();
        java.util.Set<String> dbSet = new java.util.HashSet<>(dbCats);

        // nuke wack cats
        List<String> toNuke = new ArrayList<>();
        for (String dbc : dbCats) {
            if (!sourceSet.contains(dbc)) toNuke.add(dbc);
        }

        List<String> toAdd = new ArrayList<>();
        for (String sc : sourceCats) {
            if (!dbSet.contains(sc)) toAdd.add(sc);
        }

        System.out.printf("[JLOAD_INFO] categories: toAdd=%d toNuke=%d%n", toAdd.size(), toNuke.size());
        if (!toAdd.isEmpty()) {
            int show = Math.min(10, toAdd.size());
            System.out.print("[JLOAD_INFO] sample categories to add: ");
            for (int i = 0; i < show; i++) {
                System.out.print(toAdd.get(i));
                if (i < show - 1) System.out.print(", ");
            }
            System.out.println();
        }
        if (!toNuke.isEmpty()) {
            if (toNuke.size() > 2000) {
                // if a ton must be nuked, likely wack code and needs debugging
                System.out.printf("[JLOAD_WARN] Skipping deletion of %d categories (too many).%n", toNuke.size());
                toNuke.clear();
            } else {
                jdbw.nukeCats(toNuke);
            }
        }
        if (!toAdd.isEmpty()) jdbw.addCats(toAdd);
        
        // debugging for categories, i had some tragic bugs happen and im leaving it in.
        try {
            java.util.HashMap<String, Integer> catMap = jdbr.getCNTIDMap();
            System.out.printf("[JLOAD_DEBUG] post-addCats catMap.size=%d\n", catMap.size());
            int show = Math.min(10, catMap.size());
            System.out.print("[JLOAD_DEBUG] sample catMap keys: ");
            int i = 0;
            for (String k : catMap.keySet()) {
                System.out.print(k);
                if (++i >= show) break;
                System.out.print(", ");
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("[JLOAD_WARN] Failed to fetch catMap after addCats: " + e.getMessage());
        }
    
    }

    private void checkShows(List<Show> correctShows, List<Show> fromdb) {
        HashMap<Integer, Show> correct = new HashMap<>();
        correctShows.stream().forEach(s -> correct.put((Integer) s.show_id, s));

        List<Show> corrections = new ArrayList<>();
        fromdb.stream()
                .filter(s -> !s.equals(correct.get(s.show_id)))
                .forEach(s -> corrections.add(correct.get(s.show_id)));

    }
}

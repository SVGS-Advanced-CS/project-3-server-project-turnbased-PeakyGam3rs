package com.svgs.db;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.svgs.dtos.Jep;
import com.svgs.dtos.Qu;
import com.svgs.dtos.Show;
import com.svgs.model.Round;
// this class rectifies the DTOS loaded in 
public class Rectified {
    private static final Gson gson = new Gson();
    private static final List<Honcho> honchos = new ArrayList<>();
    private static String[] cachedCats = null;
    private static Show[] cachedShows = null;
    private static Round[] cachedRounds = null;
    private static Qu[] cachedQuestions = null;

    static boolean containsNull(Jep j) {
        if (j.category == null)
            return true;
        if (j.value == null)
            return true;
        if (j.air_date == null)
            return true;
        if (j.round == null)
            return true;
        if (j.show_number == 0)
            return true;
        if (j.question == null)
            return true;
        return j.answer == null;
    }

    static Honcho toHoncho(Jep j) {
        return new Honcho(j.category.trim(),
                j.value.trim().replaceAll("[,$]",""),
                j.air_date.trim(),
                j.round.trim(),
                j.show_number,
                j.question.trim(),
                j.answer.trim());
    }

    private record Honcho( // internal class for combining data sets
            String category,
            String value,
            String air_date,
            String round_name,
            int show_id,
            String question,
            String answer) {
    }

    public static void load200kSet() {
        // reset previous state to avoid appending on repeated runs
        honchos.clear();

        List<Jep> jeps = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("./src/main/resources/200k_questions.json"))) {
            jeps.addAll(Arrays.asList(gson.fromJson(br, Jep[].class)));
        } catch (Exception e) {
            System.out.println("[JCTRL_ERROR] " + e);
        }
        for (Jep j : jeps) {
            if (!containsNull(j)) {
                honchos.add(toHoncho(j));
            }
        }

        // compute and cache derived arrays in a single pass to avoid multiple scans
        java.util.Set<String> catSet = new java.util.LinkedHashSet<>();
        java.util.Map<Integer, String> showMap = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, java.util.Set<String>> roundsMap = new java.util.LinkedHashMap<>();
        java.util.List<Qu> qlist = new java.util.ArrayList<>();

        for (Honcho h : honchos) {
            // preserve original category string (trim only) — do not normalize or lowercase
            String cat = h.category == null ? "" : h.category.trim();
            if (cat.length() > 2 && cat.chars().anyMatch(Character::isLetter))
                catSet.add(cat);
            showMap.putIfAbsent(h.show_id, h.air_date);
            roundsMap.computeIfAbsent(h.show_id, k -> new java.util.LinkedHashSet<>()).add(h.round_name);
            Qu q = new Qu(h.question, h.answer, h.value, h.show_id);
            q.round_name = h.round_name;
            q.category = cat;
            qlist.add(q);
        }

        cachedCats = catSet.toArray(new String[0]);
        cachedShows = showMap.entrySet().stream().map(e -> new Show(e.getKey(), e.getValue())).toArray(Show[]::new);
        java.util.List<Round> rlist = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, java.util.Set<String>> e : roundsMap.entrySet()) {
            int sid = e.getKey();
            for (String rn : e.getValue()) {
                rlist.add(new Round(-1, sid, rn));
            }
        }
        cachedRounds = rlist.toArray(new Round[0]);
        cachedQuestions = qlist.toArray(new Qu[0]);
        

    }

    // normalize category strings to reduce unique variants (trim, collapse whitespace,
    // remove punctuation, lowercase) TODO: implement
    private static String normalizeCategory(String s) {
        if (s == null) return "";
        return s.trim();
    }

    public static String[] getCats() {
        if (cachedCats != null)
            return cachedCats;
        return new String[0];
    }

    public static Show[] getShows() {
        if (cachedShows != null)
            return cachedShows;
        return new Show[0];
    }

    public static Round[] getRounds() {
        if (cachedRounds != null)
            return cachedRounds;
        return new Round[0];
    }

    static Qu[] getQuestions() {
        if (cachedQuestions != null)
            return cachedQuestions;
        return new Qu[0];

    }

}

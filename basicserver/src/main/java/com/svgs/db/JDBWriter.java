package com.svgs.db;
// is THE ONLY class that writes ANYTHING to jprdy.db
// some methods may not write, but are only relevant to writing

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.svgs.dtos.Qu;
import com.svgs.dtos.Show;
import com.svgs.model.Round;

public class JDBWriter {
    static JDBWriter instance = new JDBWriter();
    private  static Connection conn;
    private JDBReader jdbr;

    private JDBWriter() {
        String url = "jdbc:sqlite:./src/main/resources/JPRDY.db";
        try {
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public static JDBWriter getInstance() {
        return instance;
    }

    public void setJDBR(JDBReader jdbr) {
        this.jdbr = jdbr;
    }

    Connection getConn() {
        return conn;
    }

    void setAutoCommit(boolean ac) throws SQLException {
        conn.setAutoCommit(ac);
    }

    void commit() throws SQLException {
        conn.commit();
    }

    void rollback() throws SQLException {
        conn.rollback();
    }

    void deleteRounds(List<Round> toDelete) {
        String update = "DELETE FROM rounds WHERE show_id = ? AND round_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            for (Round wacko : toDelete) {
                System.out.printf("[JDBW_DEBUG] Deleting round with id %d and name \"%s\":%n", wacko.show_id,
                        wacko.round_name);
                ps.setInt(1, wacko.show_id);
                ps.setString(2, wacko.round_name);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("[JDBW_ERROR] SQLException while deleting wack rounds in JDBWriter.ensureRounds:");
            System.out.println("\tException - " + e.getMessage());
        }
    }

    void addRounds(Round[] rounds) {
        String update = "INSERT OR IGNORE INTO rounds (show_id, round_name) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            int count = 0;
            for (Round r : rounds) {
                System.out.printf("[JDBW_DEBUG] Adding round with id %d and name \"%s\":%n", r.show_id,
                        r.round_name);
                ps.setInt(1, r.show_id);
                ps.setString(2, r.round_name);
                ps.addBatch();
                count++;
                if (count > 500) {
                    ps.executeBatch();
                    count = 0;
                }
            }
            // execute any remaining batched statements
            if (count > 0) ps.executeBatch();
        } catch (SQLException e) {
            System.out.println("[JDBW_ERROR] SQLException while adding new rounds in JrpdyCTRL.ensureRounds:");
            System.out.println("\tException - " + e.getMessage());
        }
    }

    // for simplicity, the map is just the question PK to delete.
    void deleteQuestions(int qids[]) {
        String update = "DELETE FROM questions WHERE question_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            for (int qid : qids) {
                ps.setInt(1, qid);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("[JDBW_ERROR] Failed to delete question(s) at JDBWriter.deleteQuestions.");
            System.out.println("\tException - " + e.getMessage());
        }
    }

    // correctShows are shows being loaded from json, and correcting shows w/ same
    // id
    void overwriteShows(List<Show> rights, List<Show> wrongs) {
        String update = "UPDATE shows SET air_date = ? WHERE show_id = ?";
        try (PreparedStatement state = conn.prepareStatement(update)) {
            for (Show w : wrongs) {
                Show right = w;
                for (Show r : rights) {
                    if (r.show_id == w.show_id) {
                        right = r;
                        break;
                    }
                }
                if (right.equals(w)) {
                    continue;
                }
                try {
                    state.setString(1, right.air_date);
                    state.setInt(2, w.show_id);
                    state.executeUpdate();
                } catch (SQLException e) {
                    System.out.printf("[JDBW_ERROR] Failed to write corrections (UPDATE) for show id %d to db:%n",
                            right.show_id);
                    System.out.printf("\tCaught SQLException - %s%n" + e.getMessage());
                    System.out.printf("\tStatement - \"UPDATE SHOW SET air_date = %s WHERE show_id = %d%n",
                            right.air_date, right.show_id);
                    System.out.println("\tCorrect value (air_date) - " + right.air_date);
                    System.out.println("\tIncorrect value (air_date) - " + w.air_date);
                }
            }
        } catch (Exception e) {
            System.out.println(
                    "[JDBW_ERROR] Failed to make corrections at JDBWriter.checkShows:\n\tCaught Exception - " + e);
        }
    }

    void addShows(List<Show> shows) {
        if (shows == null || shows.isEmpty()) return;
        String update = "INSERT OR IGNORE INTO shows (show_id, air_date) VALUES (?,?)";
        try (PreparedStatement state = conn.prepareStatement(update)) {
            int n = 0;
            long t0 = System.nanoTime();
            for (Show s : shows) {
                state.setInt(1, s.show_id);
                state.setString(2, s.air_date);
                state.addBatch();
                if (++n % 500 == 0) {
                    state.executeBatch();
                    System.out.printf("[JDBW_DEBUG] addShows: inserted %d so far%n", n);
                }
            }
            if (n % 500 != 0) state.executeBatch();
            long t1 = System.nanoTime();
            System.out.printf("[JDBW_INFO] addShows: added %d shows in %.3fs%n", n, (t1 - t0) / 1e9);

        } catch (Exception e) {
            System.out.println("[JDBW_ERROR] Exception in JrpdyCTRL.ensureShows: " + e);
        }
    }

    
    void nukeCats(List<String> targets) {
        if (targets == null || targets.isEmpty()) return;
        String update = "DELETE FROM categories WHERE category_name = ?";
        try (PreparedStatement state = conn.prepareStatement(update)) {
            int n = 0;
            long t0 = System.nanoTime();
            for (String target : targets) {
                state.setString(1, target);
                state.addBatch();
                if (++n % 500 == 0) {
                    state.executeBatch();
                    System.out.printf("[JDBW_DEBUG] nukeCats: deleted %d so far%n", n);
                }
            }
            if (n % 500 != 0) state.executeBatch();
            long t1 = System.nanoTime();
            System.out.printf("[JDBW_INFO] nukeCats: deleted %d categories in %.3fs%n", n, (t1 - t0) / 1e9);
        } catch (Exception e) {
            System.out.println("[JDBW_ERROR] Failed to delete unknown category at JDBWriter.nukeCats:");
            System.out.println("\tException - " + e.getMessage());
        }
    }

    void addCats(List<String> cats) {
        if (cats == null || cats.isEmpty()) return;
        String update = "INSERT OR IGNORE INTO categories (category_name) VALUES(?)";
        try (PreparedStatement state = conn.prepareStatement(update)) {
            int n = 0;
            long t0 = System.nanoTime();
            for (String cat : cats) {
                state.setString(1, cat);
                state.addBatch();
                if (++n % 500 == 0) {
                    state.executeBatch();
                    System.out.printf("[JDBW_DEBUG] addCats: inserted %d so far%n", n);
                }
            }
            if (n % 500 != 0) state.executeBatch();
            long t1 = System.nanoTime();
            System.out.printf("[JDBW_INFO] addCats: added %d categories in %.3fs%n", n, (t1 - t0) / 1e9);
        } catch (Exception e) {
            System.out.println("[JDBW_ERROR] SQL error in JrpdyCTRL.ensureCategories:");
            System.out.println("\tException - " + e);
        }
    }

    void nukeShows(List<Show> targets) {
        String update = "DELETE FROM shows WHERE show_id = ?";
        try (PreparedStatement state = conn.prepareStatement(update)) {
            for (Show target : targets) {
                try {
                    state.setInt(1, target.show_id);
                    int resp = state.executeUpdate();
                    if (resp == 0) {
                        System.out.printf(
                                "[JCTRL_WARN] No changes made with command (DELETE) %d in JDBWriter.nukeShow:%n",
                                target.show_id);
                        System.out.printf("\tSQL command - \"%s\"%n", update.substring(0, update.length() - 2),
                                target.show_id);
                        System.out.println("\tJDBC Response Code - 0\n");
                    }
                } catch (SQLException e) {
                    System.out.printf(
                            "[JDBW_ERROR] Failed to nuke (DELETE) show with id %d from db at JrpdyCTRL.nukeShow:%n",
                            target.show_id);
                    System.out.printf("\tFailed SQL command - \"%s%s\"%n", update.substring(0, update.length() - 2),
                            target.show_id);
                    System.out.println("\tSQLException - %n" + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("[JDBW_ERROR] Failed to create prepared statement in JDBWriter.nukeShow:");
            System.out.println("\tException - " + e);
        }
    }

    // Wipe all main data tables. Deletes are performed child->parent to satisfy
    // foreign key constraints (questions -> rounds -> shows -> categories).
    // This is a destructive operation and is intended to be invoked explicitly
    // by the loader when the user requests a clean load.
    void wipeDatabase() {
        try (Statement st = conn.createStatement()) {
            long t0 = System.nanoTime();

            int delQ = st.executeUpdate("DELETE FROM questions");
            System.out.printf("[JDBW_INFO] wipeDatabase: deleted %d questions%n", delQ);

            int delR = st.executeUpdate("DELETE FROM rounds");
            System.out.printf("[JDBW_INFO] wipeDatabase: deleted %d rounds%n", delR);

            int delS = st.executeUpdate("DELETE FROM shows");
            System.out.printf("[JDBW_INFO] wipeDatabase: deleted %d shows%n", delS);

            int delC = st.executeUpdate("DELETE FROM categories");
            System.out.printf("[JDBW_INFO] wipeDatabase: deleted %d categories%n", delC);

            // Reset sqlite autoincrement sequences so IDs start fresh
            st.executeUpdate("DELETE FROM sqlite_sequence WHERE name IN ('questions','rounds','shows','categories')");

            long t1 = System.nanoTime();
            System.out.printf("[JDBW_INFO] wipeDatabase: completed in %.3fs%n", (t1 - t0) / 1e9);
        } catch (SQLException e) {
            System.out.println("[JDBW_ERROR] wipeDatabase failed: " + e.getMessage());
        }
    }

    List<Show> getShows() throws SQLException {
        HashMap<Integer, Show> result = new HashMap<>();
        String query = "SELECT * FROM shows";
        Statement state = conn.createStatement();
        ResultSet rs = state.executeQuery(query);
        while (rs.next()) {
            int id = rs.getInt("show_id");
            String ad = rs.getString("air_date");
            Show tmp = new Show(id, ad);
            result.put(tmp.show_id, tmp);
        }
        return new ArrayList<>(result.values());
    }

    
    void addQuestions(Qu[] toAdd) {
        String sql = "INSERT OR IGNORE INTO questions (question_text, answer_text, value, round_id, category_id) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            HashMap<Integer, HashMap<String, Integer>> roundMap = jdbr.getRoundMap(); // show_id -> (round_name -> round_id)
            HashMap<String, Integer> catMap = jdbr.getCNTIDMap(); // category_name -> category_id

            System.out.printf("[JDBW_DEBUG] catMap size: %d\n", catMap.size());
            int showSample = Math.min(10, catMap.size());
            System.out.print("[JDBW_DEBUG] sample catMap keys: ");
            int si = 0;
            for (String k : catMap.keySet()) {
                System.out.print(k);
                if (++si >= showSample) break;
                System.out.print(", ");
            }
            System.out.println();

            java.util.Set<String> missingCatSamples = new java.util.LinkedHashSet<>();

            int total = toAdd == null ? 0 : toAdd.length;
            System.out.printf("[JDBW_INFO] addQuestions: processing %d entries%n", total);
            int n = 0, inserted = 0, skippedNoRound = 0, skippedNoCat = 0, batchRuns = 0;
            long t0 = System.nanoTime();
            for (Qu q : toAdd) {
                HashMap<String, Integer> roundsForShow = roundMap.get(q.show_id);
                if (roundsForShow == null) {
                    skippedNoRound++;
                    continue;
                }
                Integer rid = roundsForShow.get(q.round_name);
                if (rid == null) {
                    skippedNoRound++;
                    continue; // missing round mapping
                }

                Integer cid = catMap.get(q.category);
                if (cid == null) {
                    skippedNoCat++;
                    if (missingCatSamples.size() < 20) missingCatSamples.add(q.category);
                    continue; // missing category mapping
                }

                ps.setString(1, q.question);
                ps.setString(2, q.answer);
                ps.setString(3, q.value);
                ps.setInt(4, rid);
                ps.setInt(5, cid);

                ps.addBatch();
                inserted++;
                if (++n % 1000 == 0) {
                    ps.executeBatch();
                    batchRuns++;
                    if (batchRuns % 10 == 0)
                        System.out.printf("[JDBW_DEBUG] addQuestions: processed %d/%d (inserted %d, skippedRound %d, skippedCat %d)%n", n, total, inserted, skippedNoRound, skippedNoCat);
                }
            }
            if (n % 1000 != 0) {
                ps.executeBatch();
                batchRuns++;
            }
            long t1 = System.nanoTime();
            System.out.printf("[JDBW_INFO] addQuestions: inserted=%d skippedNoRound=%d skippedNoCat=%d batches=%d time=%.3fs%n", inserted, skippedNoRound, skippedNoCat, batchRuns, (t1 - t0) / 1e9);
            if (!missingCatSamples.isEmpty()) {
                System.out.print("[JDBW_DEBUG] sample missing categories: ");
                int m = 0;
                for (String s : missingCatSamples) {
                    System.out.print(s);
                    if (++m >= missingCatSamples.size()) break;
                    System.out.print(", ");
                }
                System.out.println();
            }

        } catch (SQLException e) {
            System.out.println("[JDBW_FATAL] " + e.getMessage());
            System.exit(-1);
        }
    }
    
}
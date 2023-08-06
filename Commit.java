package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Commit class for Gitlet, the tiny stupid ass version-control system.
 *  @author Song Bai
 *
 *  credits:
 *  https://docs.oracle.com/javase/8/docs/api/java/util/HashMap.html#put-K-V-
 *  https://docs.oracle.com/javase/8/docs/api/java/util/
 *  LinkedHashMap.html#LinkedHashMap-java.util.Map-
 *  https://docs.oracle.com/javase/7/docs/api/java/io/File.html
 *  https://www.w3schools.com/java/java_date.asp
 *  https://stackoverflow.com/questions/30710829/
 *  java-time-datetimeformatter-pattern-for-timezone-offset
 */

public class Commit implements Serializable {

    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** .gitlet folder. */
    static final File GITLET = new File(".gitlet");

    /** Staging for addition folder. */
    static final File STAGINGADDITION = Utils.join(GITLET, "stagingaddition");

    /** Staging for removal folder. */
    static final File STAGINGREMOVAL = Utils.join(GITLET, "stagingremoval");

    /** Commits folder. */
    static final File COMMITS = Utils.join(GITLET, "commits");

    /** Blobs folder. */
    static final File BLOBS = Utils.join(GITLET, "blobs");

    /** Branches folder. */
    static final File BRANCHES = Utils.join(GITLET, "branches");

    /** Current branch file. */
    static final File CURRENTBRANCH = Utils.join(GITLET, "currentbranch");

    /** Head commit file. */
    static final File HEAD = Utils.join(GITLET, "head");

    /** My message. */
    private String _message;

    /** My parent. */
    private String _parent;

    /** My 2nd parent, for merges. */
    private String _parent2;

    /** My timestamp. */
    private String _timestamp;

    /** My tracked files and their blobs. */
    private LinkedHashMap<String, String> _blobs;

    /** Creates a commit with MESSAGE. */
    public Commit(String message) {
        _message = message;
        _parent = Utils.readContentsAsString(HEAD);

        File parentCommitFile = Utils.join(COMMITS, _parent);
        Commit parentCommit = Utils.readObject(parentCommitFile, Commit.class);
        _blobs = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry
                : parentCommit.blobs().entrySet()) {
            String fileName = entry.getKey();
            String blobSHA = entry.getValue();
            _blobs.put(fileName, blobSHA);
        }

        for (File file : STAGINGADDITION.listFiles()) {
            String fileName = file.getName();
            String blobSHA = Utils.readContentsAsString(file);
            _blobs.put(fileName, blobSHA);
        }

        for (File file : STAGINGREMOVAL.listFiles()) {
            String fileName = file.getName();
            _blobs.remove(fileName);
        }

        ZonedDateTime dateTime = ZonedDateTime.now();
        DateTimeFormatter format = DateTimeFormatter.
                ofPattern("EEE MMM d HH:mm:ss yyyy Z");
        _timestamp = dateTime.format(format);
    }

    /** Creates the initial commit. */
    public Commit() {
        _message = "initial commit";
        _parent = null;
        _timestamp = "Wed Dec 31 16:00:00 1969 -0800";
        _blobs = new LinkedHashMap<>();
    }

    /** Creates the commit for merge with 2nd parent PARENT2
     * and message MESSAGE. */
    public Commit(String parent2, String message) {
        this(message);
        _parent2 = parent2;
    }

    /** Returns my tracked files and their blobs. */
    public LinkedHashMap<String, String> blobs() {
        return _blobs;
    }

    /** Returns my message. */
    public String message() {
        return _message;
    }

    /** Returns my parent. */
    public String parent() {
        return _parent;
    }

    /** Returns my 2nd parent, for merges. */
    public String parent2() {
        return _parent2;
    }

    /** Returns my timestamp. */
    public String timestamp() {
        return _timestamp;
    }
}
